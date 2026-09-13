# Aquagenda - Supabase Integration & Migrations

Este diretório contém a estrutura completa de banco de dados, migrações e políticas de segurança (RLS) para o **Aquagenda**.

## Estrutura do Banco de Dados

- **`profiles`**: Perfis dos atletas com preferência de metragem de piscina (25m ou 50m) e nível de treino (`INICIANTE` / `INTERMEDIARIO` / `AVANCADO`).
- **`workouts`**: Planilhas e treinos com fases em JSONB (`Aquecimento`, `Preparatória`, `Principal`, `Soltura`), ritmo, séries e distância.
- **`swim_set_records`**: Registros das séries e voltas calculadas em tempo real pelo cronômetro poolside, incluindo parciais, ritmos por 100m e variações de split.
- **`swimmer_stats`**: Métricas acumuladas de distância total, tempo de piscina e melhor tempo registrado.
- **`ciclos_treino`** e **`treinos_ciclo`**: os treinos sugeridos. É o programa do carrossel "Cada Dia 1 Treino" do @natacaocriativa: 28 dias × 3 níveis = 84 treinos. Leitura pública, escrita só pelo SQL Editor.
- **`treinos_sugeridos(p_data, p_level)`**: função que devolve o treino sugerido de uma data, com a mesma conta do carrossel, `(data - âncora) mod 28`. O treino de hoje no app é sempre o do carrossel publicado hoje, sem nenhum processo diário.

## Segurança da tabela `workouts`

A migração inicial deixava a chave pública do app (anon) criar, alterar e sobrescrever os treinos públicos (sem dono). Também deixava um usuário transformar um treino dele em público. `supabase/migrations/20260913000002_workouts_rls_seguranca.sql` fecha isso:

| Ação | Quem pode |
|---|---|
| Ler | os próprios treinos e os públicos |
| Criar | só usuário logado, com o próprio `user_id` (preenchido sozinho) |
| Alterar | só os próprios, sem trocar o dono |
| Excluir | só os próprios |

Treinos públicos passam a ser mantidos só pelo SQL Editor. O app não é afetado, porque só lê essa tabela. A migração se confere no fim: se a chave pública ainda conseguir escrever, ela desfaz tudo.

## Contas: login obrigatório

O app só abre depois do login ou cadastro (Supabase Auth). `supabase/migrations/20260913000003_contas_do_app.sql` prepara o banco:

- **Perfil no cadastro:** o gatilho `handle_new_user` grava o nome e o nível (`INICIANTE` / `INTERMEDIARIO` / `AVANCADO`) enviados pelo app. O e-mail do perfil é sempre o da conta.
- **Cada um só com o que é seu:** `profiles`, `swim_set_records` e `swimmer_stats` passam a ter RLS só do dono. Antes, os e-mails de todos os perfis eram públicos e a chave do app gravava séries sem dono.
- **Excluir a conta:** a função `excluir_minha_conta()` apaga a conta de quem está logado. Perfil, treinos, séries e estatísticas saem junto.

A migração se confere no fim: se a chave pública ainda conseguir ler perfis ou gravar séries, ela desfaz tudo.

Se a confirmação de e-mail estiver ligada em *Authentication → Providers → Email*, o cadastro pelo app avisa para confirmar o e-mail antes de entrar.

### Esqueci minha senha

A recuperação acontece dentro do app, com um **código** enviado por e-mail:

1. A pessoa digita o e-mail e o app pede o envio (`POST /auth/v1/recover`).
2. A pessoa digita o código, a senha nova e a confirmação. O app troca o código por uma sessão (`POST /auth/v1/verify`, `type: recovery`), grava a senha (`PUT /auth/v1/user`) e entra na conta.

O e-mail padrão "Reset Password" do Supabase só traz um link, e o link abre no navegador, não no app. Por isso o modelo `supabase/templates/redefinir_senha.html` mostra o código (`{{ .Token }}`).

O Supabase só aceita um novo envio para o mesmo e-mail depois de 60 segundos, e a tela respeita isso.

### Modelos de e-mail em português

O Supabase só deixa editar os modelos com **SMTP próprio**. O projeto usa um servidor SMTP particular, configurado em *Authentication → Emails → SMTP Settings*. Com o SMTP ligado, o limite padrão é de 30 e-mails por hora (ajustável em *Rate Limits*).

Os modelos de `supabase/templates/` estão aplicados em *Authentication → Emails → Templates* (desde 13/09/2026). O assunto de cada um está no comentário do topo do arquivo:

| Modelo no Supabase | Arquivo |
|---|---|
| Confirm sign up | `confirmar_cadastro.html` |
| Invite user | `convite.html` |
| Magic link or OTP | `link_de_acesso.html` |
| Change email address | `trocar_email.html` |
| Reset password | `redefinir_senha.html` |
| Reauthentication | `reautenticacao.html` |
| Password changed (Security) | `aviso_senha_alterada.html` |
| Email address changed (Security) | `aviso_email_alterado.html` |
| Phone number changed (Security) | `aviso_telefone_alterado.html` |
| Sign-in method linked (Security) | `aviso_login_vinculado.html` |
| Sign-in method removed (Security) | `aviso_login_removido.html` |
| MFA method added (Security) | `aviso_verificacao_adicionada.html` |
| MFA method removed (Security) | `aviso_verificacao_removida.html` |

Os avisos de segurança vêm desligados ("Enable notification"). Traduzir não liga nenhum deles. **Pendente:** as traduções dos 7 avisos de segurança (`aviso_*.html`) estão prontas aqui, mas ainda não foram coladas no painel.

Ao mudar um arquivo, cole de novo no painel, sem o comentário do topo.

## Concluir treino e publicar nas redes

A execução ao vivo percorre o treino escolhido, seja a sugestão do dia ou um de Meus treinos. Cada toque em "Repetição feita" conta uma repetição da série ("8x75m" = 8 toques de 75m). A tela fica ligada durante o treino, e o tempo usa um relógio que não atrasa nem pula.

Ao concluir, a pessoa dá as notas de **Intensidade** e **Complexidade** de 0 a 10, que o carrossel pede na legenda, e o treino é gravado em `treinos_realizados` (`supabase/migrations/20260913000004_treinos_realizados.sql`):

- **RLS só do dono:** cada um vê, registra e apaga só os próprios. Não há UPDATE: o registro é o que aconteceu.
- **Gatilho:** soma metros, tempo e quantidade em `swimmer_stats`, e desconta se o registro for apagado. Também marca `workouts.is_completed` quando o treino feito é um de Meus treinos da própria pessoa.
- **Travas:** metros e séries feitos não passam dos planejados, notas vão de 0 a 10, e a observação tem até 500 letras.

Depois de salvar, o app gera uma **imagem do treino** em formato de feed (4:5) ou de stories (9:16) e abre o menu de compartilhar do Android. A legenda, com as hashtags do carrossel, vai junto e também é copiada para a área de transferência, porque o Instagram ignora o texto que vem com a imagem. Não há publicação automática: postar pela API do Instagram exigiria aprovação da Meta para cada pessoa.

## Treinos sugeridos (carrossel → banco → app)

A fonte é o `treinos.json` do repositório [natacao-treinos](https://github.com/Nicevargas/natacao-treinos), o mesmo arquivo que gera o carrossel do Instagram.

1. Rode no **SQL Editor**, uma vez, `supabase/migrations/20260913000001_treinos_sugeridos_do_carrossel.sql`. Ela cria as tabelas e a função e acrescenta o nível `INICIANTE`.
2. Rode `supabase/seed/treinos_ciclo.sql`. Ele grava os 84 treinos. Pode rodar de novo quando quiser: é upsert, e se o ciclo ficar incompleto a transação é desfeita.
3. Quando o programa do carrossel mudar, gere o seed de novo e repita o passo 2:

```bash
python scripts/carrossel_para_supabase.py
```

O script baixa o `treinos.json` do GitHub e regrava **as duas cópias**: o seed SQL e `app/src/main/assets/treinos_ciclo.json`, que o app usa quando está offline. Recompile o app para atualizar a cópia embarcada.

Teste rápido no SQL Editor:

```sql
SELECT ciclo_dia, foco, level, total_distance_meters
FROM treinos_sugeridos(CURRENT_DATE);
```

Tempo estimado e calorias **não vêm do carrossel**. São estimativas do script: ritmo médio por nível mais os intervalos, e ~8 kcal/min.

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
