"""
programa_nc_para_supabase.py - Leva o programa do Método Natação Criativa (o
carrossel "Cada Dia 1 Treino" a partir de 28/09/2026) aos treinos sugeridos do
Aquagenda. Vale desde 15/09/2026 (âncora do programa).

A fonte é o programa_nc.json do repositório natacao-treinos, o MESMO arquivo
que gera o carrossel. Daqui saem, da mesma conversão:

    supabase/seed/programa_nc.sql          -> rodar no SQL Editor, DEPOIS da
                                              migração 20260914000001_metodo_nc.sql
    app/src/main/assets/programa_nc.json   -> cópia embarcada, usada offline

O ciclo antigo (cada-dia-1-treino) continua no banco e no app. A função
public.treinos_sugeridos escolhe o ciclo pela data: vale o de âncora mais
recente que já começou. Até 14/09 saiu o antigo; de 15/09 em diante, este.

A auditoria do método (zonas, PSE, pausas, proporções) mora em
natacao-treinos/scripts/programa_nc.py e roda antes de todo post; aqui só se
confere o que a conversão precisa para não gravar lixo.

Uso:
    python scripts/programa_nc_para_supabase.py
    python scripts/programa_nc_para_supabase.py --fonte ../natacao-treinos/programa_nc.json
"""
import argparse
import json
import re
import sys
import urllib.request
from datetime import date
from pathlib import Path

for _s in (sys.stdout, sys.stderr):
    try:
        _s.reconfigure(encoding="utf-8", errors="replace")
    except AttributeError:
        pass

RAIZ = Path(__file__).resolve().parent.parent
FONTE_LOCAL = RAIZ.parent / "natacao-treinos" / "programa_nc.json"
FONTE_REMOTA = "https://raw.githubusercontent.com/Nicevargas/natacao-treinos/main/programa_nc.json"
SAIDA_SQL = RAIZ / "supabase" / "seed" / "programa_nc.sql"
SAIDA_JSON = RAIZ / "app" / "src" / "main" / "assets" / "programa_nc.json"

CICLO_ID = "metodo-nc"
CICLO_NOME = "Cada Dia 1 Treino · Método NC"

NIVEIS = {"verde": "INICIANTE", "amarelo": "INTERMEDIARIO", "vermelho": "AVANCADO"}
BLOCOS = (("ativacao", "Ativação"), ("preparacao", "Preparação"),
          ("desenvolvimento", "Desenvolvimento"), ("consolidacao", "Consolidação"),
          ("recuperacao", "Recuperação"))
ZONAS = ("A0", "A1", "A2", "A3", "AN", "AA")

_REPS = re.compile(r"^(\d+)\s*x\s*(\d+)\s*m\b\s*", re.I)
_SIMPLES = re.compile(r"^(\d+)\s*m\b\s*", re.I)
_INTERVALO = re.compile(r"""^([#@])\s*(?:(\d+)')?\s*(?:(\d+)")?$""")

MATERIAIS = (
    ("Palmar", re.compile(r"palmar", re.I)),
    ("Pull buoy", re.compile(r"pull\s*buoy", re.I)),
    ("Nadadeira", re.compile(r"nadadeira", re.I)),
    ("Prancha", re.compile(r"prancha", re.I)),
)

# Mesma estimativa de duração de natacao-treinos/scripts/programa_nc.py, para o
# "~53 min" do app bater com o do slide.
RITMO_A1 = {"verde": 180, "amarelo": 140, "vermelho": 110}
FATOR_DA_ZONA = {"A0": 1.12, "A1": 1.0, "A2": 0.92, "A3": 0.85, "AN": 0.80, "AA": 0.70}
FATOR_CORRETIVO = 1.25
TRANSICAO_S = 30
KCAL_POR_MINUTO = 8

# Mesmas frases da legenda do carrossel (natacao-treinos/scripts/postar_treino.py).
SOBRE_O_FOCO = {
    "Técnica": "Hoje o ganho é de percepção: os corretivos da Preparação voltam ao nado completo no Desenvolvimento.",
    "Resistência": "Ritmo moderado que daria para sustentar por 20 a 30 minutos. Constância vale mais que velocidade.",
    "Velocidade": "Acelerações curtas com o corpo descansado, antes da série principal. Qualidade máxima e pausa generosa.",
    "Estilos": "Costas, peito e borboleta com corretivos. Nadar o que você não gosta costuma destravar o que você gosta.",
    "Ritmo": "Passagem negativa: começar controlado e terminar mais rápido. É assim que se aprende a dosar o esforço.",
    "Força específica": "Palmar, pull buoy e nadadeira ensinam sensação. O que importa é o que muda quando você tira o material.",
    "Recuperação": "Dia leve de propósito. Recuperação faz parte do treino: pular este dia atrapalha a semana seguinte.",
}


def ler_fonte(fonte: str | None) -> tuple[dict, str]:
    if fonte is None:
        fonte = str(FONTE_LOCAL) if FONTE_LOCAL.exists() else FONTE_REMOTA
    if fonte.startswith(("http://", "https://")):
        with urllib.request.urlopen(fonte, timeout=30) as r:
            return json.loads(r.read().decode("utf-8")), fonte
    return json.loads(Path(fonte).read_text(encoding="utf-8")), FONTE_REMOTA


def repeticoes(cab: str) -> tuple[int, int, str]:
    if m := _REPS.match(cab):
        return int(m.group(1)), int(m.group(2)), cab[m.end():].strip()
    if m := _SIMPLES.match(cab):
        return 1, int(m.group(1)), cab[m.end():].strip()
    raise ValueError(f"série sem distância legível no cabeçalho: {cab!r}")


def ler_serie(serie: dict, cor: str, fase_id: str, i: int) -> tuple[dict, float]:
    cab = serie["serie"].strip()
    reps, dist, nado = repeticoes(cab)
    zona = serie.get("zona")
    if zona not in ZONAS:
        raise ValueError(f"{cab!r}: zona {zona!r} inválida")
    pse = str(serie.get("pse", "")).strip()
    detalhes = serie.get("detalhes", [])

    intervalo = serie.get("intervalo")
    pausa = 0
    envio = 0
    if intervalo:
        m = _INTERVALO.match(intervalo.strip())
        if not m or not (m.group(2) or m.group(3)):
            raise ValueError(f"{cab!r}: intervalo ilegível {intervalo!r}")
        seg = int(m.group(2) or 0) * 60 + int(m.group(3) or 0)
        # Intervalo aberto (#) é a pausa. No fechado (@) a pausa depende de quem
        # nada; o app não inventa um número para ela.
        if m.group(1) == "#":
            pausa = seg
        else:
            envio = seg

    ritmo = RITMO_A1[cor] * FATOR_DA_ZONA[zona] * (FATOR_CORRETIVO if serie.get("corretivo") else 1)
    esforco = dist / 100 * ritmo
    segundos = (reps * max(envio, esforco) if envio else reps * esforco + (reps - 1) * pausa) + TRANSICAO_S

    texto = " ".join([cab, *detalhes])
    cor_obj = serie.get("corretivo")
    return {
        "id": f"{fase_id}_s{i + 1}",
        "serie": cab,
        "repsDescription": f"{reps}x{dist}",
        "stroke": nado or " · ".join(detalhes) or "Nado livre",
        "details": detalhes,
        "distanceMeters": reps * dist,
        "interval": intervalo,
        "intensity": f"{zona} · PSE {pse}" if zona != "AA" else "AA · esforço máximo curto",
        "restSeconds": pausa,
        "equipment": " + ".join(n for n, rx in MATERIAIS if rx.search(texto)) or None,
        "isDone": False,
        "zona": zona,
        "pse": pse,
        "corretivo": ({"nome": cor_obj["nome"], "objetivo": cor_obj["objetivo"], "dica": cor_obj["dica"]}
                      if cor_obj else None),
    }, segundos


def porcentagens(metros: list) -> list:
    """Arredonda preservando a soma em 100 (maior resto)."""
    total = sum(metros)
    brutos = [m * 100 / total for m in metros]
    base = [int(b) for b in brutos]
    falta = 100 - sum(base)
    for i in sorted(range(len(brutos)), key=lambda i: brutos[i] - base[i], reverse=True)[:falta]:
        base[i] += 1
    return base


def converter_nivel(t: dict, dia: int, dias: int, cor: str, rotulo: dict) -> dict:
    nivel = t["niveis"][cor]
    level = NIVEIS[cor]
    desconhecidos = set(nivel["blocos"]) - {b for b, _ in BLOCOS}
    if desconhecidos:
        raise ValueError(f"bloco(s) desconhecido(s): {sorted(desconhecidos)}")

    fases, segundos = [], 0.0
    for chave, titulo in BLOCOS:
        series = nivel["blocos"].get(chave)
        if not series:
            continue
        fase_id = f"nc_d{dia:02d}_{cor}_{chave}"
        sets = []
        for i, s in enumerate(series):
            conv, seg = ler_serie(s, cor, fase_id, i)
            sets.append(conv)
            segundos += seg
        fases.append({
            "id": fase_id,
            "title": titulo,
            "summary": " + ".join(s["serie"] for s in sets),
            "distanceMeters": sum(s["distanceMeters"] for s in sets),
            "status": "PENDING",
            "sets": sets,
        })

    for fase, pct in zip(fases, porcentagens([f["distanceMeters"] for f in fases])):
        fase["percentage"] = pct

    minutos = round(segundos / 60)
    return {
        "id": f"nc_d{dia:02d}_{level.lower()}",
        "ciclo_id": CICLO_ID,
        "ciclo_dia": dia,
        "semana": t.get("semana"),
        "bloco": t.get("mesociclo"),
        "foco": t["foco"],
        "level": level,
        "nivel_carrossel": cor,
        "title": t["foco"],
        "subtitle": f"{rotulo['nome']} · {t.get('mesociclo')}",
        "tag": f"Método NC · Dia {dia}/{dias}",
        "total_distance_meters": sum(f["distanceMeters"] for f in fases),
        "estimated_minutes": minutos,
        "calories": round(minutos * KCAL_POR_MINUTO / 10) * 10,
        "motivational_tip": SOBRE_O_FOCO.get(t["foco"]),
        "objetivo": nivel["objetivo"],
        "zona": nivel["zona"],
        "ajuste": nivel["ajuste"],
        "phases": fases,
    }


def converter(dados: dict, fonte: str) -> tuple:
    ancora = date.fromisoformat(dados["ancora"])
    lista = dados["treinos"]
    problemas = []
    # A âncora pode ser qualquer dia (o método começa numa terça); o que prende
    # cada foco a um dia da semana é o ciclo ser múltiplo de 7.
    if len(lista) % 7:
        problemas.append(f"O ciclo tem {len(lista)} dias; precisa ser múltiplo de 7.")

    linhas = []
    for idx, t in enumerate(lista):
        dia = idx + 1  # o carrossel escolhe pela POSIÇÃO na lista
        if t.get("dia") != dia:
            problemas.append(f"Posição {dia} traz 'dia': {t.get('dia')}.")
        for cor in NIVEIS:
            if cor not in t["niveis"]:
                problemas.append(f"dia {dia}: falta o nível {cor}.")
                continue
            try:
                linhas.append(converter_nivel(t, dia, len(lista), cor, dados["rotulos"][cor]))
            except (ValueError, KeyError) as e:
                problemas.append(f"dia {dia}/{cor}: {e}")

    ciclo = {"id": CICLO_ID, "nome": CICLO_NOME, "ancora": dados["ancora"], "dias": len(lista),
             "handle": dados.get("handle"), "fonte": fonte}
    return ciclo, linhas, problemas


# -------------------------------------------------------------------- saídas

def sql_texto(v) -> str:
    if v is None:
        return "NULL"
    if isinstance(v, bool):
        return "TRUE" if v else "FALSE"
    if isinstance(v, int):
        return str(v)
    return "'" + str(v).replace("'", "''") + "'"


def sql_json(v) -> str:
    txt = json.dumps(v, ensure_ascii=False)
    if "$fases$" in txt:
        raise ValueError("O conteúdo contém o delimitador $fases$.")
    return f"$fases${txt}$fases$::jsonb"


COLUNAS = ("id", "ciclo_id", "ciclo_dia", "semana", "bloco", "foco", "level",
           "nivel_carrossel", "title", "subtitle", "tag", "total_distance_meters",
           "estimated_minutes", "calories", "motivational_tip", "objetivo", "zona", "ajuste")


def montar_sql(ciclo: dict, linhas: list) -> str:
    valores = ",\n".join(
        "  (" + ", ".join(sql_texto(l[c]) for c in COLUNAS) + ",\n   " + sql_json(l["phases"]) + ")"
        for l in linhas)
    atualizar = ",\n  ".join(f"{c} = EXCLUDED.{c}" for c in (*COLUNAS[1:], "phases"))
    ids = ", ".join(sql_texto(l["id"]) for l in linhas)
    cid = sql_texto(ciclo["id"])

    return f"""-- GERADO por scripts/programa_nc_para_supabase.py a partir de
-- {ciclo['fonte']}
-- Não edite à mão: mude o programa_nc.json e rode o script de novo.
--
-- Pré-requisitos: supabase/migrations/20260914000001_metodo_nc.sql e
-- 20260914000002_ancora_em_qualquer_dia.sql (a âncora do método é uma terça).
-- Só grava o ciclo {ciclo['id']}: o ciclo antigo (cada-dia-1-treino) não é tocado.
-- Pode rodar quantas vezes quiser: é upsert, e treinos que saíram DESTE ciclo são removidos.

BEGIN;

INSERT INTO public.ciclos_treino (id, nome, ancora, dias, handle, fonte)
VALUES ({cid}, {sql_texto(ciclo['nome'])}, {sql_texto(ciclo['ancora'])},
        {ciclo['dias']}, {sql_texto(ciclo['handle'])}, {sql_texto(ciclo['fonte'])})
ON CONFLICT (id) DO UPDATE SET
  nome = EXCLUDED.nome, ancora = EXCLUDED.ancora, dias = EXCLUDED.dias,
  handle = EXCLUDED.handle, fonte = EXCLUDED.fonte;

INSERT INTO public.treinos_ciclo ({", ".join(COLUNAS)}, phases)
VALUES
{valores}
ON CONFLICT (id) DO UPDATE SET
  {atualizar};

DELETE FROM public.treinos_ciclo
WHERE ciclo_id = {cid} AND id NOT IN ({ids});

-- Conferência: cada dia do ciclo com os três níveis, cada treino com objetivo e
-- zona. Se faltar algo, a exceção desfaz a transação e o banco fica como estava.
DO $conferencia$
DECLARE
  faltando INT;
  sem_metodo INT;
BEGIN
  SELECT count(*) INTO faltando
  FROM public.ciclos_treino c
  CROSS JOIN generate_series(1, c.dias) AS d(dia)
  CROSS JOIN (VALUES ('INICIANTE'), ('INTERMEDIARIO'), ('AVANCADO')) AS n(level)
  LEFT JOIN public.treinos_ciclo t
    ON t.ciclo_id = c.id AND t.ciclo_dia = d.dia AND t.level = n.level
  WHERE c.id = {cid} AND t.id IS NULL;

  SELECT count(*) INTO sem_metodo
  FROM public.treinos_ciclo
  WHERE ciclo_id = {cid} AND (objetivo IS NULL OR zona IS NULL);

  IF faltando > 0 OR sem_metodo > 0 THEN
    RAISE EXCEPTION 'Ciclo incompleto: % treino(s) faltando, % sem objetivo/zona. Nada foi gravado.',
      faltando, sem_metodo;
  END IF;
END
$conferencia$;

COMMIT;
"""


def main() -> int:
    ap = argparse.ArgumentParser(description="Converte o programa do Método NC em treinos sugeridos do Aquagenda.")
    ap.add_argument("--fonte", help="URL ou caminho do programa_nc.json (padrão: ../natacao-treinos/programa_nc.json)")
    args = ap.parse_args()

    dados, fonte = ler_fonte(args.fonte)
    ciclo, linhas, problemas = converter(dados, fonte)
    if problemas:
        print("O programa tem problemas; nada foi gerado:")
        for p in problemas:
            print(f"  - {p}")
        return 1

    SAIDA_SQL.parent.mkdir(parents=True, exist_ok=True)
    SAIDA_SQL.write_text(montar_sql(ciclo, linhas), encoding="utf-8", newline="\n")
    SAIDA_JSON.parent.mkdir(parents=True, exist_ok=True)
    SAIDA_JSON.write_text(json.dumps({**ciclo, "treinos": linhas}, ensure_ascii=False, indent=1) + "\n",
                          encoding="utf-8", newline="\n")

    for dia in range(1, ciclo["dias"] + 1):
        do_dia = [l for l in linhas if l["ciclo_dia"] == dia]
        print(f"  dia {dia:2d}/{ciclo['dias']}  {do_dia[0]['bloco']:11s} {do_dia[0]['foco']:16s} "
              + " / ".join(f"{l['zona']} {l['total_distance_meters']}m ~{l['estimated_minutes']}min" for l in do_dia))
    print(f"\nOK: {len(linhas)} treinos do ciclo {CICLO_ID} (âncora {ciclo['ancora']}).")
    print(f"  {SAIDA_SQL.relative_to(RAIZ)}")
    print(f"  {SAIDA_JSON.relative_to(RAIZ)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
