-- ==============================================================================
-- APP NATAÇÃO CRIATIVA - PAR-Q (QUESTIONÁRIO DE PRONTIDÃO PARA ATIVIDADE FÍSICA)
-- Arquivo: 20260915000001_parq.sql
--
-- Antes de treinar, a pessoa responde as 7 perguntas do PAR-Q. Se responder SIM
-- a alguma, só treina aceitando o termo de responsabilidade. Isso é a prova de
-- que ela se declarou apta, por isso:
--   - cada resposta vira uma linha nova, que ninguém edita nem apaga pela API;
--   - a data e a hora vêm do servidor (gatilho), não do celular;
--   - SIM sem termo aceito é recusado pelo banco.
--
-- Quanto tempo o PAR-Q vale (hoje 12 meses) é regra do app: a tabela só guarda
-- quando foi respondido.
--
-- RLS: cada um vê e registra só os próprios. Transação única com conferência.
-- ==============================================================================

BEGIN;

CREATE TABLE IF NOT EXISTS public.parq_respostas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL DEFAULT auth.uid() REFERENCES auth.users(id) ON DELETE CASCADE,
    -- Versão do texto das perguntas e do termo que a pessoa viu.
    versao TEXT NOT NULL CHECK (char_length(versao) BETWEEN 1 AND 50),
    -- Uma resposta por pergunta, na ordem: true = SIM, false = NÃO.
    respostas JSONB NOT NULL CHECK (
        CASE WHEN jsonb_typeof(respostas) = 'array'
             THEN jsonb_array_length(respostas) = 7 AND respostas <@ '[true, false]'::jsonb
             ELSE FALSE
        END
    ),
    algum_sim BOOLEAN NOT NULL,
    declaracao_aceita BOOLEAN NOT NULL CHECK (declaracao_aceita),
    termo_aceito BOOLEAN NOT NULL DEFAULT FALSE,
    respondido_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    respondido_no_dia DATE NOT NULL DEFAULT (now() AT TIME ZONE 'America/Sao_Paulo')::date,
    CONSTRAINT parq_algum_sim_confere CHECK (algum_sim = (respostas @> '[true]'::jsonb)),
    CONSTRAINT parq_termo_quando_sim CHECK (NOT algum_sim OR termo_aceito)
);

CREATE INDEX IF NOT EXISTS idx_parq_respostas_usuario
    ON public.parq_respostas(user_id, respondido_em DESC);

-- Data e hora sempre do servidor: o celular não consegue antedatar nem adiantar.
CREATE OR REPLACE FUNCTION public.parq_carimbar_data()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = public
AS $$
BEGIN
    NEW.respondido_em := now();
    NEW.respondido_no_dia := (now() AT TIME ZONE 'America/Sao_Paulo')::date;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS tr_parq_carimbar_data ON public.parq_respostas;
CREATE TRIGGER tr_parq_carimbar_data
    BEFORE INSERT ON public.parq_respostas
    FOR EACH ROW EXECUTE FUNCTION public.parq_carimbar_data();

ALTER TABLE public.parq_respostas ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Ver o próprio PAR-Q" ON public.parq_respostas;
CREATE POLICY "Ver o próprio PAR-Q"
    ON public.parq_respostas FOR SELECT TO authenticated
    USING (user_id = auth.uid());

DROP POLICY IF EXISTS "Registrar só o próprio PAR-Q" ON public.parq_respostas;
CREATE POLICY "Registrar só o próprio PAR-Q"
    ON public.parq_respostas FOR INSERT TO authenticated
    WITH CHECK (user_id = auth.uid());

-- Sem UPDATE e sem DELETE: resposta registrada é o que foi declarado.
GRANT SELECT, INSERT ON public.parq_respostas TO authenticated;

-- ------------------------------------------------------------------------------
-- CONFERÊNCIA: a chave pública não lê nem grava.
-- ------------------------------------------------------------------------------
DO $conferencia$
DECLARE
    n INT;
BEGIN
    SET LOCAL ROLE anon;

    SELECT count(*) INTO n FROM public.parq_respostas;
    IF n > 0 THEN
        RAISE EXCEPTION 'BRECHA: a chave pública lê respostas do PAR-Q. Nada foi alterado.';
    END IF;

    BEGIN
        INSERT INTO public.parq_respostas (versao, respostas, algum_sim, declaracao_aceita)
        VALUES ('conferencia', '[false,false,false,false,false,false,false]', FALSE, TRUE);
        RAISE EXCEPTION 'BRECHA: a chave pública grava respostas do PAR-Q. Nada foi alterado.';
    EXCEPTION WHEN insufficient_privilege THEN
        NULL;
    END;

    RESET ROLE;
END
$conferencia$;

COMMIT;
