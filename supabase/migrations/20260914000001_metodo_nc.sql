-- ==============================================================================
-- AQUAGENDA - MÉTODO NATAÇÃO CRIATIVA NOS TREINOS SUGERIDOS
-- Arquivo: 20260914000001_metodo_nc.sql
--
-- A partir de 28/09/2026 o carrossel "Cada Dia 1 Treino" segue o Método NC:
-- blocos Ativação -> Preparação -> Desenvolvimento -> Consolidação -> Recuperação,
-- zona de intensidade (A0-AA), PSE e corretivos. O programa novo entra como um
-- SEGUNDO ciclo (metodo-nc); o ciclo antigo continua como está.
--
-- Esta migração não apaga nem altera dados:
--   1. acrescenta três colunas opcionais em treinos_ciclo (vazias no ciclo antigo);
--   2. troca a função treinos_sugeridos para escolher o ciclo pela data.
--
-- Depois dela, rode supabase/seed/programa_nc.sql. Enquanto o seed não roda,
-- a função continua devolvendo o ciclo antigo.
-- ==============================================================================

-- 1. O QUE O MÉTODO ACRESCENTA A CADA TREINO
ALTER TABLE public.treinos_ciclo ADD COLUMN IF NOT EXISTS objetivo TEXT;
ALTER TABLE public.treinos_ciclo ADD COLUMN IF NOT EXISTS zona TEXT;
ALTER TABLE public.treinos_ciclo ADD COLUMN IF NOT EXISTS ajuste TEXT;

ALTER TABLE public.treinos_ciclo DROP CONSTRAINT IF EXISTS treinos_ciclo_zona_check;
ALTER TABLE public.treinos_ciclo ADD CONSTRAINT treinos_ciclo_zona_check
    CHECK (zona IS NULL OR zona IN ('A0', 'A1', 'A2', 'A3', 'AN', 'AA'));

-- 2. SUGESTÃO DO DIA, AGORA ESCOLHENDO O CICLO PELA DATA
-- O tipo de retorno muda (objetivo, zona, ajuste), por isso DROP + CREATE.
-- Sem p_ciclo, vale o ciclo ativo de âncora mais recente que já começou em
-- p_data; para datas antes de todos, o mais antigo. O app antigo chama só com
-- p_data e p_level e continua funcionando: os campos novos ele ignora.
DROP FUNCTION IF EXISTS public.treinos_sugeridos(DATE, TEXT, TEXT);

CREATE FUNCTION public.treinos_sugeridos(
    p_data DATE DEFAULT (now() AT TIME ZONE 'America/Sao_Paulo')::date,
    p_level TEXT DEFAULT NULL,
    p_ciclo TEXT DEFAULT NULL
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
          -- Só ciclo com treinos: um ciclo cadastrado sem o seed não vira sugestão vazia.
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
    -- date - date dá inteiro; o duplo módulo acerta datas antes da âncora.
    WHERE t.ciclo_dia = (((p_data - c.ancora) % c.dias) + c.dias) % c.dias + 1
      AND (p_level IS NULL OR t.level = upper(p_level))
    ORDER BY CASE t.level WHEN 'INICIANTE' THEN 1 WHEN 'INTERMEDIARIO' THEN 2 ELSE 3 END;
$$;

GRANT EXECUTE ON FUNCTION public.treinos_sugeridos(DATE, TEXT, TEXT) TO anon, authenticated;
