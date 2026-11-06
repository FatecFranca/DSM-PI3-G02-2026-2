# City Pizzas — Sprint 2 + início da Sprint 3 (tarefas do João Pedro)

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
- A tela protótipo de pedidos continua em desenvolvimento. Por segurança,
  a impressão é cancelada se a API rejeitar o salvamento; não considerar essa
  tela como a interface final da Sprint 3.

## Executar no Windows (PowerShell)

No diretório com `pom.xml`:

```powershell
$env:MONGODB_URI = "SUA_URI_MONGODB_ATLAS_OU_LOCAL"
# Configure o preço-base aprovado pela pizzaria (valor abaixo apenas ilustrativo):
$env:PIZZA_PRECO_BASE = "59.00"
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

   O primeiro item recebe preparo `ASSADA` automaticamente. Para `meioAMeio:true`,
   `segundoProdutoId` é obrigatório e deve ser um sabor diferente. O valor da
   pizza é buscado da **configuração única do servidor**, nunca do preço do
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
