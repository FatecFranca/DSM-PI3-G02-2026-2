# City Pizzas — Projeto Interdisciplinar (até a Sprint 2)

Java 21 + Spring Boot + MongoDB (NoSQL) e front-end HTML/CSS/JavaScript.
Este pacote contém o código da Sprint 2 (PB01–PB06). Telas de pedidos, produtos
 e histórico estavam no ZIP original como protótipo e **não estão concluídas nem
validadas** — esses módulos são da Sprint 3 em diante.

## Requisitos
- Java 21 e Maven 3.9+ (ou executar `mvnw.cmd` no Windows). O Dev Container foi alinhado para Java 21 e porta 8080.
- MongoDB local ou Atlas, com `MONGODB_URI` configurada.
- Para a recuperação por e-mail, um SMTP real configurado (veja abaixo).

## Inicialização
1. No primeiro uso, configure as variáveis de ambiente abaixo.
2. Execute o backend a partir da raiz: `./mvnw spring-boot:run` (Linux/macOS) ou
   `.\mvnw.cmd spring-boot:run` (PowerShell).
3. Sirva a pasta `frontend` em uma origem HTTP local, por exemplo
   `python -m http.server 5500 --directory frontend`, a partir da raiz do projeto.
4. Abra `http://localhost:5500/index.html`. A API usa `http://localhost:8080`.
   Não abra as telas via `file://`, pois o CORS permite origens HTTP locais.

## Variáveis de ambiente

| Variável | Finalidade |
| --- | --- |
| `MONGODB_URI` | Ex.: `mongodb://localhost:27017/pizzaria` (sem senha no código). |
| `BOOTSTRAP_ADMIN_NOME` | Nome do primeiro administrador. |
| `BOOTSTRAP_ADMIN_MATRICULA` | Matrícula do primeiro administrador. |
| `BOOTSTRAP_ADMIN_EMAIL` | E-mail do primeiro administrador. |
| `BOOTSTRAP_ADMIN_SENHA` | Senha forte, pelo menos 8 caracteres. |
| `SMTP_HOST` | Host SMTP, necessário à recuperação por e-mail. |
| `SMTP_PORT` | Porta SMTP (padrão: 587). |
| `SMTP_USERNAME` | Login SMTP. |
| `SMTP_PASSWORD` | Senha específica do SMTP. |
| `SMTP_FROM` | Endereço do remetente. |
| `CORS_ALLOWED_ORIGIN_PATTERNS` | Origens permitidas, separadas por vírgula. |

**Primeiro administrador:** o bootstrap é intencionalmente restrito a um banco
sem nenhum colaborador. Não existe conta/senha padrão embutida. Se o banco já
contiver colaboradores e nenhum administrador, ajuste o banco com controle de
acesso ou restaure o banco de testes; não abra cadastro público para contornar
o problema. Remova as variáveis de bootstrap após a inicialização.

**Segurança:** a API retorna um token aleatório de sessão após o login.
Endpoints de clientes exigem token válido; endpoints de colaboradores e redefinição
administrativa exigem perfil de administrador, verificado no back-end. As sessões
ficam em memória e expiram após oito horas; reiniciar a API exige novo login.
Senhas e códigos de recuperação são armazenados via BCrypt. O código de
recuperação expira em 15 minutos, permite até cinco tentativas e **nunca é
exibido na resposta da API**. Sem SMTP configurado, a recuperação não fica
operacional: o servidor avisa que o serviço está indisponível.

## Endpoints da Sprint 2

| Método | Endpoint | Perfil |
| --- | --- | --- |
| `POST` | `/auth/login` | Público |
| `POST` | `/auth/recuperar-senha` | Público (envia e-mail se configurado) |
| `POST` | `/auth/redefinir-senha` | Público (exige código válido) |
| `POST` | `/auth/logout` | Usuário autenticado |
| `POST` | `/auth/admin/redefinir-senha` | Administrador |
| `GET` | `/clientes/busca?termo=...` | Usuário autenticado |
| `GET` | `/clientes` | Usuário autenticado |
| `GET` | `/clientes/{id}` | Usuário autenticado |
| `POST` | `/clientes` | Usuário autenticado |
| `PUT` | `/clientes/{id}` | Usuário autenticado |
| `GET/POST/PUT/DELETE` | `/colaboradores...` | Administrador |

Enviar `Authorization: Bearer SEU_TOKEN` nas rotas protegidas.
No login a API responde `{ "matricula", "nome", "perfil", "token" }`.

## Testes
- `./mvnw test` com Maven e dependências disponíveis.
- Servir a interface e testar: login correto/incorreto, erro de permissão (403)
  de funcionário na API de colaboradores, cadastro e busca de cliente,
  recuperação por SMTP, redefinição de senha e logout.
- Os testes automatizados de unidade foram adicionados em `src/test`.

## Limitações para próxima sprint
- Não há ainda pedido persistido, catálogo pesquisável completo ou auditoria.
- O token em memória exige nova autenticação após reinício da API.
- Para produção, avaliar HTTPS, limitação de tentativas de login, sessão persistida,
  política de privacidade e proteção de dados pessoais.
