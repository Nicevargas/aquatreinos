-- ==============================================================================
-- APP NATAÇÃO CRIATIVA - RANKING DOS NADADORES (SÓ QUEM ACEITAR)
-- Arquivo: 20260915000004_ranking.sql
--
-- A pessoa escolhe entrar no ranking e aparece com o nome que ela definir
-- (o app sugere primeiro nome e inicial). E-mail, data de nascimento, notas de
-- esforço e respostas do PAR-Q nunca saem daqui.
--
-- Filtros que ela mesma informa no perfil: ano de nascimento (vira faixa de
-- idade), sexo, cidade e local onde nada. O horário vem da hora em que cada
-- treino foi salvo (manhã, tarde, noite).
--
-- Pontos (os mesmos do app, em Pontuacao.kt):
--   1 ponto a cada 100 m nadados
--   +10 por treino completo
--   +20 por semana (segunda a domingo) com pelo menos um treino
--
-- A leitura é só pela função ranking_nadadores(): a tabela de perfis continua
-- fechada (cada um vê só o próprio). Transação única com conferência.
-- ==============================================================================

BEGIN;

-- ------------------------------------------------------------------------------
-- 1. DADOS DO RANKING NO PERFIL
-- ------------------------------------------------------------------------------
ALTER TABLE public.profiles
    ADD COLUMN IF NOT EXISTS ranking_publico BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS ranking_nome TEXT,
    ADD COLUMN IF NOT EXISTS ranking_aceito_em TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS ano_nascimento SMALLINT,
    ADD COLUMN IF NOT EXISTS sexo TEXT,
    ADD COLUMN IF NOT EXISTS cidade TEXT,
    ADD COLUMN IF NOT EXISTS local_treino TEXT;

ALTER TABLE public.profiles DROP CONSTRAINT IF EXISTS profiles_ranking_nome_tamanho;
ALTER TABLE public.profiles ADD CONSTRAINT profiles_ranking_nome_tamanho
    CHECK (ranking_nome IS NULL OR char_length(trim(ranking_nome)) BETWEEN 2 AND 40);

ALTER TABLE public.profiles DROP CONSTRAINT IF EXISTS profiles_ranking_com_nome;
ALTER TABLE public.profiles ADD CONSTRAINT profiles_ranking_com_nome
    CHECK (NOT ranking_publico OR ranking_nome IS NOT NULL);

ALTER TABLE public.profiles DROP CONSTRAINT IF EXISTS profiles_ano_nascimento_valido;
ALTER TABLE public.profiles ADD CONSTRAINT profiles_ano_nascimento_valido
    CHECK (ano_nascimento IS NULL OR ano_nascimento BETWEEN 1900 AND 2100);

ALTER TABLE public.profiles DROP CONSTRAINT IF EXISTS profiles_sexo_valido;
ALTER TABLE public.profiles ADD CONSTRAINT profiles_sexo_valido
    CHECK (sexo IS NULL OR sexo IN ('F', 'M', 'OUTRO'));

ALTER TABLE public.profiles DROP CONSTRAINT IF EXISTS profiles_cidade_tamanho;
ALTER TABLE public.profiles ADD CONSTRAINT profiles_cidade_tamanho
    CHECK (cidade IS NULL OR char_length(cidade) <= 80);

ALTER TABLE public.profiles DROP CONSTRAINT IF EXISTS profiles_local_treino_tamanho;
ALTER TABLE public.profiles ADD CONSTRAINT profiles_local_treino_tamanho
    CHECK (local_treino IS NULL OR char_length(local_treino) <= 80);

-- Quando a pessoa aceita, o banco guarda a hora do aceite; ao sair, apaga.
CREATE OR REPLACE FUNCTION public.perfil_aceite_do_ranking()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = public
AS $$
BEGIN
    IF NEW.ranking_publico THEN
        IF TG_OP = 'INSERT' OR NOT COALESCE(OLD.ranking_publico, FALSE) THEN
            NEW.ranking_aceito_em := now();
        ELSE
            NEW.ranking_aceito_em := OLD.ranking_aceito_em;
        END IF;
    ELSE
        NEW.ranking_aceito_em := NULL;
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS tr_profiles_aceite_do_ranking ON public.profiles;
CREATE TRIGGER tr_profiles_aceite_do_ranking
    BEFORE INSERT OR UPDATE ON public.profiles
    FOR EACH ROW EXECUTE FUNCTION public.perfil_aceite_do_ranking();

REVOKE ALL ON FUNCTION public.perfil_aceite_do_ranking() FROM PUBLIC, anon, authenticated;

-- ------------------------------------------------------------------------------
-- 2. O RANKING
-- p_periodo: semana | mes | ano | tudo
-- p_faixa:   ate-17 | 18-29 | 30-39 | 40-49 | 50-59 | 60+
-- p_sexo:    F | M | OUTRO
-- p_horario: manha (5h-11h) | tarde (12h-17h) | noite (18h-4h)
-- p_cidade, p_local: comparação sem diferença de maiúsculas e espaços
-- ------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.ranking_nadadores(
    p_periodo TEXT DEFAULT 'mes',
    p_faixa TEXT DEFAULT NULL,
    p_sexo TEXT DEFAULT NULL,
    p_horario TEXT DEFAULT NULL,
    p_cidade TEXT DEFAULT NULL,
    p_local TEXT DEFAULT NULL,
    p_limite INT DEFAULT 100
)
RETURNS TABLE (posicao INT, nome TEXT, pontos INT, metros INT, treinos INT, semanas INT, sou_eu BOOLEAN)
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    WITH base AS (
        SELECT
            (now() AT TIME ZONE 'America/Sao_Paulo')::date AS hoje,
            extract(year FROM (now() AT TIME ZONE 'America/Sao_Paulo'))::int AS ano_atual
    ),
    janela AS (
        SELECT
            hoje,
            ano_atual,
            CASE p_periodo
                WHEN 'semana' THEN date_trunc('week', hoje)::date
                WHEN 'mes' THEN date_trunc('month', hoje)::date
                WHEN 'ano' THEN date_trunc('year', hoje)::date
                ELSE DATE '1900-01-01'
            END AS inicio
        FROM base
    ),
    participantes AS (
        SELECT p.id, trim(p.ranking_nome) AS nome
        FROM public.profiles p, janela j
        WHERE auth.uid() IS NOT NULL
          AND p.ranking_publico
          AND p.ranking_nome IS NOT NULL
          AND (p_sexo IS NULL OR p.sexo = p_sexo)
          AND (p_cidade IS NULL OR lower(trim(p.cidade)) = lower(trim(p_cidade)))
          AND (p_local IS NULL OR lower(trim(p.local_treino)) = lower(trim(p_local)))
          AND (
              p_faixa IS NULL
              OR (p.ano_nascimento IS NOT NULL AND CASE p_faixa
                  WHEN 'ate-17' THEN j.ano_atual - p.ano_nascimento <= 17
                  WHEN '18-29' THEN j.ano_atual - p.ano_nascimento BETWEEN 18 AND 29
                  WHEN '30-39' THEN j.ano_atual - p.ano_nascimento BETWEEN 30 AND 39
                  WHEN '40-49' THEN j.ano_atual - p.ano_nascimento BETWEEN 40 AND 49
                  WHEN '50-59' THEN j.ano_atual - p.ano_nascimento BETWEEN 50 AND 59
                  WHEN '60+' THEN j.ano_atual - p.ano_nascimento >= 60
                  ELSE FALSE
              END)
          )
    ),
    feitos AS (
        SELECT t.user_id, t.metros_feitos, t.metros_planejados, t.data_treino
        FROM public.treinos_realizados t
        JOIN participantes pa ON pa.id = t.user_id
        CROSS JOIN janela j
        WHERE t.data_treino BETWEEN j.inicio AND j.hoje
          AND (
              p_horario IS NULL
              OR CASE p_horario
                  WHEN 'manha' THEN extract(hour FROM t.created_at AT TIME ZONE 'America/Sao_Paulo') BETWEEN 5 AND 11
                  WHEN 'tarde' THEN extract(hour FROM t.created_at AT TIME ZONE 'America/Sao_Paulo') BETWEEN 12 AND 17
                  WHEN 'noite' THEN extract(hour FROM t.created_at AT TIME ZONE 'America/Sao_Paulo') NOT BETWEEN 5 AND 17
                  ELSE FALSE
              END
          )
    ),
    somas AS (
        SELECT
            user_id,
            (sum(metros_feitos / 100
                 + CASE WHEN metros_planejados > 0 AND metros_feitos >= metros_planejados THEN 10 ELSE 0 END)
             + 20 * count(DISTINCT date_trunc('week', data_treino)))::int AS pontos,
            sum(metros_feitos)::int AS metros,
            count(*)::int AS treinos,
            count(DISTINCT date_trunc('week', data_treino))::int AS semanas
        FROM feitos
        GROUP BY user_id
    )
    SELECT
        (rank() OVER (ORDER BY s.pontos DESC, s.metros DESC))::int AS posicao,
        pa.nome,
        s.pontos,
        s.metros,
        s.treinos,
        s.semanas,
        pa.id = auth.uid() AS sou_eu
    FROM somas s
    JOIN participantes pa ON pa.id = s.user_id
    ORDER BY posicao, pa.nome
    LIMIT least(greatest(COALESCE(p_limite, 100), 1), 200);
$$;

REVOKE ALL ON FUNCTION public.ranking_nadadores(TEXT, TEXT, TEXT, TEXT, TEXT, TEXT, INT) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.ranking_nadadores(TEXT, TEXT, TEXT, TEXT, TEXT, TEXT, INT) TO authenticated;

-- ------------------------------------------------------------------------------
-- 3. CONFERÊNCIA: a chave pública não vê o ranking nem perfis.
-- ------------------------------------------------------------------------------
DO $conferencia$
DECLARE
    n INT;
BEGIN
    SET LOCAL ROLE anon;

    SELECT count(*) INTO n FROM public.profiles;
    IF n > 0 THEN
        RAISE EXCEPTION 'BRECHA: a chave pública lê perfis. Nada foi alterado.';
    END IF;

    BEGIN
        PERFORM * FROM public.ranking_nadadores();
        RAISE EXCEPTION 'BRECHA: a chave pública vê o ranking. Nada foi alterado.';
    EXCEPTION WHEN insufficient_privilege THEN
        NULL;
    END;

    RESET ROLE;
END
$conferencia$;

COMMIT;
