-- ==============================================================================
-- APP NATAÇÃO CRIATIVA - PLANO DE TREINO
-- Arquivo: 20260915000003_planos_de_treino.sql
--
-- A pessoa monta um plano de 1 a 12 semanas, de 1 a 6 treinos por semana, com o
-- que quer melhorar. O app remonta os treinos do Método NC a partir desta
-- configuração; aqui fica só o que ela escolheu.
--
-- Cada treino do plano concluído é um treino_realizado com plano_id, semana e
-- número do treino: é daí que sai o progresso.
--
-- RLS: cada um vê, cria e apaga só os próprios planos, e só liga um treino
-- realizado a um plano seu. Transação única com conferência.
-- ==============================================================================

BEGIN;

CREATE TABLE IF NOT EXISTS public.planos_treino (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL DEFAULT auth.uid() REFERENCES auth.users(id) ON DELETE CASCADE,
    semanas SMALLINT NOT NULL CHECK (semanas BETWEEN 1 AND 12),
    treinos_por_semana SMALLINT NOT NULL CHECK (treinos_por_semana BETWEEN 1 AND 6),
    -- Vazio = todas as capacidades.
    focos TEXT[] NOT NULL DEFAULT '{}' CHECK (
        focos <@ ARRAY['Técnica', 'Resistência', 'Velocidade', 'Estilos', 'Ritmo', 'Força específica']::TEXT[]
    ),
    semanas_relaxadas BOOLEAN NOT NULL DEFAULT TRUE,
    encerramento_relaxado BOOLEAN NOT NULL DEFAULT TRUE,
    nivel TEXT NOT NULL CHECK (nivel IN ('INICIANTE', 'INTERMEDIARIO', 'AVANCADO')),
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_planos_treino_usuario
    ON public.planos_treino(user_id, criado_em DESC);

ALTER TABLE public.planos_treino ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Ver os próprios planos" ON public.planos_treino;
CREATE POLICY "Ver os próprios planos"
    ON public.planos_treino FOR SELECT TO authenticated
    USING (user_id = auth.uid());

DROP POLICY IF EXISTS "Criar só planos próprios" ON public.planos_treino;
CREATE POLICY "Criar só planos próprios"
    ON public.planos_treino FOR INSERT TO authenticated
    WITH CHECK (user_id = auth.uid());

DROP POLICY IF EXISTS "Apagar os próprios planos" ON public.planos_treino;
CREATE POLICY "Apagar os próprios planos"
    ON public.planos_treino FOR DELETE TO authenticated
    USING (user_id = auth.uid());

GRANT SELECT, INSERT, DELETE ON public.planos_treino TO authenticated;

-- ------------------------------------------------------------------------------
-- TREINO REALIZADO DENTRO DO PLANO
-- Apagar o plano não apaga os treinos feitos: eles só deixam de apontar para ele.
-- ------------------------------------------------------------------------------
ALTER TABLE public.treinos_realizados
    ADD COLUMN IF NOT EXISTS plano_id UUID REFERENCES public.planos_treino(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS plano_semana SMALLINT CHECK (plano_semana BETWEEN 1 AND 12),
    ADD COLUMN IF NOT EXISTS plano_treino SMALLINT CHECK (plano_treino BETWEEN 1 AND 6);

ALTER TABLE public.treinos_realizados
    DROP CONSTRAINT IF EXISTS treinos_realizados_plano_completo;
ALTER TABLE public.treinos_realizados
    ADD CONSTRAINT treinos_realizados_plano_completo
    CHECK (plano_id IS NULL OR (plano_semana IS NOT NULL AND plano_treino IS NOT NULL));

CREATE INDEX IF NOT EXISTS idx_treinos_realizados_plano
    ON public.treinos_realizados(plano_id) WHERE plano_id IS NOT NULL;

-- Registrar continua só para si; e o plano, se houver, tem de ser seu.
DROP POLICY IF EXISTS "Registrar só treinos próprios" ON public.treinos_realizados;
CREATE POLICY "Registrar só treinos próprios"
    ON public.treinos_realizados FOR INSERT TO authenticated
    WITH CHECK (
        user_id = auth.uid()
        AND (
            plano_id IS NULL
            OR EXISTS (SELECT 1 FROM public.planos_treino p WHERE p.id = plano_id AND p.user_id = auth.uid())
        )
    );

-- ------------------------------------------------------------------------------
-- CONFERÊNCIA: a chave pública não lê nem cria planos.
-- ------------------------------------------------------------------------------
DO $conferencia$
DECLARE
    n INT;
BEGIN
    SET LOCAL ROLE anon;

    SELECT count(*) INTO n FROM public.planos_treino;
    IF n > 0 THEN
        RAISE EXCEPTION 'BRECHA: a chave pública lê planos de treino. Nada foi alterado.';
    END IF;

    BEGIN
        INSERT INTO public.planos_treino (semanas, treinos_por_semana, nivel) VALUES (4, 3, 'INICIANTE');
        RAISE EXCEPTION 'BRECHA: a chave pública cria planos de treino. Nada foi alterado.';
    EXCEPTION WHEN insufficient_privilege THEN
        NULL;
    END;

    RESET ROLE;
END
$conferencia$;

COMMIT;
