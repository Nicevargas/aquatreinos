-- ==============================================================================
-- AQUAGENDA - SCHEMA DE BANCO DE DADOS SUPABASE (POSTGRESQL)
-- Migração Inicial: Criação de Tabelas, RLS, Triggers e Índices
-- Arquivo: 20260912000001_create_aquagenda_schema.sql
-- ==============================================================================

-- 1. EXTENSÕES
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 2. TABELA DE PERFIS DE ATLETAS (PROFILES)
-- Vinculada automaticamente aos usuários registrados no Supabase Auth
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    email TEXT,
    full_name TEXT,
    avatar_url TEXT,
    preferred_pool_meters INT DEFAULT 25 CHECK (preferred_pool_meters IN (25, 50)),
    training_level TEXT DEFAULT 'INTERMEDIARIO' CHECK (training_level IN ('INTERMEDIARIO', 'AVANCADO')),
    created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 3. TABELA DE TREINOS AGENDADOS / PLANILHAS (WORKOUTS)
CREATE TABLE IF NOT EXISTS public.workouts (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid()::text,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    subtitle TEXT,
    tag TEXT DEFAULT 'Treino Principal',
    workout_date DATE NOT NULL DEFAULT CURRENT_DATE,
    total_distance_meters INT NOT NULL DEFAULT 0,
    estimated_minutes INT DEFAULT 50,
    calories INT DEFAULT 450,
    level TEXT NOT NULL DEFAULT 'INTERMEDIARIO' CHECK (level IN ('INTERMEDIARIO', 'AVANCADO')),
    is_completed BOOLEAN DEFAULT FALSE,
    phases JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 4. TABELA DE SÉRIES E VOLTAS CRONOMETRADAS (SWIM_SET_RECORDS)
-- Registra as séries salvas pelo cronômetro em tempo real
CREATE TABLE IF NOT EXISTS public.swim_set_records (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid()::text,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    workout_id TEXT REFERENCES public.workouts(id) ON DELETE SET NULL,
    set_number INT NOT NULL,
    rep_description TEXT DEFAULT '8x100m Crawl',
    distance_meters INT DEFAULT 100,
    time_formatted TEXT NOT NULL, -- Ex: "01:21.2"
    time_millis BIGINT NOT NULL,   -- Ex: 81200L
    pace_per_100m TEXT,            -- Ex: "1'21\"/100m"
    split_difference TEXT,         -- Ex: "-0.7s" ou "Base"
    created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 5. TABELA DE ESTATÍSTICAS DO NADADOR (SWIMMER_STATS)
CREATE TABLE IF NOT EXISTS public.swimmer_stats (
    user_id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    total_distance_meters BIGINT DEFAULT 0,
    total_time_seconds BIGINT DEFAULT 0,
    completed_workouts_count INT DEFAULT 0,
    best_100m_pace TEXT,
    updated_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 6. ÍNDICES DE PERFORMANCE
CREATE INDEX IF NOT EXISTS idx_workouts_user_date ON public.workouts(user_id, workout_date);
CREATE INDEX IF NOT EXISTS idx_workouts_level ON public.workouts(level);
CREATE INDEX IF NOT EXISTS idx_swim_sets_workout ON public.swim_set_records(workout_id);
CREATE INDEX IF NOT EXISTS idx_swim_sets_user ON public.swim_set_records(user_id);
CREATE INDEX IF NOT EXISTS idx_swim_sets_created ON public.swim_set_records(created_at DESC);

-- 7. FUNÇÃO E TRIGGER PARA ATUALIZAÇÃO AUTOMÁTICA DE updated_at
CREATE OR REPLACE FUNCTION public.handle_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = timezone('utc'::text, now());
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS tr_profiles_updated_at ON public.profiles;
CREATE TRIGGER tr_profiles_updated_at
    BEFORE UPDATE ON public.profiles
    FOR EACH ROW EXECUTE FUNCTION public.handle_updated_at();

DROP TRIGGER IF EXISTS tr_workouts_updated_at ON public.workouts;
CREATE TRIGGER tr_workouts_updated_at
    BEFORE UPDATE ON public.workouts
    FOR EACH ROW EXECUTE FUNCTION public.handle_updated_at();

-- 8. TRIGGER DE CRIAÇÃO AUTOMÁTICA DE PERFIL QUANDO O USUÁRIO SE REGISTRA
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.profiles (id, email, full_name, avatar_url)
    VALUES (
        NEW.id,
        NEW.email,
        NEW.raw_user_meta_data->>'full_name',
        NEW.raw_user_meta_data->>'avatar_url'
    )
    ON CONFLICT (id) DO NOTHING;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- 9. SEGURANÇA E POLÍTICAS DE ACESSO (ROW LEVEL SECURITY - RLS)
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.workouts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.swim_set_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.swimmer_stats ENABLE ROW LEVEL SECURITY;

-- Políticas para Profiles
CREATE POLICY "Usuários podem ver seu próprio perfil ou perfis públicos"
    ON public.profiles FOR SELECT
    USING (auth.uid() = id OR true);

CREATE POLICY "Usuários podem atualizar seu próprio perfil"
    ON public.profiles FOR UPDATE
    USING (auth.uid() = id);

-- Políticas para Workouts
CREATE POLICY "Permitir leitura de treinos públicos ou do próprio usuário"
    ON public.workouts FOR SELECT
    USING (auth.uid() = user_id OR user_id IS NULL);

CREATE POLICY "Usuários podem criar seus próprios treinos"
    ON public.workouts FOR INSERT
    WITH CHECK (auth.uid() = user_id OR user_id IS NULL);

CREATE POLICY "Usuários podem atualizar seus próprios treinos"
    ON public.workouts FOR UPDATE
    USING (auth.uid() = user_id OR user_id IS NULL);

CREATE POLICY "Usuários podem excluir seus próprios treinos"
    ON public.workouts FOR DELETE
    USING (auth.uid() = user_id);

-- Políticas para Swim Set Records
CREATE POLICY "Permitir leitura de séries"
    ON public.swim_set_records FOR SELECT
    USING (auth.uid() = user_id OR user_id IS NULL);

CREATE POLICY "Permitir inserção de séries"
    ON public.swim_set_records FOR INSERT
    WITH CHECK (auth.uid() = user_id OR user_id IS NULL);

CREATE POLICY "Permitir exclusão de séries próprias"
    ON public.swim_set_records FOR DELETE
    USING (auth.uid() = user_id);

-- Políticas para Swimmer Stats
CREATE POLICY "Usuários podem ver suas estatísticas"
    ON public.swimmer_stats FOR SELECT
    USING (auth.uid() = user_id OR user_id IS NULL);

CREATE POLICY "Usuários podem atualizar suas estatísticas"
    ON public.swimmer_stats FOR ALL
    USING (auth.uid() = user_id OR user_id IS NULL);

-- 10. DADOS INICIAIS (SEED DATA) PARA AQUAGENDA
INSERT INTO public.workouts (id, user_id, title, subtitle, tag, workout_date, total_distance_meters, estimated_minutes, calories, level, is_completed, phases)
VALUES
(
    'workout_inter_13',
    NULL,
    'Performance Day',
    'Série técnica de resistência',
    'Treino Principal',
    CURRENT_DATE,
    2500,
    55,
    480,
    'INTERMEDIARIO',
    false,
    '[
        {"id":"p1","title":"Aquecimento","summary":"400m Crawl Relaxado","distanceMeters":400,"percentage":15,"status":"COMPLETED","sets":[{"id":"s1","repsDescription":"1x400","stroke":"Crawl Relaxado","interval":"8''00\"","intensity":"Z1 (60%)","restSeconds":30,"equipment":null,"isDone":true}]},
        {"id":"p2","title":"Preparatória","summary":"200m Educativos Medley","distanceMeters":200,"percentage":10,"status":"COMPLETED","sets":[{"id":"s2","repsDescription":"4x50","stroke":"Educativos Medley","interval":"1''15\"","intensity":"Z2 (65%)","restSeconds":20,"equipment":"Pull Buoy","isDone":true}]},
        {"id":"p3","title":"Principal","summary":"8x100m Crawl c/ Palmar + 1x600m c/ Nadadeira","distanceMeters":1400,"percentage":60,"status":"CURRENT","sets":[{"id":"s3","repsDescription":"8x100","stroke":"Crawl Ritmo Firme","interval":"1''45\"","intensity":"Z3 (75%)","restSeconds":30,"equipment":"Palmar","isDone":true},{"id":"s4","repsDescription":"1x600","stroke":"Crawl c/ Nadadeira","interval":"10''00\"","intensity":"Z2 (70%)","restSeconds":45,"equipment":"Nadadeira","isDone":false}]},
        {"id":"p4","title":"Soltura","summary":"500m Nado Livre Suave","distanceMeters":500,"percentage":15,"status":"UPCOMING","sets":[{"id":"s5","repsDescription":"1x500","stroke":"Soltura Costas/Peito","interval":"10''00\"","intensity":"Z1 (50%)","restSeconds":0,"equipment":null,"isDone":false}]}
    ]'::jsonb
),
(
    'workout_adv_13',
    NULL,
    'Aeróbico & Fôlego',
    'Capacidade aeróbica e limiar de lactato',
    'Treino Intenso',
    CURRENT_DATE,
    3200,
    65,
    620,
    'AVANCADO',
    false,
    '[
        {"id":"pa1","title":"Aquecimento","summary":"600m Misturado (Crawl + Costas)","distanceMeters":600,"percentage":18,"status":"COMPLETED","sets":[{"id":"sa1","repsDescription":"1x600","stroke":"Crawl/Costas Alternado","interval":"11''00\"","intensity":"Z1 (60%)","restSeconds":30,"equipment":null,"isDone":true}]},
        {"id":"pa2","title":"Preparatória","summary":"400m Pernada com Prancha","distanceMeters":400,"percentage":12,"status":"COMPLETED","sets":[{"id":"sa2","repsDescription":"8x50","stroke":"Pernada Crawl c/ Prancha","interval":"1''10\"","intensity":"Z3 (75%)","restSeconds":15,"equipment":"Prancha","isDone":true}]},
        {"id":"pa3","title":"Principal","summary":"10x150m Crawl Limiar + 4x100m Medley","distanceMeters":1900,"percentage":60,"status":"CURRENT","sets":[{"id":"sa3","repsDescription":"10x150","stroke":"Crawl no Limiar (Pace 1''20\")","interval":"2''30\"","intensity":"Z4 (85%)","restSeconds":20,"equipment":"Palmar + Nadadeira","isDone":false},{"id":"sa4","repsDescription":"4x100","stroke":"Medley Máximo Esforço","interval":"2''00\"","intensity":"Z5 (95%)","restSeconds":45,"equipment":null,"isDone":false}]},
        {"id":"pa4","title":"Soltura","summary":"300m Nado Suave Regenerativo","distanceMeters":300,"percentage":10,"status":"UPCOMING","sets":[{"id":"sa5","repsDescription":"1x300","stroke":"Crawl e Nado de Peito","interval":"6''00\"","intensity":"Z1 (50%)","restSeconds":0,"equipment":null,"isDone":false}]}
    ]'::jsonb
)
ON CONFLICT (id) DO NOTHING;

-- Inserir séries cronometradas de exemplo conectadas ao treino de hoje
INSERT INTO public.swim_set_records (id, user_id, workout_id, set_number, rep_description, distance_meters, time_formatted, time_millis, pace_per_100m, split_difference)
VALUES
('lap_1', NULL, 'workout_inter_13', 1, '8x100m Crawl', 100, '01:23.6', 83600, '1''23"/100m', 'Base'),
('lap_2', NULL, 'workout_inter_13', 2, '8x100m Crawl', 100, '01:22.7', 82700, '1''22"/100m', '-0.9s'),
('lap_3', NULL, 'workout_inter_13', 3, '8x100m Crawl', 100, '01:21.9', 81900, '1''21"/100m', '-0.8s'),
('lap_4', NULL, 'workout_inter_13', 4, '8x100m Crawl', 100, '01:21.2', 81200, '1''21"/100m', '-0.7s')
ON CONFLICT (id) DO NOTHING;
