"""
carrossel_para_supabase.py - Converte o programa do carrossel "Cada Dia 1 Treino"
(@natacaocriativa) nos treinos sugeridos do Aquagenda.

A fonte é o treinos.json do repositório natacao-treinos, o MESMO arquivo que gera
o carrossel publicado todo dia às 6h. Daqui saem dois arquivos, da mesma
conversão, para o banco e o app nunca divergirem:

    supabase/seed/treinos_ciclo.sql         -> rodar no SQL Editor do Supabase
    app/src/main/assets/treinos_ciclo.json  -> cópia embarcada, usada offline

Não se grava uma linha por data: o banco guarda os 28 dias x 3 níveis e a
função public.treinos_sugeridos(data, nível) escolhe o dia com a mesma conta do
carrossel, (data - âncora) mod 28. Assim o treino sugerido de hoje é sempre o do
carrossel de hoje, sem processo diário para falhar.

Quando o treinos.json mudar, rode de novo e reaplique o .sql.

Uso:
    python scripts/carrossel_para_supabase.py
    python scripts/carrossel_para_supabase.py --fonte ../natacao-treinos/treinos.json
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
FONTE_PADRAO = "https://raw.githubusercontent.com/Nicevargas/natacao-treinos/main/treinos.json"
SAIDA_SQL = RAIZ / "supabase" / "seed" / "treinos_ciclo.sql"
SAIDA_JSON = RAIZ / "app" / "src" / "main" / "assets" / "treinos_ciclo.json"

CICLO_ID = "cada-dia-1-treino"
CICLO_NOME = "Cada Dia 1 Treino"

# O carrossel tem três níveis por cor; o app e o banco falam em nível de treino.
NIVEIS = {"verde": "INICIANTE", "amarelo": "INTERMEDIARIO", "vermelho": "AVANCADO"}
FASES = (("aquecimento", "Aquecimento"), ("principal", "Principal"), ("final", "Final"))

# Mesma leitura de distância de natacao-treinos/scripts/treino.py: o cabeçalho
# carrega a metragem ("8x50m" = 400m) e os detalhes não somam.
_REPS = re.compile(r"^(\d+)\s*x\s*(\d+)\s*m\b\s*", re.I)
_SIMPLES = re.compile(r"^(\d+)\s*m\b\s*", re.I)
_SEGUNDOS = re.compile(r"(\d+)")

# Ordem importa só para a exibição: "Palmar + Pull buoy".
MATERIAIS = (
    ("Palmar", re.compile(r"palmar", re.I)),
    ("Pull buoy", re.compile(r"pull\s*buoy", re.I)),
    ("Nadadeira", re.compile(r"nadadeira", re.I)),
    ("Prancha", re.compile(r"prancha", re.I)),
)

# Mesmas frases da legenda do carrossel (natacao-treinos/scripts/postar_treino.py).
SOBRE_O_FOCO = {
    "Técnica": "Hoje o ganho não é de fôlego, é de percepção. Nadar devagar prestando atenção rende mais do que nadar rápido no automático.",
    "Aeróbico": "Ritmo constante do início ao fim. É esse tipo de treino, sem brilho nenhum, que constrói o fundo que aparece nos outros dias.",
    "Velocidade": "Tiro curto e descanso longo. Se o intervalo parecer generoso, é porque o esforço tem que ser de verdade máximo.",
    "Estilos": "Os quatro nados no mesmo treino. Nadar o que você não gosta costuma ser o que destrava o que você gosta.",
    "Volume": "O treino mais longo da semana. O objetivo não é velocidade: é chegar no fim com a técnica inteira.",
    "Material": "Nadadeira, palmar e pull buoy entram para ensinar sensação, não para facilitar. O que importa é o que muda quando você tira.",
    "Regenerativo": "Dia leve de propósito. Recuperação faz parte do treino — pular esse dia é o que atrapalha a semana seguinte.",
}

# O carrossel não informa tempo nem gasto calórico. São ESTIMATIVAS para os
# cards do app: ritmo médio de nado por nível somado aos intervalos das séries,
# e ~8 kcal por minuto de natação moderada.
RITMO_S_POR_100M = {"verde": 150, "amarelo": 130, "vermelho": 115}
KCAL_POR_MINUTO = 8


def ler_fonte(fonte: str) -> dict:
    if fonte.startswith(("http://", "https://")):
        with urllib.request.urlopen(fonte, timeout=30) as r:
            return json.loads(r.read().decode("utf-8"))
    return json.loads(Path(fonte).read_text(encoding="utf-8"))


def ler_serie(serie: dict, fase_id: str, i: int) -> dict:
    cab = serie["serie"].strip()
    if m := _REPS.match(cab):
        reps, dist = int(m.group(1)), int(m.group(2))
    elif m := _SIMPLES.match(cab):
        reps, dist = 1, int(m.group(1))
    else:
        raise ValueError(f"Série sem distância legível no cabeçalho: {cab!r}")

    detalhes = serie.get("detalhes", [])
    # "8x75m" sozinho no cabeçalho: o que nadar está nos detalhes ("25m Peito").
    nado = cab[m.end():].strip() or " · ".join(detalhes) or "Nado livre"
    intervalo = serie.get("intervalo")
    descanso = int(_SEGUNDOS.search(intervalo).group(1)) if intervalo else 0

    texto = " ".join([cab, *detalhes])
    materiais = [nome for nome, rx in MATERIAIS if rx.search(texto)]

    return {
        "id": f"{fase_id}_s{i + 1}",
        "serie": cab,
        "repsDescription": f"{reps}x{dist}",
        "stroke": nado,
        "details": detalhes,
        "distanceMeters": reps * dist,
        "interval": intervalo,
        "intensity": None,
        "restSeconds": descanso,
        "equipment": " + ".join(materiais) or None,
        "isDone": False,
        "_reps": reps,
    }


def porcentagens(metros: list) -> list:
    """Arredonda preservando a soma em 100 (maior resto)."""
    total = sum(metros)
    brutos = [m * 100 / total for m in metros]
    base = [int(b) for b in brutos]
    falta = 100 - sum(base)
    for i in sorted(range(len(brutos)), key=lambda i: brutos[i] - base[i], reverse=True)[:falta]:
        base[i] += 1
    return base


def converter_nivel(t: dict, dia: int, cor: str, rotulo: dict) -> dict:
    nivel = t["niveis"][cor]
    level = NIVEIS[cor]
    fases, segundos = [], 0

    for fase_chave, fase_titulo in FASES:
        fase_id = f"d{dia:02d}_{cor}_{fase_chave}"
        sets = [ler_serie(s, fase_id, i) for i, s in enumerate(nivel[fase_chave])]
        for s in sets:
            segundos += s["distanceMeters"] * RITMO_S_POR_100M[cor] / 100
            segundos += s.pop("_reps") * s["restSeconds"]
        fases.append({
            "id": fase_id,
            "title": fase_titulo,
            "summary": " + ".join(s["serie"] for s in sets),
            "distanceMeters": sum(s["distanceMeters"] for s in sets),
            "status": "PENDING",
            "sets": sets,
        })

    for fase, pct in zip(fases, porcentagens([f["distanceMeters"] for f in fases])):
        fase["percentage"] = pct

    total = sum(f["distanceMeters"] for f in fases)
    minutos = max(5, round(segundos / 60 / 5) * 5)

    return {
        "id": f"ciclo_d{dia:02d}_{level.lower()}",
        "ciclo_id": CICLO_ID,
        "ciclo_dia": dia,
        "semana": t.get("semana"),
        "bloco": t.get("bloco"),
        "foco": t["foco"],
        "level": level,
        "nivel_carrossel": cor,
        "title": t["foco"],
        "subtitle": f"{rotulo['nome']} · Bloco {t.get('bloco')}",
        "tag": f"Cada Dia 1 Treino · Dia {dia}/{{dias}}",
        "total_distance_meters": total,
        "estimated_minutes": minutos,
        "calories": round(minutos * KCAL_POR_MINUTO / 10) * 10,
        "motivational_tip": SOBRE_O_FOCO.get(t["foco"]),
        "phases": fases,
    }


def converter(dados: dict) -> tuple:
    ancora = date.fromisoformat(dados["ancora"])
    lista = dados["treinos"]
    problemas = []

    if ancora.weekday() != 0:
        problemas.append(f"A âncora {ancora} não é segunda-feira.")
    if len(lista) % 7:
        problemas.append(f"O ciclo tem {len(lista)} dias; precisa ser múltiplo de 7.")

    linhas = []
    for idx, t in enumerate(lista):
        # O carrossel escolhe pela POSIÇÃO na lista, não pelo campo "dia".
        dia = idx + 1
        if t.get("dia") != dia:
            problemas.append(f"Posição {dia} traz 'dia': {t.get('dia')}; o carrossel usaria a posição.")
        for cor in NIVEIS:
            if cor not in t["niveis"]:
                problemas.append(f"dia {dia}: falta o nível {cor}.")
                continue
            try:
                linha = converter_nivel(t, dia, cor, dados["rotulos"][cor])
            except ValueError as e:
                problemas.append(f"dia {dia}/{cor}: {e}")
                continue
            linha["tag"] = linha["tag"].replace("{dias}", str(len(lista)))
            linhas.append(linha)

    ciclo = {
        "id": CICLO_ID,
        "nome": CICLO_NOME,
        "ancora": dados["ancora"],
        "dias": len(lista),
        "handle": dados.get("handle"),
        "fonte": FONTE_PADRAO,
    }
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
           "estimated_minutes", "calories", "motivational_tip")


def montar_sql(ciclo: dict, linhas: list) -> str:
    valores = ",\n".join(
        "  (" + ", ".join(sql_texto(l[c]) for c in COLUNAS) + ",\n   " + sql_json(l["phases"]) + ")"
        for l in linhas)
    atualizar = ",\n  ".join(f"{c} = EXCLUDED.{c}" for c in (*COLUNAS[1:], "phases"))
    ids = ", ".join(sql_texto(l["id"]) for l in linhas)

    return f"""-- GERADO por scripts/carrossel_para_supabase.py a partir de
-- {ciclo['fonte']}
-- Não edite à mão: mude o treinos.json e rode o script de novo.
--
-- Pré-requisito: supabase/migrations/20260913000001_treinos_sugeridos_do_carrossel.sql
-- Pode rodar quantas vezes quiser: é upsert, e treinos que saíram do ciclo são removidos.

BEGIN;

INSERT INTO public.ciclos_treino (id, nome, ancora, dias, handle, fonte)
VALUES ({sql_texto(ciclo['id'])}, {sql_texto(ciclo['nome'])}, {sql_texto(ciclo['ancora'])},
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
WHERE ciclo_id = {sql_texto(ciclo['id'])} AND id NOT IN ({ids});

-- Conferência: cada dia do ciclo precisa ter os três níveis, e nada fora dele.
-- Se faltar algo, a exceção desfaz a transação inteira e o banco fica como estava.
DO $conferencia$
DECLARE
  faltando INT;
  sobrando INT;
BEGIN
  SELECT count(*) INTO faltando
  FROM public.ciclos_treino c
  CROSS JOIN generate_series(1, c.dias) AS d(dia)
  CROSS JOIN (VALUES ('INICIANTE'), ('INTERMEDIARIO'), ('AVANCADO')) AS n(level)
  LEFT JOIN public.treinos_ciclo t
    ON t.ciclo_id = c.id AND t.ciclo_dia = d.dia AND t.level = n.level
  WHERE c.id = {sql_texto(ciclo['id'])} AND t.id IS NULL;

  SELECT count(*) INTO sobrando
  FROM public.treinos_ciclo t
  JOIN public.ciclos_treino c ON c.id = t.ciclo_id
  WHERE c.id = {sql_texto(ciclo['id'])} AND t.ciclo_dia > c.dias;

  IF faltando > 0 OR sobrando > 0 THEN
    RAISE EXCEPTION 'Ciclo incompleto: % treino(s) faltando, % fora do ciclo. Nada foi gravado.',
      faltando, sobrando;
  END IF;
END
$conferencia$;

COMMIT;
"""


def main() -> int:
    ap = argparse.ArgumentParser(description="Converte o carrossel em treinos sugeridos do Aquagenda.")
    ap.add_argument("--fonte", default=FONTE_PADRAO, help="URL ou caminho do treinos.json")
    args = ap.parse_args()

    dados = ler_fonte(args.fonte)
    ciclo, linhas, problemas = converter(dados)
    if problemas:
        print("O programa de treinos tem problemas; nada foi gerado:")
        for p in problemas:
            print(f"  - {p}")
        return 1

    SAIDA_SQL.parent.mkdir(parents=True, exist_ok=True)
    SAIDA_SQL.write_text(montar_sql(ciclo, linhas), encoding="utf-8", newline="\n")

    SAIDA_JSON.parent.mkdir(parents=True, exist_ok=True)
    SAIDA_JSON.write_text(
        json.dumps({**ciclo, "treinos": linhas}, ensure_ascii=False, indent=1) + "\n",
        encoding="utf-8", newline="\n")

    for dia in range(1, ciclo["dias"] + 1):
        do_dia = [l for l in linhas if l["ciclo_dia"] == dia]
        print(f"  dia {dia:2d}/{ciclo['dias']}  {do_dia[0]['bloco']:12s} {do_dia[0]['foco']:13s} "
              + " / ".join(f"{l['total_distance_meters']}m ~{l['estimated_minutes']}min" for l in do_dia))
    print(f"\nOK: {len(linhas)} treinos.")
    print(f"  {SAIDA_SQL.relative_to(RAIZ)}")
    print(f"  {SAIDA_JSON.relative_to(RAIZ)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
