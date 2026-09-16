-- ==============================================================================
-- APP NATAÇÃO CRIATIVA - PERCEPÇÃO DE ESFORÇO (PSE) OBRIGATÓRIA
-- Arquivo: 20260915000002_pse_obrigatoria.sql
--
-- Todo treino salvo passa a ter a nota de esforço de 0 a 10 (coluna
-- intensidade), para a estatística e o acompanhamento da intensidade.
--
-- NOT VALID: vale só para os treinos registrados daqui para frente. Os que já
-- estão no banco sem nota ficam como estão; nada é apagado nem alterado.
-- ==============================================================================

BEGIN;

ALTER TABLE public.treinos_realizados
    DROP CONSTRAINT IF EXISTS treinos_realizados_pse_obrigatoria;

ALTER TABLE public.treinos_realizados
    ADD CONSTRAINT treinos_realizados_pse_obrigatoria
    CHECK (intensidade IS NOT NULL) NOT VALID;

COMMENT ON COLUMN public.treinos_realizados.intensidade IS
    'Percepção de esforço (PSE) de 0 (nada cansado) a 10 (máximo). Obrigatória a partir de 15/09/2026.';

COMMIT;
