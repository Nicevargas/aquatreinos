-- ==============================================================================
-- AQUAGENDA - CONTAS: PERFIL NO CADASTRO, RLS DO QUE É PESSOAL E EXCLUIR CONTA
-- Arquivo: 20260913000003_contas_do_app.sql
--
-- O app passa a exigir login. Esta migração prepara o banco para isso.
--
-- 1. PERFIL NO CADASTRO. O gatilho da migração inicial já criava o perfil, mas
--    ignorava o nível escolhido no cadastro. Agora grava nome e nível
--    (INICIANTE / INTERMEDIARIO / AVANCADO) vindos do raw_user_meta_data.
--
-- 2. O E-MAIL DO PERFIL É SEMPRE O DA CONTA. O usuário edita nome, piscina e
--    nível; e-mail e id não.
--
-- 3. FECHA AS BRECHAS QUE O LOGIN OBRIGATÓRIO PERMITE FECHAR:
--    profiles         e-mails de todos eram públicos ("OR true")
--    swim_set_records a chave pública gravava e lia tempos sem dono
--    swimmer_stats    a chave pública criava, alterava e apagava estatísticas
--
-- 4. EXCLUIR A PRÓPRIA CONTA pelo app, via função. Perfil, treinos, séries e
--    estatísticas saem junto (ON DELETE CASCADE).
--
-- Transação única com conferência no fim: se algo não fechar, nada muda.
-- ==============================================================================

BEGIN;

-- ------------------------------------------------------------------------------
-- 1. PERFIL CRIADO NO CADASTRO
-- ------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    nivel TEXT := upper(COALESCE(NEW.raw_user_meta_data->>'training_level', ''));
BEGIN
    INSERT INTO public.profiles (id, email, full_name, avatar_url, training_level)
    VALUES (
        NEW.id,
        NEW.email,
        NULLIF(trim(NEW.raw_user_meta_data->>'full_name'), ''),
        NEW.raw_user_meta_data->>'avatar_url',
        CASE WHEN nivel IN ('INICIANTE', 'INTERMEDIARIO', 'AVANCADO') THEN nivel ELSE 'INTERMEDIARIO' END
    )
    ON CONFLICT (id) DO NOTHING;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- ------------------------------------------------------------------------------
-- 2. E-MAIL DO PERFIL = E-MAIL DA CONTA
-- ------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.perfil_email_da_conta()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    IF TG_OP = 'UPDATE' THEN
        NEW.id := OLD.id;
    END IF;
    NEW.email := (SELECT email FROM auth.users WHERE id = NEW.id);
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS tr_profiles_email_da_conta ON public.profiles;
CREATE TRIGGER tr_profiles_email_da_conta
    BEFORE INSERT OR UPDATE ON public.profiles
    FOR EACH ROW EXECUTE FUNCTION public.perfil_email_da_conta();

-- ------------------------------------------------------------------------------
-- 3. RLS: CADA UM SÓ COM O QUE É SEU
-- Remove TODAS as políticas das três tabelas: permissivas se somam (OR), e uma
-- esquecida manteria a brecha.
-- ------------------------------------------------------------------------------
DO $limpar$
DECLARE
    p RECORD;
BEGIN
    FOR p IN
        SELECT tablename, policyname FROM pg_policies
        WHERE schemaname = 'public'
          AND tablename IN ('profiles', 'swim_set_records', 'swimmer_stats')
    LOOP
        EXECUTE format('DROP POLICY %I ON public.%I', p.policyname, p.tablename);
    END LOOP;
END
$limpar$;

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.swim_set_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.swimmer_stats ENABLE ROW LEVEL SECURITY;

-- profiles: sem DELETE; a conta sai inteira pela função do item 4.
-- INSERT existe para a conta que ficou sem perfil (criada antes do gatilho).
CREATE POLICY "Ver o próprio perfil"
    ON public.profiles FOR SELECT TO authenticated
    USING (id = auth.uid());
CREATE POLICY "Criar o próprio perfil"
    ON public.profiles FOR INSERT TO authenticated
    WITH CHECK (id = auth.uid());
CREATE POLICY "Editar o próprio perfil"
    ON public.profiles FOR UPDATE TO authenticated
    USING (id = auth.uid())
    WITH CHECK (id = auth.uid());

ALTER TABLE public.swim_set_records ALTER COLUMN user_id SET DEFAULT auth.uid();
CREATE POLICY "Ver as próprias séries"
    ON public.swim_set_records FOR SELECT TO authenticated
    USING (user_id = auth.uid());
CREATE POLICY "Gravar só séries próprias"
    ON public.swim_set_records FOR INSERT TO authenticated
    WITH CHECK (user_id = auth.uid());
CREATE POLICY "Apagar as próprias séries"
    ON public.swim_set_records FOR DELETE TO authenticated
    USING (user_id = auth.uid());

ALTER TABLE public.swimmer_stats ALTER COLUMN user_id SET DEFAULT auth.uid();
CREATE POLICY "Ver as próprias estatísticas"
    ON public.swimmer_stats FOR SELECT TO authenticated
    USING (user_id = auth.uid());
CREATE POLICY "Criar as próprias estatísticas"
    ON public.swimmer_stats FOR INSERT TO authenticated
    WITH CHECK (user_id = auth.uid());
CREATE POLICY "Atualizar as próprias estatísticas"
    ON public.swimmer_stats FOR UPDATE TO authenticated
    USING (user_id = auth.uid())
    WITH CHECK (user_id = auth.uid());

-- ------------------------------------------------------------------------------
-- 4. EXCLUIR A PRÓPRIA CONTA
-- Chamada pelo app: POST /rest/v1/rpc/excluir_minha_conta
-- ------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.excluir_minha_conta()
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    uid UUID := auth.uid();
BEGIN
    IF uid IS NULL THEN
        RAISE EXCEPTION 'É preciso estar logado para excluir a conta.'
            USING ERRCODE = '42501';
    END IF;
    DELETE FROM auth.users WHERE id = uid;
END;
$$;

REVOKE ALL ON FUNCTION public.excluir_minha_conta() FROM PUBLIC;
REVOKE ALL ON FUNCTION public.excluir_minha_conta() FROM anon;
GRANT EXECUTE ON FUNCTION public.excluir_minha_conta() TO authenticated;

-- Funções de gatilho não são para chamar pela API.
REVOKE ALL ON FUNCTION public.handle_new_user() FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.perfil_email_da_conta() FROM PUBLIC, anon, authenticated;

-- ------------------------------------------------------------------------------
-- 5. CONFERÊNCIA: tenta, com a chave pública, o que as brechas permitiam.
-- Qualquer tentativa que passar desfaz a transação inteira.
-- ------------------------------------------------------------------------------
DO $conferencia$
DECLARE
    n INT;
BEGIN
    SET LOCAL ROLE anon;

    SELECT count(*) INTO n FROM public.profiles;
    IF n > 0 THEN
        RAISE EXCEPTION 'BRECHA: a chave pública ainda lê % perfil(is). Nada foi alterado.', n;
    END IF;

    SELECT count(*) INTO n FROM public.swim_set_records;
    IF n > 0 THEN
        RAISE EXCEPTION 'BRECHA: a chave pública ainda lê % série(s). Nada foi alterado.', n;
    END IF;

    BEGIN
        INSERT INTO public.swim_set_records (set_number, time_formatted, time_millis)
        VALUES (1, '00:00.0', 0);
        RAISE EXCEPTION 'BRECHA: a chave pública ainda grava séries. Nada foi alterado.';
    EXCEPTION WHEN insufficient_privilege THEN
        NULL;
    END;

    UPDATE public.swimmer_stats SET total_distance_meters = total_distance_meters;
    GET DIAGNOSTICS n = ROW_COUNT;
    IF n > 0 THEN
        RAISE EXCEPTION 'BRECHA: a chave pública ainda altera estatísticas. Nada foi alterado.';
    END IF;

    BEGIN
        PERFORM public.excluir_minha_conta();
        RAISE EXCEPTION 'BRECHA: a chave pública executa a exclusão de conta. Nada foi alterado.';
    EXCEPTION WHEN insufficient_privilege THEN
        NULL;
    END;

    RESET ROLE;
END
$conferencia$;

COMMIT;
