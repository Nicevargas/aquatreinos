"""
programa_aa_para_supabase.py - Leva o programa de ÁGUAS ABERTAS do Método NC
(natacao-treinos/programa_aa.json, o mesmo que gera os slides 5 e 6 do
carrossel) aos treinos sugeridos do Aquagenda, no modo aguas_abertas.

    supabase/seed/programa_aa.sql          -> rodar no SQL Editor, DEPOIS da
                                              migração 20260917000001_aguas_abertas.sql
    app/src/main/assets/programa_aa.json   -> cópia embarcada, usada offline

Cada treino tem as sete partes da arte (Respiração, Corretivos, Ativação,
Pernas + braço, Desenvolvimento, Consolidação e Recuperação), que viram as fases
do treino no app.

Só Condicionamento (INTERMEDIARIO) e Aperfeiçoamento (AVANCADO). A conversão de
série, duração e porcentagens é a mesma do programa de piscina
(programa_nc_para_supabase.py); a auditoria do método mora em
natacao-treinos/scripts/programa_aa.py.

Uso:
    python scripts/programa_aa_para_supabase.py
    python scripts/programa_aa_para_supabase.py --fonte ../natacao-treinos/programa_aa.json
"""
import argparse
import json
import sys
import urllib.request
from datetime import date
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import programa_nc_para_supabase as nc  # noqa: E402

RAIZ = nc.RAIZ
FONTE_LOCAL = RAIZ.parent / "natacao-treinos" / "programa_aa.json"
FONTE_REMOTA = "https://raw.githubusercontent.com/Nicevargas/natacao-treinos/main/programa_aa.json"
SAIDA_SQL = RAIZ / "supabase" / "seed" / "programa_aa.sql"
SAIDA_JSON = RAIZ / "app" / "src" / "main" / "assets" / "programa_aa.json"

CICLO_ID = "aguas-abertas"
CICLO_NOME = "Cada Dia 1 Treino · Águas abertas"
MODO = "aguas_abertas"

NIVEIS = {"amarelo": "INTERMEDIARIO", "vermelho": "AVANCADO"}
# As sete partes da arte de águas abertas, na ordem (natacao-treinos/scripts/programa_aa.py).
PARTES = (("respiracao", "Respiração"), ("corretivos", "Corretivos"), ("ativacao", "Ativação"),
          ("pernas_braco", "Pernas + braço"), ("desenvolvimento", "Desenvolvimento"),
          ("consolidacao", "Consolidação"), ("recuperacao", "Recuperação"))


def ler_fonte(fonte: str | None) -> tuple[dict, str]:
    if fonte is None:
        fonte = str(FONTE_LOCAL) if FONTE_LOCAL.exists() else FONTE_REMOTA
    if fonte.startswith(("http://", "https://")):
        with urllib.request.urlopen(fonte, timeout=30) as r:
            return json.loads(r.read().decode("utf-8")), fonte
    return json.loads(Path(fonte).read_text(encoding="utf-8")), FONTE_REMOTA


def converter_nivel(t: dict, dia: int, dias: int, cor: str, rotulo: dict, seguranca: str) -> dict:
    nivel = t["niveis"][cor]
    level = NIVEIS[cor]
    desconhecidas = set(nivel["partes"]) - {p for p, _ in PARTES}
    if desconhecidas:
        raise ValueError(f"parte(s) desconhecida(s): {sorted(desconhecidas)}")

    fases, segundos = [], 0.0
    for chave, titulo in PARTES:
        series = nivel["partes"].get(chave)
        if not series:
            continue
        fase_id = f"aa_d{dia:02d}_{cor}_{chave}"
        sets = []
        for i, s in enumerate(series):
            conv, seg = nc.ler_serie(s, cor, fase_id, i)
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

    for fase, pct in zip(fases, nc.porcentagens([f["distanceMeters"] for f in fases])):
        fase["percentage"] = pct

    minutos = round(segundos / 60)
    return {
        "id": f"aa_d{dia:02d}_{level.lower()}",
        "ciclo_id": CICLO_ID,
        "ciclo_dia": dia,
        "semana": t.get("semana"),
        "bloco": t.get("mesociclo"),
        "foco": t["foco"],
        "level": level,
        "nivel_carrossel": cor,
        "title": t["foco"],
        "subtitle": f"{rotulo['nome']} · {t.get('mesociclo')}",
        "tag": f"Águas abertas · Dia {dia}/{dias}",
        "total_distance_meters": sum(f["distanceMeters"] for f in fases),
        "estimated_minutes": minutos,
        "calories": round(minutos * nc.KCAL_POR_MINUTO / 10) * 10,
        # A dica do dia é a habilidade treinada + o aviso de segurança do modo.
        "motivational_tip": f"{t['habilidade']}. {seguranca}",
        "objetivo": nivel["objetivo"],
        "zona": nivel["zona"],
        "ajuste": nivel["ajuste"],
        "phases": fases,
    }


def converter(dados: dict, fonte: str) -> tuple:
    lista = dados["treinos"]
    date.fromisoformat(dados["ancora"])
    problemas = []
    if len(lista) % 7:
        problemas.append(f"O ciclo tem {len(lista)} dias; precisa ser múltiplo de 7.")
    seguranca = str(dados.get("seguranca", "")).strip()
    if not seguranca:
        problemas.append("Falta o aviso de segurança (\"seguranca\").")

    linhas = []
    for idx, t in enumerate(lista):
        dia = idx + 1
        if t.get("dia") != dia:
            problemas.append(f"Posição {dia} traz 'dia': {t.get('dia')}.")
        for cor in NIVEIS:
            if cor not in t["niveis"]:
                problemas.append(f"dia {dia}: falta o nível {cor}.")
                continue
            try:
                linhas.append(converter_nivel(t, dia, len(lista), cor, dados["rotulos"][cor], seguranca))
            except (ValueError, KeyError) as e:
                problemas.append(f"dia {dia}/{cor}: {e}")

    ciclo = {"id": CICLO_ID, "nome": CICLO_NOME, "ancora": dados["ancora"], "dias": len(lista),
             "handle": dados.get("handle"), "fonte": fonte, "modo": MODO}
    return ciclo, linhas, problemas


def montar_sql(ciclo: dict, linhas: list) -> str:
    colunas = nc.COLUNAS
    valores = ",\n".join(
        "  (" + ", ".join(nc.sql_texto(l[c]) for c in colunas) + ",\n   " + nc.sql_json(l["phases"]) + ")"
        for l in linhas)
    atualizar = ",\n  ".join(f"{c} = EXCLUDED.{c}" for c in (*colunas[1:], "phases"))
    ids = ", ".join(nc.sql_texto(l["id"]) for l in linhas)
    cid = nc.sql_texto(ciclo["id"])
    t = nc.sql_texto

    return f"""-- GERADO por scripts/programa_aa_para_supabase.py a partir de
-- {ciclo['fonte']}
-- Não edite à mão: mude o programa_aa.json e rode o script de novo.
--
-- Pré-requisito: supabase/migrations/20260917000001_aguas_abertas.sql (coluna
-- modo e treinos_sugeridos por modo). Sem ela, este ciclo tomaria o lugar do de
-- piscina; por isso a primeira linha confere que a coluna existe.
-- Só grava o ciclo {ciclo['id']}: os ciclos de piscina não são tocados.
-- Pode rodar quantas vezes quiser: é upsert.

BEGIN;

DO $migracao$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                 WHERE table_schema = 'public' AND table_name = 'ciclos_treino' AND column_name = 'modo') THEN
    RAISE EXCEPTION 'Rode antes a migração 20260917000001_aguas_abertas.sql. Nada foi gravado.';
  END IF;
END
$migracao$;

INSERT INTO public.ciclos_treino (id, nome, ancora, dias, handle, fonte, modo)
VALUES ({cid}, {t(ciclo['nome'])}, {t(ciclo['ancora'])},
        {ciclo['dias']}, {t(ciclo['handle'])}, {t(ciclo['fonte'])}, {t(ciclo['modo'])})
ON CONFLICT (id) DO UPDATE SET
  nome = EXCLUDED.nome, ancora = EXCLUDED.ancora, dias = EXCLUDED.dias,
  handle = EXCLUDED.handle, fonte = EXCLUDED.fonte, modo = EXCLUDED.modo;

INSERT INTO public.treinos_ciclo ({", ".join(colunas)}, phases)
VALUES
{valores}
ON CONFLICT (id) DO UPDATE SET
  {atualizar};

DELETE FROM public.treinos_ciclo
WHERE ciclo_id = {cid} AND id NOT IN ({ids});

-- Conferência: cada dia com Condicionamento e Aperfeiçoamento, com objetivo e
-- zona, e o treino de piscina de hoje continua vindo do ciclo de piscina.
DO $conferencia$
DECLARE
  faltando INT;
  sem_metodo INT;
  modo_de_hoje TEXT;
BEGIN
  SELECT count(*) INTO faltando
  FROM public.ciclos_treino c
  CROSS JOIN generate_series(1, c.dias) AS d(dia)
  CROSS JOIN (VALUES ('INTERMEDIARIO'), ('AVANCADO')) AS n(level)
  LEFT JOIN public.treinos_ciclo tc
    ON tc.ciclo_id = c.id AND tc.ciclo_dia = d.dia AND tc.level = n.level
  WHERE c.id = {cid} AND tc.id IS NULL;

  SELECT count(*) INTO sem_metodo
  FROM public.treinos_ciclo
  WHERE ciclo_id = {cid} AND (objetivo IS NULL OR zona IS NULL);

  SELECT c.modo INTO modo_de_hoje
  FROM public.treinos_sugeridos(p_level => 'INTERMEDIARIO') s
  JOIN public.ciclos_treino c ON c.id = s.ciclo_id;

  IF faltando > 0 OR sem_metodo > 0 OR modo_de_hoje IS DISTINCT FROM 'piscina' THEN
    RAISE EXCEPTION 'Ciclo incompleto (% faltando, % sem objetivo/zona) ou treino de piscina trocado (%). Nada foi gravado.',
      faltando, sem_metodo, modo_de_hoje;
  END IF;
END
$conferencia$;

COMMIT;
"""


def main() -> int:
    ap = argparse.ArgumentParser(description="Converte o programa de águas abertas em treinos sugeridos do Aquagenda.")
    ap.add_argument("--fonte", help="URL ou caminho do programa_aa.json (padrão: ../natacao-treinos/programa_aa.json)")
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
        print(f"  dia {dia:2d}/{ciclo['dias']}  {do_dia[0]['bloco']:11s} {do_dia[0]['foco']:17s} "
              + " / ".join(f"{l['zona']} {l['total_distance_meters']}m ~{l['estimated_minutes']}min" for l in do_dia))
    print(f"\nOK: {len(linhas)} treinos do ciclo {CICLO_ID} (âncora {ciclo['ancora']}).")
    print(f"  {SAIDA_SQL.relative_to(RAIZ)}")
    print(f"  {SAIDA_JSON.relative_to(RAIZ)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
