# Aquagenda - Supabase Integration & Migrations

Este diretório contém a estrutura completa de banco de dados, migrações e políticas de segurança (RLS) para o **Aquagenda**.

## Estrutura do Banco de Dados

- **`profiles`**: Perfis dos atletas com preferência de metragem de piscina (25m ou 50m) e nível de treino (`INTERMEDIARIO` / `AVANCADO`).
- **`workouts`**: Planilhas e treinos com fases em JSONB (`Aquecimento`, `Preparatória`, `Principal`, `Soltura`), ritmo, séries e distância.
- **`swim_set_records`**: Registros das séries e voltas calculadas em tempo real pelo cronômetro poolside, incluindo parciais, ritmos por 100m e variações de split.
- **`swimmer_stats`**: Métricas acumuladas de distância total, tempo de piscina e melhor tempo registrado.

## Como Executar as Migrações no Supabase

### Opção 1: Via Dashboard do Supabase (Mais Rápido)
1. Acesse seu projeto no [Supabase Dashboard](https://supabase.com/dashboard).
2. Vá em **SQL Editor** no menu lateral esquerdo.
3. Clique em **New query**.
4. Copie todo o conteúdo do arquivo `supabase/migrations/20260912000001_create_aquagenda_schema.sql` e cole no editor.
5. Clique em **Run**. Todas as tabelas, índices, triggers e dados iniciais serão criados com sucesso!

### Opção 2: Via Supabase CLI
Se você utiliza a CLI do Supabase localmente:
```bash
# Linkar ao seu projeto
supabase link --project-ref seu-project-id

# Aplicar as migrações
supabase db push
```

## Como Conectar no Aplicativo Android

No Google AI Studio ou no arquivo `.env`:
1. Abra o painel **Secrets** no AI Studio.
2. Adicione ou preencha as variáveis:
   - `SUPABASE_URL`: A URL do seu projeto (ex: `https://xyzproject.supabase.co`)
   - `SUPABASE_ANON_KEY`: A chave pública anônima do projeto (encontrada em *Project Settings > API > anon public*).

O aplicativo Android detectará automaticamente a presença das credenciais através do `BuildConfig.SUPABASE_URL` e `BuildConfig.SUPABASE_ANON_KEY`, sincronizando os treinos e séries em tempo real com fallback automático para modo local caso esteja offline!
