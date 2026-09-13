-- ==============================================================================
-- AQUAGENDA - SEGURANÇA DA TABELA WORKOUTS
-- Arquivo: 20260913000002_workouts_rls_seguranca.sql
--
-- A migração inicial deixava a chave pública (anon), que vai dentro do app e
-- qualquer um pode extrair do APK, CRIAR, ALTERAR e sobrescrever os treinos sem
-- dono (user_id NULL) -- justamente os treinos públicos que todo mundo vê.
-- Também deixava um usuário logado "soltar" um treino dele (UPDATE para
-- user_id NULL), transformando-o em treino público para todos.
--
-- Regra nova:
--   ler      os próprios treinos e os públicos (sem dono)      -- igual antes
--   criar    só usuário logado, e só com o próprio user_id
--   alterar  só os próprios, sem trocar o dono
--   excluir  só os próprios                                     -- igual antes
-- Treino público passa a ser mantido só pelo SQL Editor (ou service_role).
--
-- O app não é afetado: ele só LÊ esta tabela.
-- Tudo roda numa transação e se confere no fim; se algo não fechar, nada muda.
-- ==============================================================================

BEGIN;

-- 1. Remove TODAS as políticas de workouts, não só as de nome conhecido.
-- Políticas permissivas se somam (OR): uma esquecida manteria a brecha aberta.
DO $limpar$
DECLARE
    p RECORD;
BEGIN
    FOR p IN
        SELECT policyname FROM pg_policies
        WHERE schemaname = 'public' AND tablename = 'workouts'
    LOOP
        EXECUTE format('DROP POLICY %I ON public.workouts', p.policyname);
    END LOOP;
END
$limpar$;

ALTER TABLE public.workouts ENABLE ROW LEVEL SECURITY;

-- Quem cria logado não precisa mandar o user_id: ele vem da sessão.
ALTER TABLE public.workouts ALTER COLUMN user_id SET DEFAULT auth.uid();

-- 2. Políticas novas
CREATE POLICY "Ler treinos próprios e públicos"
    ON public.workouts FOR SELECT
    USING (user_id IS NULL OR user_id = auth.uid());

CREATE POLICY "Criar só treinos próprios"
    ON public.workouts FOR INSERT
    TO authenticated
    WITH CHECK (auth.uid() IS NOT NULL AND user_id = auth.uid());

-- USING escolhe as linhas que pode mexer; WITH CHECK impede trocar o dono.
CREATE POLICY "Alterar só treinos próprios, sem trocar o dono"
    ON public.workouts FOR UPDATE
    TO authenticated
    USING (user_id = auth.uid())
    WITH CHECK (user_id = auth.uid());

CREATE POLICY "Excluir só treinos próprios"
    ON public.workouts FOR DELETE
    TO authenticated
    USING (user_id = auth.uid());

-- 3. Conferência: tenta, com a chave pública, tudo o que a brecha permitia.
-- Qualquer tentativa que passar aborta a transação inteira.
DO $conferencia$
DECLARE
    n INT;
BEGIN
    SET LOCAL ROLE anon;

    BEGIN
        INSERT INTO public.workouts (title) VALUES ('conferência de segurança');
        RAISE EXCEPTION 'BRECHA: a chave pública ainda cria treino. Nada foi alterado.';
    EXCEPTION WHEN insufficient_privilege THEN
        NULL; -- recusado pelo RLS, como deve ser
    END;

    UPDATE public.workouts SET title = title WHERE user_id IS NULL;
    GET DIAGNOSTICS n = ROW_COUNT;
    IF n > 0 THEN
        RAISE EXCEPTION 'BRECHA: a chave pública ainda altera % treino(s). Nada foi alterado.', n;
    END IF;

    DELETE FROM public.workouts WHERE user_id IS NULL;
    GET DIAGNOSTICS n = ROW_COUNT;
    IF n > 0 THEN
        RAISE EXCEPTION 'BRECHA: a chave pública ainda apaga % treino(s). Nada foi alterado.', n;
    END IF;

    RESET ROLE;
END
$conferencia$;

COMMIT;
