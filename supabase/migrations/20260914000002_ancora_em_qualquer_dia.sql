-- ==============================================================================
-- AQUAGENDA - CICLO PODE COMEÇAR EM QUALQUER DIA DA SEMANA
-- Arquivo: 20260914000002_ancora_em_qualquer_dia.sql
--
-- O Método NC começa numa terça (15/09/2026), e ciclos_treino exigia âncora na
-- segunda-feira. Com o ciclo múltiplo de 7 (que continua exigido), cada foco
-- segue preso a um dia fixo da semana; só muda qual dia.
--
-- Só remove essa restrição: não apaga nem altera dados.
-- Rode ANTES de supabase/seed/programa_nc.sql.
-- ==============================================================================

ALTER TABLE public.ciclos_treino DROP CONSTRAINT IF EXISTS ciclos_treino_ancora_check;
