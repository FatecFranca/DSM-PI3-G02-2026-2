# City Pizzas — Sprints 2 e 3: catálogo, pedidos e melhorias

Projeto Java 21 + Spring Boot 4.1.1 + MongoDB + HTML/CSS/JavaScript. Esta entrega
parte do ZIP mais recente enviado pela equipe; não substitui as demais tarefas da Sprint 3.

## O que entrou nesta atualização

- **PB02**: recuperação por e-mail com envio SMTP (`/auth/recuperar-senha`).
  O código continua sendo gerado com seis dígitos, salvo em hash BCrypt,
  válido por 15 minutos e com até cinco tentativas. Ele nunca é devolvido pela API.
  O serviço passa a informar quando o SMTP não foi configurado.
- **Sprint 3 — João Pedro / PB10 (RF13)**: criação de pedido vinculada a um
  cliente existente e ao colaborador identificado pelo token de sessão.
- **Sprint 3 — João Pedro / PB12–PB14 (RF15–RF17)**: pizza inteira ou meio a
  meio (dois sabores distintos), preparo assada por padrão ou pré-assada e
  preço-base único independente da escolha dos sabores.
- Os pedidos são gravados na coleção MongoDB `pedidos`, inicialmente
  `EM_ABERTO`. O campo `subtotalItens` **não** é o total final com entrega e
  pagamento, que serão implementados na Sprint 4.
- A interface de pedidos busca os preços das pizzas na API e permite a pesquisa
  de pedidos `EM_ABERTO`. Entrega e pagamento continuam fora da persistência
  até a Sprint 4.

## Executar no Windows (PowerShell)

No diretório com `pom.xml`:

```powershell
$env:MONGODB_URI = "SUA_URI_MONGODB_ATLAS_OU_LOCAL"
# Preço da pizza ASSADA (inteira ou meio a meio); valor apenas ilustrativo:
$env:PIZZA_PRECO_BASE = "59.00"
# Preço da pizza PRÉ-ASSADA (inteira ou meio a meio); valor apenas ilustrativo:
$env:PIZZA_PRECO_PRE_ASSADA = "49.00"
.\mvnw.cmd spring-boot:run
```

A API atende em `http://localhost:8080`. Abra `frontend/index.html` com Live Server
(`http://127.0.0.1:5500/...`) e faça login. **Mantenha o terminal da API aberto.**

Se o `MONGODB_URI` não for definido, a aplicação tentará `mongodb://localhost:27017/pizzaria`.
O preço-base **não** possui valor padrão porque o cliente ainda não definiu seu
valor nos documentos de requisitos. Sem `PIZZA_PRECO_BASE`, a criação de pizza
retorna `503` com mensagem de configuração pendente. Não utilize valores
ilustrativos em produção sem validar com a pizzaria.

## PB02 — configurar e receber recuperação de senha por e-mail

**O envio não funciona sem uma conta/servidor SMTP real.** Configure as variáveis
na mesma janela de terminal antes de executar o Spring Boot. Exemplo usando
SMTP do Gmail (se sua conta permitir senha de aplicativo):

```powershell
$env:SMTP_HOST = "smtp.gmail.com"
$env:SMTP_PORT = "587"
$env:SMTP_USERNAME = "conta.remetente@gmail.com"
$env:SMTP_PASSWORD = "SENHA_DE_APLICATIVO_DO_EMAIL"
$env:SMTP_FROM = "conta.remetente@gmail.com"
$env:SMTP_AUTH = "true"
$env:SMTP_STARTTLS = "true"
$env:SMTP_SSL = "false"
.\mvnw.cmd spring-boot:run
```

**Não use a senha normal da conta quando o provedor exigir uma senha de
aplicativo.** Não grave senhas no `application.properties`, `.env` versionado,
prints, relatório ou Git. As variáveis `$env:` acima valem somente para a
sessão do PowerShell; substitua os exemplos apenas no seu computador.

Teste em `index.html`:
1. Clique em **Esqueci a senha**.
2. Digite o **e-mail que já está cadastrado na coleção `colaboradores`**.
3. Clique em **Enviar código**, verifique a caixa de entrada e a pasta de spam.
4. Digite os seis dígitos recebidos e uma nova senha com pelo menos oito caracteres.
5. Entre novamente com a matrícula e a nova senha.

O endpoint `POST /auth/recuperar-senha` recebe `{ "email": "funcionario@exemplo.com" }`.
A resposta é genérica para não indicar se uma conta existe. Se não houver SMTP
configurado, a API retorna `503`; se houver erro de autenticação/entrega,
verifique as credenciais e o servidor SMTP. Um teste automatizado com simulação
de SMTP **não garante** entrega real ao Gmail/Outlook.

## Sprint 3 — criar pedido (Postman)

Este é um **endpoint para testar o back-end**, não a tela pronta do Cauã.

1. Faça `POST http://localhost:8080/auth/login` com:

   ```json
   {"matricula":"MATRICULA_EXISTENTE", "senha":"SENHA_EXISTENTE"}
   ```

2. Copie o campo `token` da resposta. Nas requisições abaixo, inclua
   `Authorization: Bearer TOKEN_RECEBIDO` e `Content-Type: application/json`.
3. Use `GET /clientes` para obter um `clienteId` real e `GET /pizzas` para
   escolher IDs de pizzas já cadastradas. Esta entrega **não cria pizzas**:
   cadastro de produtos e catálogo são tarefas do Ronie na Sprint 3.
4. Crie um pedido em `POST http://localhost:8080/pedidos` com o exemplo abaixo:

   ```json
   {
     "clienteId": "ID_REAL_DO_CLIENTE",
     "itens": [
       {
         "produtoId": "ID_REAL_DA_PIZZA_CALABRESA",
         "meioAMeio": false,
         "quantidade": 1
       },
       {
         "produtoId": "ID_REAL_DA_PIZZA_MUSSARELA",
         "segundoProdutoId": "ID_REAL_DA_PIZZA_PORTUGUESA",
         "meioAMeio": true,
         "tipoPreparo": "PRE_ASSADA",
         "quantidade": 2
       }
     ]
   }
   ```

   O primeiro item recebe preparo `ASSADA` automaticamente. O preço depende do preparo
   (`ASSADA` ou `PRE_ASSADA`) mesmo quando `meioAMeio:true`. Para `meioAMeio:true`,
   `segundoProdutoId` é obrigatório e deve ser um sabor diferente. O valor da
   pizza é buscado da **configuração de preparo do servidor**, nunca do preço do
   sabor/JSON. Quantidade é de 1 a 100. IDs precisam existir no MongoDB.
5. Em caso de sucesso, o servidor retorna `201 Created` com `id`, `clienteId`,
   `colaboradorId` (do token), `colaboradorMatricula`, `criadoEm`, `itens`,
   `subtotalItens` e `situacao=EM_ABERTO`.
6. Confirme a persistência usando `GET /pedidos/ID_RETORNADO` com o mesmo token.

### Restrições importantes

- Uma requisição sem token recebe `401`; não é possível escolher o atendente
  enviando `colaboradorId` no JSON.
- Cliente ou pizza inexistentes retornam `404`; combinações de sabores inválidas,
  quantidade inválida ou pedido vazio recebem `400`.
- Campos de entrega, pagamento, observações e acréscimos ainda **não são
  recebidos** neste endpoint. Em vez de descartá-los silenciosamente, o DTO
  rejeita campos não suportados. Integração completa da tela vem com as tarefas
  do Ronie e Cauã e com as regras da Sprint 4.
- `DELETE /pedidos/{id}`, cálculo final, auditoria e impressão no servidor
  **não foram implementados nesta entrega**.

## Testes

```powershell
.\mvnw.cmd test
```

Há testes unitários do serviço de pedidos, do envio de e-mail simulado e
rejeição de campos não suportados, além dos testes já presentes no ZIP.
Teste real de SMTP requer credenciais válidas e teste de persistência exige MongoDB.

## Alterações e escopo

Ver `RELATORIO_PB02_SPRINT3_JOAO.md` para a lista de arquivos, critérios de
validação e responsabilidades conforme o planejamento de sprints.

## Atualização PB07 / PB08 / pesquisa de pedidos em aberto

- Em **Cadastros > Produto**, funcionários logados podem cadastrar, pesquisar,
  editar e excluir produtos. Para bebidas, a apresentação/volume é obrigatória:
  registre separadamente, por exemplo, **Coca-Cola lata 350 ml** e
  **Coca-Cola 2 litros** (mesmo nome, apresentações diferentes).
- Os campos `criadoPorId` e `alteradoPorId` são preenchidos no servidor pelo token
  do colaborador autenticado, e as ações de criação/alteração/exclusão são
  rastreadas na coleção `auditoria_produtos`.
- Em **Pedidos anteriores**, a parte superior exibe **Pedidos em aberto** consultados
  diretamente no MongoDB, com busca por ID, nome/telefone do cliente ou matrícula.
  Os registros de impressão abaixo continuam locais ao navegador.
- Para preços, configure `PIZZA_PRECO_BASE` (pizza **assada**) e,
  **opcionalmente**, `PIZZA_PRECO_PRE_ASSADA` (pizza **pré-assada**).
  O preço é definido pelo **tipo de preparo**, tanto para pizza inteira quanto
  meio a meio; não faz média nem cobra pela quantidade de sabores (PB14).
  Se o preço pré-assada não for definido, usa o mesmo valor da assada.
  **ATENÇÃO:** confirme os dois valores comerciais com o PO/cliente; os PDFs
  fornecidos não determinam um valor específico para pizza pré-assada.
- `GET /pedidos/precos` devolve os preços para o frontend; `GET /pedidos/abertos?termo=...`
  lista e pesquisa os pedidos `EM_ABERTO`. Ambos exigem login válido.
- No quadrado de visualização de 64×64 px, a pizza inteira usa um X e
  a meio a meio usa uma diagonal. Outros produtos mantêm o quadrado sem traços.
- Corrigidos o envio duplicado de pedido no frontend e o subtotal do backend
  para considerar também os acréscimos de R$ 5,00 por ingrediente.

### Teste manual recomendado

1. Configure o MongoDB, `PIZZA_PRECO_BASE` e, se necessário, `PIZZA_PRECO_PRE_ASSADA`.
2. Rode `./mvnw spring-boot:run` (ou ` .\mvnw.cmd spring-boot:run` no PowerShell)
   e abra `frontend/index.html` pelo Live Server (porta 5500).
3. Faça login como funcionário. Em **Cadastros > Produto**, cadastre uma bebida
   com apresentação **350 ml**, outra da mesma marca com **2 litros** e pelo menos
   dois sabores de pizza (categoria **PIZZA**).
4. No pedido, busque os produtos por nome e volume; confira o X e a diagonal,
   teste o segundo sabor e alterne Assada/Pré-assada para conferir os preços,
   independentemente da escolha Inteira/Meio a meio.
5. Crie um pedido e, em **Pedidos anteriores**, procure por nome ou telefone do cliente.
6. Confira no MongoDB as coleções `produtos`, `pedidos` e `auditoria_produtos`.

**Situação dos testes nesta entrega:** JavaScript validado com `node --check` e
as três interfaces exercitadas no Chromium com respostas simuladas da API.
O backend Spring Boot e os testes JUnit precisam ser executados no ambiente
local com Maven e MongoDB; o ambiente de edição não conseguiu baixar o Maven.


## Reintegração Sprint 4

Consulte [ALTERACOES_INTEGRADAS_SPRINT4.md](ALTERACOES_INTEGRADAS_SPRINT4.md) para o escopo, as rotas, as variáveis necessárias e o roteiro de validação. Os preços da pizza foram preservados da versão-base.
