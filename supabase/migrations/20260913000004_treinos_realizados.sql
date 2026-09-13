-- ==============================================================================
-- AQUAGENDA - TREINOS REALIZADOS ("CONCLUIR TREINO")
-- Arquivo: 20260913000004_treinos_realizados.sql
--
-- Cada vez que a pessoa conclui um treino na execução ao vivo, o app grava aqui
-- o que foi feito: metros e séries (planejados e feitos), tempo, e as notas de
-- Intensidade e Complexidade de 0 a 10 que o carrossel "Cada Dia 1 Treino" pede
-- na legenda.
--
-- Um gatilho mantém public.swimmer_stats em dia (metros, tempo e quantidade de
-- treinos) e marca como concluído o treino de "Meus treinos" que foi feito.
--
-- RLS: cada um vê, registra e apaga só os próprios. Não há UPDATE: o registro
-- é o que aconteceu; para corrigir, apaga e registra de novo.
-- Transação única com conferência no fim.
-- ==============================================================================

BEGIN;

CREATE TABLE IF NOT EXISTS public.treinos_realizados (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL DEFAULT auth.uid() REFERENCES auth.users(id) ON DELETE CASCADE,
    -- De onde veio o treino: um de "Meus treinos" ou a sugestão do ciclo.
    workout_id TEXT REFERENCES public.workouts(id) ON DELETE SET NULL,
    treino_ciclo_id TEXT REFERENCES public.treinos_ciclo(id) ON DELETE SET NULL,
    titulo TEXT NOT NULL CHECK (char_length(titulo) BETWEEN 1 AND 200),
    foco TEXT,
    nivel TEXT NOT NULL CHECK (nivel IN ('INICIANTE', 'INTERMEDIARIO', 'AVANCADO')),
    data_treino DATE NOT NULL,
    metros_planejados INT NOT NULL CHECK (metros_planejados >= 0),
    metros_feitos INT NOT NULL CHECK (metros_feitos >= 0 AND metros_feitos <= metros_planejados),
    series_planejadas INT NOT NULL CHECK (series_planejadas >= 0),
    series_feitas INT NOT NULL CHECK (series_feitas >= 0 AND series_feitas <= series_planejadas),
    duracao_segundos INT NOT NULL CHECK (duracao_segundos BETWEEN 0 AND 86400),
    intensidade SMALLINT CHECK (intensidade BETWEEN 0 AND 10),
    complexidade SMALLINT CHECK (complexidade BETWEEN 0 AND 10),
    observacao TEXT CHECK (char_length(observacao) <= 500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

CREATE INDEX IF NOT EXISTS idx_treinos_realizados_usuario_data
    ON public.treinos_realizados(user_id, data_treino DESC);

ALTER TABLE public.treinos_realizados ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Ver os próprios treinos realizados" ON public.treinos_realizados;
CREATE POLICY "Ver os próprios treinos realizados"
    ON public.treinos_realizados FOR SELECT TO authenticated
    USING (user_id = auth.uid());

DROP POLICY IF EXISTS "Registrar só treinos próprios" ON public.treinos_realizados;
CREATE POLICY "Registrar só treinos próprios"
    ON public.treinos_realizados FOR INSERT TO authenticated
    WITH CHECK (user_id = auth.uid());

DROP POLICY IF EXISTS "Apagar os próprios treinos realizados" ON public.treinos_realizados;
CREATE POLICY "Apagar os próprios treinos realizados"
    ON public.treinos_realizados FOR DELETE TO authenticated
    USING (user_id = auth.uid());

GRANT SELECT, INSERT, DELETE ON public.treinos_realizados TO authenticated;

-- ------------------------------------------------------------------------------
-- ESTATÍSTICAS E TREINO CONCLUÍDO
-- ------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.somar_treino_realizado()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        INSERT INTO public.swimmer_stats
            (user_id, total_distance_meters, total_time_seconds, completed_workouts_count, updated_at)
        VALUES
            (NEW.user_id, NEW.metros_feitos, NEW.duracao_segundos, 1, timezone('utc'::text, now()))
        ON CONFLICT (user_id) DO UPDATE SET
            total_distance_meters = COALESCE(swimmer_stats.total_distance_meters, 0) + EXCLUDED.total_distance_meters,
            total_time_seconds = COALESCE(swimmer_stats.total_time_seconds, 0) + EXCLUDED.total_time_seconds,
            completed_workouts_count = COALESCE(swimmer_stats.completed_workouts_count, 0) + 1,
            updated_at = EXCLUDED.updated_at;

        -- Só marca o treino se ele for da mesma pessoa.
        IF NEW.workout_id IS NOT NULL THEN
            UPDATE public.workouts
               SET is_completed = TRUE
             WHERE id = NEW.workout_id
               AND user_id = NEW.user_id;
        END IF;
        RETURN NEW;
    END IF;

    -- DELETE. Se a conta inteira está sendo excluída, as estatísticas saem junto.
    IF NOT EXISTS (SELECT 1 FROM auth.users WHERE id = OLD.user_id) THEN
        RETURN OLD;
    END IF;

    UPDATE public.swimmer_stats SET
        total_distance_meters = GREATEST(0, COALESCE(total_distance_meters, 0) - OLD.metros_feitos),
        total_time_seconds = GREATEST(0, COALESCE(total_time_seconds, 0) - OLD.duracao_segundos),
        completed_workouts_count = GREATEST(0, COALESCE(completed_workouts_count, 0) - 1),
        updated_at = timezone('utc'::text, now())
    WHERE user_id = OLD.user_id;
    RETURN OLD;
END;
$$;

DROP TRIGGER IF EXISTS tr_treinos_realizados_estatisticas ON public.treinos_realizados;
CREATE TRIGGER tr_treinos_realizados_estatisticas
    AFTER INSERT OR DELETE ON public.treinos_realizados
    FOR EACH ROW EXECUTE FUNCTION public.somar_treino_realizado();

REVOKE ALL ON FUNCTION public.somar_treino_realizado() FROM PUBLIC, anon, authenticated;

-- ------------------------------------------------------------------------------
-- CONFERÊNCIA: a chave pública não lê nem grava.
-- ------------------------------------------------------------------------------
DO $conferencia$
DECLARE
    n INT;
BEGIN
    SET LOCAL ROLE anon;

    SELECT count(*) INTO n FROM public.treinos_realizados;
    IF n > 0 THEN
        RAISE EXCEPTION 'BRECHA: a chave pública lê treinos realizados. Nada foi alterado.';
    END IF;

    BEGIN
        INSERT INTO public.treinos_realizados
            (titulo, nivel, data_treino, metros_planejados, metros_feitos, series_planejadas, series_feitas, duracao_segundos)
        VALUES ('conferência', 'INICIANTE', CURRENT_DATE, 100, 100, 1, 1, 60);
        RAISE EXCEPTION 'BRECHA: a chave pública grava treinos realizados. Nada foi alterado.';
    EXCEPTION WHEN insufficient_privilege THEN
        NULL;
    END;

    RESET ROLE;
END
$conferencia$;

COMMIT;
