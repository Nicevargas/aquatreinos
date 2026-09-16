-- ==============================================================================
-- APP NATAÇÃO CRIATIVA - TREINO COMPARTILHADO POR LINK
-- Arquivo: 20260916000001_treinos_compartilhados.sql
--
-- A pessoa compartilha um treino (sugestão, plano ou dela): o app guarda uma
-- cópia aqui e o banco gera um código curto. O link com o código vai pelo
-- WhatsApp ou pelas redes; quem abre no app vê o treino e, ao concluir, ele
-- entra em Meus treinos dessa pessoa.
--
-- RLS: cada um vê, cria e apaga só os próprios. Abrir o treino de outra pessoa
-- é só pela função abrir_treino_compartilhado(código), para quem está logado,
-- e mostra só o treino e o primeiro nome de quem mandou.
-- Transação única com conferência.
-- ==============================================================================

BEGIN;

CREATE TABLE IF NOT EXISTS public.treinos_compartilhados (
    codigo TEXT PRIMARY KEY CHECK (codigo ~ '^[a-f0-9]{10}$'),
    user_id UUID NOT NULL DEFAULT auth.uid() REFERENCES auth.users(id) ON DELETE CASCADE,
    titulo TEXT NOT NULL CHECK (char_length(trim(titulo)) BETWEEN 1 AND 200),
    treino JSONB NOT NULL CHECK (
        jsonb_typeof(treino) = 'object'
        -- COALESCE: sem "phases", jsonb_typeof dá NULL, e CHECK com NULL passaria.
        AND COALESCE(jsonb_typeof(treino -> 'phases'), '') = 'array'
        AND pg_column_size(treino) <= 100000
    ),
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_treinos_compartilhados_usuario
    ON public.treinos_compartilhados(user_id, criado_em DESC);

-- O código e a data são sempre do banco: ninguém escolhe o próprio código.
CREATE OR REPLACE FUNCTION public.treino_compartilhado_codigo()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = public
AS $$
BEGIN
    NEW.codigo := substr(md5(gen_random_uuid()::text || clock_timestamp()::text), 1, 10);
    NEW.criado_em := now();
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS tr_treino_compartilhado_codigo ON public.treinos_compartilhados;
CREATE TRIGGER tr_treino_compartilhado_codigo
    BEFORE INSERT ON public.treinos_compartilhados
    FOR EACH ROW EXECUTE FUNCTION public.treino_compartilhado_codigo();

REVOKE ALL ON FUNCTION public.treino_compartilhado_codigo() FROM PUBLIC, anon, authenticated;

ALTER TABLE public.treinos_compartilhados ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Ver os próprios treinos compartilhados" ON public.treinos_compartilhados;
CREATE POLICY "Ver os próprios treinos compartilhados"
    ON public.treinos_compartilhados FOR SELECT TO authenticated
    USING (user_id = auth.uid());

DROP POLICY IF EXISTS "Compartilhar só treinos próprios" ON public.treinos_compartilhados;
CREATE POLICY "Compartilhar só treinos próprios"
    ON public.treinos_compartilhados FOR INSERT TO authenticated
    WITH CHECK (user_id = auth.uid());

DROP POLICY IF EXISTS "Apagar os próprios treinos compartilhados" ON public.treinos_compartilhados;
CREATE POLICY "Apagar os próprios treinos compartilhados"
    ON public.treinos_compartilhados FOR DELETE TO authenticated
    USING (user_id = auth.uid());

REVOKE ALL ON public.treinos_compartilhados FROM anon;
GRANT SELECT, INSERT, DELETE ON public.treinos_compartilhados TO authenticated;

-- ------------------------------------------------------------------------------
-- ABRIR PELO CÓDIGO
-- ------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.abrir_treino_compartilhado(p_codigo TEXT)
RETURNS TABLE (codigo TEXT, titulo TEXT, treino JSONB, enviado_por TEXT)
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT
        t.codigo,
        t.titulo,
        t.treino,
        NULLIF(split_part(trim(COALESCE(p.full_name, '')), ' ', 1), '') AS enviado_por
    FROM public.treinos_compartilhados t
    LEFT JOIN public.profiles p ON p.id = t.user_id
    WHERE auth.uid() IS NOT NULL
      AND t.codigo = lower(trim(p_codigo));
$$;

REVOKE ALL ON FUNCTION public.abrir_treino_compartilhado(TEXT) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.abrir_treino_compartilhado(TEXT) TO authenticated;

-- ------------------------------------------------------------------------------
-- CONFERÊNCIA: a chave pública não lê, não cria e não abre treinos.
-- ------------------------------------------------------------------------------
DO $conferencia$
DECLARE
    n INT;
BEGIN
    SET LOCAL ROLE anon;

    BEGIN
        SELECT count(*) INTO n FROM public.treinos_compartilhados;
        IF n > 0 THEN
            RAISE EXCEPTION 'BRECHA: a chave pública lê treinos compartilhados. Nada foi alterado.';
        END IF;
    EXCEPTION WHEN insufficient_privilege THEN
        NULL;
    END;

    BEGIN
        INSERT INTO public.treinos_compartilhados (titulo, treino) VALUES ('conferência', '{"phases": []}');
        RAISE EXCEPTION 'BRECHA: a chave pública cria treinos compartilhados. Nada foi alterado.';
    EXCEPTION WHEN insufficient_privilege THEN
        NULL;
    END;

    BEGIN
        PERFORM * FROM public.abrir_treino_compartilhado('0123456789');
        RAISE EXCEPTION 'BRECHA: a chave pública abre treinos compartilhados. Nada foi alterado.';
    EXCEPTION WHEN insufficient_privilege THEN
        NULL;
    END;

    RESET ROLE;
END
$conferencia$;

COMMIT;
