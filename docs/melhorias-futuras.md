# Melhorias futuras do app Natação Criativa

Ideias pedidas em 16/09/2026 pela Nice e pelo professor. **Ainda não implementadas**: por enquanto só têm estimativa, na planilha `NatacaoCriativa-custos-e-publicacao.xlsx`, aba Melhorias.

## Feitas em 16/09/2026

As duas abaixo já estão no app: o treino compartilhado vai com imagem e link (`nicevargas.github.io/natacao-treinos/treino.html#<código>`), e também dá para abrir colando o código em Meus treinos.

### 1. Editar o treino antes de nadar
Na hora de entrar no treino, a pessoa troca ou tira uma série que não quer ou não consegue fazer.
- Botão "Editar este treino" na aba Treinos e no plano, reaproveitando o editor de Meus treinos (`EditorDeTreinoScreen`, `MontadorDeTreino.paraDigitacao`).
- A versão editada vai para a execução sem salvar antes. **Só vai para Meus treinos quando o treino é concluído.**
- Estimativa: 6 a 12 h.

### 2. Compartilhar treino por link
Mandar o treino para outro usuário do app, pelo WhatsApp ou pelas redes.
- **Um link só** para os três casos: quem toca abre o treino no app, ou uma página com o treino se não tiver o app.
- Quem recebe e **conclui** o treino fica com ele salvo em Meus treinos.
- Tabela de treinos compartilhados com código curto e RLS.
- A página pública fica **fora do Site_professor**, porque os projetos são independentes (ex.: GitHub Pages).
- O link abrir direto no app (App Links) precisa da chave de assinatura de produção do Android.
- Estimativa: 16 a 30 h.

## Registradas para depois (pedido do professor)

### 3. Biblioteca de vídeos dos exercícios
O aluno se filma fazendo o exercício e envia o link do YouTube ou do Drive. A biblioteca cresce sem precisar filmar ninguém.
- O professor aprova o vídeo antes de ele aparecer para todos.
- No treino, tocar no exercício abre o vídeo.
- Estimativa: 25 a 50 h.

### 4. Timeline social
Foto e texto do que a pessoa nadou hoje. Os outros veem, curtem, comentam e se motivam: "quem vê alguém nadando quer nadar também".
- Ver o que cada usuário nadou e não nadou, e interagir com ele.
- Participação por escolha da pessoa (opt-in).
- **Denunciar e bloquear**: exigido pela Play Store e pela App Store para conteúdo de usuário.
- Fotos devem passar do plano grátis do Supabase (Pro: cerca de US$ 25/mês).
- Estimativa: 50 a 100 h.
