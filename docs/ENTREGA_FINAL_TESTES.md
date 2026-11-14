# PI Pizzaria — entrega integrada e roteiro de validação

Este pacote deve ser **extraído diretamente sobre a pasta do projeto original**, na branch de testes. A pasta `.git` não está incluída; o ZIP não altera o histórico do repositório.

## Ajustes incluídos

- **Login:** corrigido o espaço interno dos campos para que os ícones de matrícula e senha não fiquem sobre o texto, também em telas menores.
- **CEP:** adicionado botão **Buscar CEP** no cadastro do cliente e no endereço de entrega do pedido, com estado *Buscando...*, confirmação, erro e opção de preenchimento manual.
- **Números de pedido:** o MongoDB passa a definir o número comercial sequencial, compartilhado entre computadores; não depende mais do contador do navegador. Editar ou finalizar não altera o número.
- **Pedidos já existentes:** na inicialização do servidor, pedidos sem número recebem números por ordem de criação, preservando o `id` MongoDB. Uma sequência persistida impede reinício do contador. Faça **backup do MongoDB** antes de executar a nova versão com dados reais.
- **Histórico, impressão e painel:** mostram `Pedido 1`, `Pedido 6` etc., e não o ID técnico do MongoDB; o histórico de auditoria associa o número ao pedido, inclusive após exclusão.
- **Administração:** corrigida a rota usada pelo botão de excluir pedido (`/pedidos/admin/{id}`); adicionado formulário para corrigir total com motivo obrigatório, registrando valor anterior e novo na auditoria.
- **Auditoria:** ampliado registro de criação, atualização, finalização e exclusão de pedidos, e de alterações em produtos e colaboradores.
- **Testes JUnit:** incluídas verificações para a estabilidade da numeração na edição e para autorização/correção administrativa de valores.

## Configuração

1. Verifique se o MongoDB está ligado e se `MONGODB_URI` aponta para o banco correto.
2. Confira os valores em `src/main/resources/application.properties`: `PIZZA_PRECO_BASE`, `PIZZA_PRECO_PRE_ASSADA`, `TAXA_ENTREGA`. Os padrões `60.00`, `50.00` e `7.00` **são exemplos para teste**, não preços validados pela pizzaria.
3. Para recuperação de senha por e-mail, configure `SMTP_HOST`, `SMTP_USERNAME`, `SMTP_PASSWORD` e demais opções conforme o provedor. Sem SMTP válido, o envio não estará operacional.
4. Inicialize a API, preferencialmente executando antes `./mvnw clean test` (Linux/macOS) ou `.\mvnw.cmd clean test` (Windows). Em seguida, execute `spring-boot:run`.
5. Abra `frontend/index.html` no seu servidor local de front-end (por exemplo Live Server) e use a URL da API definida em `frontend/js/api.js`.

## Testes manuais prioritários

- [ ] **Tela de login:** verifique se os ícones estão alinhados ao lado do texto em ambos os campos, no desktop e no celular.
- [ ] **Login/perfis:** autentique administrador e funcionário; teste senha incorreta e acesso restrito a endpoints administrativos (403).
- [ ] **CEP no cadastro:** digite um CEP válido e clique em **Buscar CEP**; confira rua/bairro/cidade e preenchimento manual quando a busca falhar.
- [ ] **CEP no pedido:** repita o teste; verifique que não precisa tirar o foco do campo para disparar a pesquisa.
- [ ] **Numeração:** crie um pedido, anote o número (por exemplo `Pedido 27`), edite-o, salve e confira se permanece `Pedido 27`.
- [ ] **Sequência:** crie outro pedido e confirme que recebeu o próximo número. Recarregue a página e reinicie a API: a contagem **não** pode voltar ao início.
- [ ] **Pedidos antigos:** confira se o histórico e os pedidos anteriores ao update receberam números comerciais (o valor inicial dependerá do banco existente).
- [ ] **Listagens e impressão:** compare o mesmo número na edição, nos pedidos em aberto, finalizados, no comprovante e no painel do administrador.
- [ ] **Preço:** teste pizza inteira/meio a meio, assada/pré-assada, acréscimo de R$ 5,00 e quantidade maior que 1.
- [ ] **Entrega/retirada:** confira taxa apenas para entrega, endereço e total, e pagamentos dinheiro, Pix, crédito e débito.
- [ ] **Correção administrativa:** finalize pedido, altere seu total pelo painel informando um motivo, confirme o novo total; tente fazer a mesma chamada com credencial de funcionário (403).
- [ ] **Auditoria:** confira ações de criação, alteração, finalização, correção de valor, exclusão e alterações de produtos/colaboradores. O histórico precisa identificar o número do pedido, responsável e data.
- [ ] **Exclusão administrativa:** exclua pedido de teste pelo painel, confirme que sumiu da lista e que o histórico continua mostrando `Pedido N` na ação de exclusão.
- [ ] **Postman e testes:** execute a coleção `docs/PI_Pizzaria_Sprint6_Postman.json` e a suíte JUnit.

## Limitações desta validação

Foi feita verificação estática de sintaxe dos arquivos Java e JavaScript. Não foi possível executar o `mvnw clean test` ou a aplicação Spring Boot conectada ao MongoDB neste ambiente porque o Maven Wrapper tentou baixar o Maven e o download não estava disponível. **Os testes de funcionamento e integração precisam ser executados no computador da equipe.**
