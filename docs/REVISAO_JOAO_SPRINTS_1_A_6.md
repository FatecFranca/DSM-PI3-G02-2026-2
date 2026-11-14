# Revisão técnica do PI Pizzaria — João Pedro — Sprints 1 a 6

## Escopo e limites

Base: ZIP `DSM-PI3-G02-2026-2.zip`, documentos 01 (visão/escopo), 02 (requisitos), 03 (backlog), 04 (sprints). Este material é uma revisão estática e um conjunto de correções direcionadas. **Não constitui homologação da aplicação**, pois não foi possível executar o Maven e integrar um banco MongoDB neste ambiente.

## Responsabilidades de João conforme Documento 04

- Sprint 1: iniciar projeto Java/Spring Boot, pacotes, dependências, organização Git e convenções. Estrutura encontrada no ZIP; conferir convenções com grupo.
- Sprint 2: autenticação por matrícula/senha, perfis, conferência de autorização. Existiam `AuthServicy`, `SessaoService` e interceptor; reforçada a restrição das rotas de administração.
- Sprint 3: criação de pedido, vínculo com cliente e colaborador, pizza inteira/meio a meio, preparo e preço-base. Lógica presente em `PedidoService`; requer teste com MongoDB e interfaces.
- Sprint 4: modalidade entrega/retirada, taxa e cálculo final. Regras presentes em `PedidoService`; dependem das configurações `PRECO_BASE_PIZZA` e `TAXA_ENTREGA` e de integração com o front-end.
- Sprint 5: regras administrativas, proteção de rotas e segurança em conjunto com Cauã. Reforçadas permissões e adicionados registros de alteração/exclusão de pedido e redefinição administrativa de senha.
- Sprint 6: verificação do backlog, registro de pendências e testes de API junto com Ronie. Incluída coleção Postman de referência; deve ser executada e adaptada ao banco de testes.

## Alterações aplicadas

1. `config/AutorizacaoInterceptor.java`: bloqueia funcionário comum em `/auditoria` e `/pedidos/admin/{id}` já no interceptor, reforçando checagem existente no service.
2. `config/CorsConfig.java`: inclui `/auditoria` na lista de rotas autenticadas.
3. `controller/AuditoriaController.java` (novo): implementa `GET /auditoria` esperado pelo JavaScript existente, com filtros `termo`, `de` e `ate`; resposta com `dataHora`, `usuario`, `acao`, `registro`.
4. `controller/PedidoController.java`: registra auditoria para alteração e exclusão realizadas pelo administrador.
5. `controller/AuthController.java`: registra auditoria quando administrador redefine a senha de um colaborador.
6. `config/AutorizacaoInterceptorTest.java`: cobre bloqueio a funcionário comum nas novas rotas administrativas.
7. `docs/PI_Pizzaria_Sprint6_Postman.json`: coleção auxiliar para testar API sem interferir no front-end.

Não foram modificados HTML/CSS/JS, models do domínio, repositories, services dos colegas nem o modelo do banco. Não foram alterados nomes dos endpoints preexistentes.

## Pendências importantes antes da entrega

- **Não testado:** compilação, testes JUnit, execução ponta a ponta com MongoDB e servidor de e-mail. Maven Wrapper tentou baixar o Maven 3.9.16 de repositório inacessível neste ambiente.
- **Auditoria incompleta:** a documentação prevê registrar alterações importantes de colaboradores, correções de valores e cadastros de produtos. A implementação existente tem um histórico separado `ProdutoAuditoria`; não foi unificada nem migrada para `Auditoria`. A nova consulta não inclui esse histórico separado.
- **Operações com banco e auditoria:** salvar/excluir pedido e registrar auditoria são operações consecutivas; uma falha no registro pode deixar a alteração realizada sem histórico. Avaliar persistência transacional e tratamento de erro antes da homologação.
- **Senha administrativa:** a troca é gravada antes da auditoria. Validar comportamento se o banco de auditoria ficar indisponível.
- **Filtros por data:** a nova rota aplica limites usando UTC. Validar datas no fuso de operação da pizzaria para registros próximos da meia-noite.
- **Testes faltantes:** conferir usuários administrativos e comuns, acesso direto às rotas, cadastro de produto e cliente, duas modalidades de pedido, preço-base, acréscimos, pagamento, impressão, redefinição de senha e auditoria.
- **Documentação e vídeo:** anexar documentos acadêmicos no GitHub, inserir link público do vídeo (até 5 minutos, com participação de todos) e enviar os dois links pelo formulário final.

## Como validar localmente

1. Trabalhar em **uma branch nova** e comparar os arquivos com a versão dos colegas, antes de mesclar.
2. Configurar `MONGODB_URI`, `PRECO_BASE_PIZZA`, `TAXA_ENTREGA` e outros dados obrigatórios conforme `application.properties`/README, sem publicar segredos.
3. Com Java 21 e Maven disponíveis, executar `./mvnw test` (Windows: `mvnw.cmd test`), depois `./mvnw spring-boot:run`.
4. Fazer login como funcionário e como administrador; testar 401 sem sessão, 403 para funcionário e 200/204 para administrador nas rotas protegidas.
5. Testar o fluxo de pedido, edição e exclusão administrativa e conferir consulta `/auditoria` no painel admin.
6. Registrar no backlog os itens validados e os que ainda estão pendentes.

## Observação de segurança e compatibilidade

O ZIP recebido continha metadados `.git`, saídas de compilação (`target`) e diferenças de final de linha em relação ao índice Git. A cópia revisada entregue omite `.git` e `target` para ser leve e evitar sobrescrever o histórico/branches do projeto original. **Não substitua o repositório atual integralmente**; compare e incorpore somente os arquivos alterados.
