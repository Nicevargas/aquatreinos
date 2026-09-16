-- ==============================================================================
-- AQUAGENDA - MODO ÁGUAS ABERTAS
-- Arquivo: 20260917000001_aguas_abertas.sql
--
-- Um segundo programa diário, o de águas abertas (treinos na piscina que
-- preparam mar, lago e travessias), ao lado do de piscina.
--
--   1. ciclos_treino ganha "modo": piscina ou aguas_abertas. Os ciclos que já
--      existem ficam como piscina.
--   2. treinos_sugeridos ganha p_modo (padrão piscina) e só escolhe ciclos
--      daquele modo. Sem isso, o ciclo de águas abertas, por ser o mais recente,
--      tomaria o lugar do de piscina para todo mundo.
--
-- Não apaga nem altera treinos. O app antigo chama só com p_data e p_level e
-- continua recebendo o treino de piscina.
-- Depois dela, rode supabase/seed/programa_aa.sql.
-- ==============================================================================

-- 1. MODO DO CICLO
ALTER TABLE public.ciclos_treino ADD COLUMN IF NOT EXISTS modo TEXT NOT NULL DEFAULT 'piscina';

ALTER TABLE public.ciclos_treino DROP CONSTRAINT IF EXISTS ciclos_treino_modo_check;
ALTER TABLE public.ciclos_treino ADD CONSTRAINT ciclos_treino_modo_check
    CHECK (modo IN ('piscina', 'aguas_abertas'));

-- 2. SUGESTÃO DO DIA POR MODO
-- A assinatura muda (p_modo), por isso DROP + CREATE: com as duas versões no
-- banco, a chamada só com p_data e p_level ficaria ambígua.
DROP FUNCTION IF EXISTS public.treinos_sugeridos(DATE, TEXT, TEXT);
DROP FUNCTION IF EXISTS public.treinos_sugeridos(DATE, TEXT, TEXT, TEXT);

CREATE FUNCTION public.treinos_sugeridos(
    p_data DATE DEFAULT (now() AT TIME ZONE 'America/Sao_Paulo')::date,
    p_level TEXT DEFAULT NULL,
    p_ciclo TEXT DEFAULT NULL,
    p_modo TEXT DEFAULT 'piscina'
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
    objetivo TEXT,
    zona TEXT,
    ajuste TEXT,
    is_completed BOOLEAN,
    is_suggestion BOOLEAN,
    phases JSONB
)
LANGUAGE sql
STABLE
SET search_path = public
AS $$
    WITH ciclo AS (
        SELECT c.id, c.ancora, c.dias
        FROM public.ciclos_treino c
        WHERE c.ativo
          AND (p_ciclo IS NULL OR c.id = p_ciclo)
          -- Pedindo um ciclo pelo id, vale o id; senão, só os ciclos do modo.
          AND (p_ciclo IS NOT NULL OR c.modo = coalesce(lower(p_modo), 'piscina'))
          AND EXISTS (SELECT 1 FROM public.treinos_ciclo t WHERE t.ciclo_id = c.id)
        ORDER BY (c.ancora <= p_data) DESC,
                 CASE WHEN c.ancora <= p_data THEN c.ancora END DESC NULLS LAST,
                 c.ancora
        LIMIT 1
    )
    SELECT
        t.id, t.ciclo_id, t.ciclo_dia, t.semana, t.bloco, t.foco, t.level,
        t.nivel_carrossel, t.title, t.subtitle, t.tag,
        p_data, t.total_distance_meters, t.estimated_minutes, t.calories,
        t.motivational_tip, t.objetivo, t.zona, t.ajuste, FALSE, TRUE, t.phases
    FROM ciclo c
    JOIN public.treinos_ciclo t ON t.ciclo_id = c.id
    WHERE t.ciclo_dia = (((p_data - c.ancora) % c.dias) + c.dias) % c.dias + 1
      AND (p_level IS NULL OR t.level = upper(p_level))
    ORDER BY CASE t.level WHEN 'INICIANTE' THEN 1 WHEN 'INTERMEDIARIO' THEN 2 ELSE 3 END;
$$;

GRANT EXECUTE ON FUNCTION public.treinos_sugeridos(DATE, TEXT, TEXT, TEXT) TO anon, authenticated;
