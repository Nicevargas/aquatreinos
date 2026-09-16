-- ==============================================================================
-- APP NATAÇÃO CRIATIVA - PLANO DE TREINO MONTADO PELO NADADOR
-- Arquivo: 20260915000005_plano_personalizado.sql
--
-- O nadador pode trocar qualquer treino do plano: por outra capacidade do Método
-- NC ("foco:Velocidade") ou por um treino dele de Meus treinos ("meu:<id>").
-- As trocas ficam num objeto { "semana-treino": "escolha" }.
--
-- modo: 'automatico' (o app monta) ou 'manual' (o nadador escolhe cada treino).
--
-- UPDATE só na coluna trocas: semanas, nível e o resto do plano não mudam
-- depois de criado (para mudar, cria outro plano). Transação única.
-- ==============================================================================

BEGIN;

ALTER TABLE public.planos_treino
    ADD COLUMN IF NOT EXISTS modo TEXT NOT NULL DEFAULT 'automatico',
    ADD COLUMN IF NOT EXISTS trocas JSONB NOT NULL DEFAULT '{}'::jsonb;

ALTER TABLE public.planos_treino DROP CONSTRAINT IF EXISTS planos_treino_modo_valido;
ALTER TABLE public.planos_treino ADD CONSTRAINT planos_treino_modo_valido
    CHECK (modo IN ('automatico', 'manual'));

ALTER TABLE public.planos_treino DROP CONSTRAINT IF EXISTS planos_treino_trocas_validas;
ALTER TABLE public.planos_treino ADD CONSTRAINT planos_treino_trocas_validas
    CHECK (jsonb_typeof(trocas) = 'object' AND pg_column_size(trocas) <= 16000);

DROP POLICY IF EXISTS "Trocar treinos dos próprios planos" ON public.planos_treino;
CREATE POLICY "Trocar treinos dos próprios planos"
    ON public.planos_treino FOR UPDATE TO authenticated
    USING (user_id = auth.uid())
    WITH CHECK (user_id = auth.uid());

REVOKE UPDATE ON public.planos_treino FROM anon, authenticated;
GRANT UPDATE (trocas) ON public.planos_treino TO authenticated;

-- ------------------------------------------------------------------------------
-- CONFERÊNCIA: a chave pública não altera planos.
-- ------------------------------------------------------------------------------
DO $conferencia$
DECLARE
    n INT;
BEGIN
    SET LOCAL ROLE anon;
    BEGIN
        UPDATE public.planos_treino SET trocas = '{}'::jsonb;
        GET DIAGNOSTICS n = ROW_COUNT;
        IF n > 0 THEN
            RAISE EXCEPTION 'BRECHA: a chave pública altera planos. Nada foi alterado.';
        END IF;
    EXCEPTION WHEN insufficient_privilege THEN
        NULL;
    END;
    RESET ROLE;
END
$conferencia$;

COMMIT;
