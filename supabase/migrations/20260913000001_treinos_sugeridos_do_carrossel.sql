-- ==============================================================================
-- AQUAGENDA - TREINOS SUGERIDOS A PARTIR DO CARROSSEL "CADA DIA 1 TREINO"
-- Arquivo: 20260913000001_treinos_sugeridos_do_carrossel.sql
--
-- O carrossel do @natacaocriativa sai todo dia de um ciclo fixo de 28 dias com
-- três níveis. Aqui ficam guardados esses 84 treinos, e a função
-- treinos_sugeridos(data, nível) devolve o de qualquer data com a mesma conta do
-- carrossel: (data - âncora) mod dias. Nada precisa rodar todo dia.
--
-- Os dados vêm de supabase/seed/treinos_ciclo.sql, gerado por
-- scripts/carrossel_para_supabase.py. Rode esta migração antes do seed.
-- ==============================================================================

-- 1. TRÊS NÍVEIS, COMO NO CARROSSEL (🟢 verde / 🟡 amarelo / 🔴 vermelho)
ALTER TABLE public.profiles DROP CONSTRAINT IF EXISTS profiles_training_level_check;
ALTER TABLE public.profiles ADD CONSTRAINT profiles_training_level_check
    CHECK (training_level IN ('INICIANTE', 'INTERMEDIARIO', 'AVANCADO'));

ALTER TABLE public.workouts DROP CONSTRAINT IF EXISTS workouts_level_check;
ALTER TABLE public.workouts ADD CONSTRAINT workouts_level_check
    CHECK (level IN ('INICIANTE', 'INTERMEDIARIO', 'AVANCADO'));

-- 2. CICLOS DE TREINO
CREATE TABLE IF NOT EXISTS public.ciclos_treino (
    id TEXT PRIMARY KEY,
    nome TEXT NOT NULL,
    -- Segunda-feira: com ciclo múltiplo de 7, cada posição cai sempre no mesmo
    -- dia da semana (seg técnica, ter aeróbico ... dom regenerativo).
    ancora DATE NOT NULL CHECK (EXTRACT(ISODOW FROM ancora) = 1),
    dias INT NOT NULL CHECK (dias > 0 AND dias % 7 = 0),
    handle TEXT,
    fonte TEXT,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 3. OS TREINOS DE CADA DIA DO CICLO, UM POR NÍVEL
-- phases usa o mesmo formato JSONB de public.workouts, então o app lê os dois
-- com o mesmo código.
CREATE TABLE IF NOT EXISTS public.treinos_ciclo (
    id TEXT PRIMARY KEY,
    ciclo_id TEXT NOT NULL REFERENCES public.ciclos_treino(id) ON DELETE CASCADE,
    ciclo_dia INT NOT NULL CHECK (ciclo_dia >= 1),
    semana INT,
    bloco TEXT,
    foco TEXT NOT NULL,
    level TEXT NOT NULL CHECK (level IN ('INICIANTE', 'INTERMEDIARIO', 'AVANCADO')),
    nivel_carrossel TEXT NOT NULL CHECK (nivel_carrossel IN ('verde', 'amarelo', 'vermelho')),
    title TEXT NOT NULL,
    subtitle TEXT,
    tag TEXT,
    total_distance_meters INT NOT NULL CHECK (total_distance_meters > 0),
    estimated_minutes INT,
    calories INT,
    motivational_tip TEXT,
    phases JSONB NOT NULL CHECK (jsonb_typeof(phases) = 'array'),
    created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL,
    UNIQUE (ciclo_id, ciclo_dia, level)
);

CREATE INDEX IF NOT EXISTS idx_treinos_ciclo_dia ON public.treinos_ciclo(ciclo_id, ciclo_dia);

DROP TRIGGER IF EXISTS tr_ciclos_treino_updated_at ON public.ciclos_treino;
CREATE TRIGGER tr_ciclos_treino_updated_at
    BEFORE UPDATE ON public.ciclos_treino
    FOR EACH ROW EXECUTE FUNCTION public.handle_updated_at();

DROP TRIGGER IF EXISTS tr_treinos_ciclo_updated_at ON public.treinos_ciclo;
CREATE TRIGGER tr_treinos_ciclo_updated_at
    BEFORE UPDATE ON public.treinos_ciclo
    FOR EACH ROW EXECUTE FUNCTION public.handle_updated_at();

-- 4. RLS: TODO MUNDO LÊ, NINGUÉM ESCREVE PELA API
-- Sem política de INSERT/UPDATE/DELETE, a chave anon do app não altera o
-- programa. Quem grava é o seed rodado no SQL Editor (ou a service_role).
ALTER TABLE public.ciclos_treino ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.treinos_ciclo ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Ciclos de treino são públicos para leitura" ON public.ciclos_treino;
CREATE POLICY "Ciclos de treino são públicos para leitura"
    ON public.ciclos_treino FOR SELECT
    USING (true);

DROP POLICY IF EXISTS "Treinos do ciclo são públicos para leitura" ON public.treinos_ciclo;
CREATE POLICY "Treinos do ciclo são públicos para leitura"
    ON public.treinos_ciclo FOR SELECT
    USING (true);

-- 5. SUGESTÃO DO DIA
-- Chamada pelo app: POST /rest/v1/rpc/treinos_sugeridos
--   {"p_data": "2026-09-13", "p_level": "INTERMEDIARIO"}
-- Sem p_level devolve os três níveis do dia. A data padrão é hoje em Brasília,
-- porque o carrossel sai às 6h de lá e o servidor roda em UTC.
CREATE OR REPLACE FUNCTION public.treinos_sugeridos(
    p_data DATE DEFAULT (now() AT TIME ZONE 'America/Sao_Paulo')::date,
    p_level TEXT DEFAULT NULL,
    p_ciclo TEXT DEFAULT 'cada-dia-1-treino'
)
RETURNS TABLE (
    id TEXT,
    ciclo_id TEXT,
    ciclo_dia INT,
    semana INT,
    bloco TEXT,
    foco TEXT,
    level TEXT,
    nivel_carrossel TEXT,
    title TEXT,
    subtitle TEXT,
    tag TEXT,
    workout_date DATE,
    total_distance_meters INT,
    estimated_minutes INT,
    calories INT,
    motivational_tip TEXT,
    is_completed BOOLEAN,
    is_suggestion BOOLEAN,
    phases JSONB
)
LANGUAGE sql
STABLE
SET search_path = public
AS $$
    SELECT
        t.id, t.ciclo_id, t.ciclo_dia, t.semana, t.bloco, t.foco, t.level,
        t.nivel_carrossel, t.title, t.subtitle, t.tag,
        p_data, t.total_distance_meters, t.estimated_minutes, t.calories,
        t.motivational_tip, FALSE, TRUE, t.phases
    FROM public.ciclos_treino c
    JOIN public.treinos_ciclo t ON t.ciclo_id = c.id
    WHERE c.id = p_ciclo
      AND c.ativo
      -- date - date dá inteiro; o duplo módulo acerta datas antes da âncora.
      AND t.ciclo_dia = (((p_data - c.ancora) % c.dias) + c.dias) % c.dias + 1
      AND (p_level IS NULL OR t.level = upper(p_level))
    ORDER BY CASE t.level WHEN 'INICIANTE' THEN 1 WHEN 'INTERMEDIARIO' THEN 2 ELSE 3 END;
$$;

GRANT SELECT ON public.ciclos_treino, public.treinos_ciclo TO anon, authenticated;
GRANT EXECUTE ON FUNCTION public.treinos_sugeridos(DATE, TEXT, TEXT) TO anon, authenticated;
