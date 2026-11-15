<p align="center">
  <img src="frontend/assets/logo-city-pizzas.png" alt="Logotipo City Pizzas" width="125">
</p>

<h1 align="center">City Pizzas</h1>
<p align="center"><strong>Sistema de Gestão de Pedidos para Pizzaria</strong></p>
<p align="center">Projeto Interdisciplinar — Desenvolvimento de Software Multiplataforma<br>FATEC Franca “Dr. Thomaz Novelino” · 3º semestre · 2026/2 · Grupo 02</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white" alt="Java 21">
  <img src="https://img.shields.io/badge/Spring%20Boot-4.1.1-6DB33F?logo=springboot&logoColor=white" alt="Spring Boot 4.1.1">
  <img src="https://img.shields.io/badge/MongoDB-NoSQL-47A248?logo=mongodb&logoColor=white" alt="MongoDB">
  <img src="https://img.shields.io/badge/Front--end-HTML%20%7C%20CSS%20%7C%20JS-222222" alt="Front-end HTML, CSS e JavaScript">
  <img src="https://img.shields.io/badge/Projeto-Acad%C3%AAmico-24557D" alt="Projeto acadêmico">
</p>

---

## Sumário

1. [Visão geral](#visão-geral)
2. [Problema e solução](#problema-e-solução)
3. [Funcionalidades](#funcionalidades)
4. [Perfis e permissões](#perfis-e-permissões)
5. [Tecnologias e arquitetura](#tecnologias-e-arquitetura)
6. [Estrutura do projeto](#estrutura-do-projeto)
7. [Instalação e execução](#instalação-e-execução)
8. [Configuração e variáveis de ambiente](#configuração-e-variáveis-de-ambiente)
9. [Regras de negócio](#regras-de-negócio)
10. [API REST](#api-rest)
11. [Banco de dados](#banco-de-dados)
12. [Segurança e auditoria](#segurança-e-auditoria)
13. [Sprints e organização da equipe](#sprints-e-organização-da-equipe)
14. [Testes e validação](#testes-e-validação)
15. [Documentação e apresentação do PI](#documentação-e-apresentação-do-pi)
16. [Limitações e próximos passos](#limitações-e-próximos-passos)
17. [Equipe](#equipe)

## Visão geral

O **City Pizzas** é uma aplicação web voltada à **operação interna de uma pizzaria**. Seu propósito é facilitar o atendimento, centralizar os cadastros, registrar pedidos e oferecer ao responsável pelo estabelecimento maior controle sobre operações administrativas.

O sistema foi desenvolvido como **Projeto Interdisciplinar (PI) do 3º semestre** do curso de **Desenvolvimento de Software Multiplataforma da FATEC Franca**. O tema acadêmico do semestre é a construção de uma solução web com **front-end, back-end e banco de dados não relacional integrados**.

| Informação | Descrição |
|---|---|
| Projeto | City Pizzas — Sistema de Gestão de Pedidos para Pizzaria |
| Modalidade | Aplicação web para uso interno |
| Instituição | FATEC Franca “Dr. Thomaz Novelino” |
| Curso | Desenvolvimento de Software Multiplataforma (DSM) |
| Período | 3º semestre — 2026/2 |
| Disciplina-chave | Gestão Ágil de Projetos |
| Gestão | Planejamento por Product Backlog e seis sprints |
| Público-alvo | Funcionários e administradores de pizzarias |
| Product Owner | João Pedro |

> **Escopo:** o foco é a operação interna. A criação de um aplicativo de delivery para o cliente final, com carrinho e acompanhamento externo, pertence ao backlog de evolução futura e **não** é requisito da primeira versão.

## Problema e solução

Em uma pizzaria, pedidos podem chegar por diferentes canais e envolver clientes recorrentes, vários sabores, bebidas, observações, taxas de entrega e formas de pagamento. Controles manuais ou descentralizados aumentam o risco de erros e dificultam a identificação de quem realizou cada operação.

O **City Pizzas** concentra esse fluxo em uma única aplicação, com:

- **Atendimento rápido:** cliente localizado, itens selecionados e opções padrão para reduzir cliques.
- **Regras automáticas:** cálculo de pizzas, acréscimos, quantidades e taxa de entrega no servidor.
- **Rastreabilidade:** vínculo do pedido ao colaborador autenticado e histórico das ações relevantes.
- **Gestão administrativa:** permissões exclusivas para colaboradores, correções, exclusões e auditoria.
- **Persistência:** clientes, produtos, pedidos e registros armazenados em MongoDB.

## Funcionalidades

### Acesso e colaboradores

- Login por **matrícula e senha**.
- Identificação dos perfis **funcionário** e **administrador**.
- Recuperação de senha por código enviado ao e-mail cadastrado, quando o serviço SMTP está configurado.
- Redefinição de senha de colaboradores pelo administrador.
- Cadastro, consulta, edição e exclusão de colaboradores por usuário autorizado.
- Encerramento de sessão.

### Clientes

- Cadastro com nome, telefone, CPF, e-mail, data de nascimento e endereço.
- Pesquisa de clientes por nome ou telefone.
- Seleção do cliente durante a montagem do pedido.
- Consulta de CEP pelo botão **Buscar CEP**, com possibilidade de correção ou digitação manual do endereço.

### Produtos e catálogo

- Cadastro e manutenção de pizzas, bebidas e outros produtos.
- Cadastro de categoria, preço, estoque e, quando aplicável, sabor ou apresentação/volume.
- Identificação de bebidas por tamanho ou embalagem (por exemplo, lata e garrafa de 2 litros).
- Pesquisa de produtos para adicioná-los ao pedido.
- Registro da autoria de operações relevantes no catálogo.

### Pedidos e atendimento

- Criação de pedido vinculado ao **cliente** e ao **colaborador autenticado**.
- Inclusão de vários itens, com suas quantidades e observações.
- Pizza **inteira** ou **meio a meio** (dois sabores diferentes).
- Preparo **assado** como padrão e alternativa **pré-assada**.
- Remoção de ingredientes sem acréscimo de preço.
- Inclusão de ingredientes adicionais com cobrança de **R$ 5,00 por adicional**.
- Pedido para **entrega** (padrão) ou **retirada**.
- Preenchimento de endereço e pesquisa de CEP para entrega.
- Pagamento em **dinheiro** (padrão), **Pix**, **crédito** ou **débito**.
- Cálculo de subtotal, taxa de entrega e total pelo back-end.
- Salvamento, edição de pedidos em aberto, finalização e opção de impressão no navegador.
- Consulta de pedidos abertos e finalizados.
- **Número comercial sequencial** (`Pedido 1`, `Pedido 2`, etc.) gerado no MongoDB e preservado ao editar ou finalizar.

### Administração e auditoria

- Painel administrativo com acesso restrito.
- Consulta, edição e exclusão administrativa de pedidos.
- Correção do **valor total** de um pedido com **motivo obrigatório**.
- Histórico de auditoria com usuário, ação, registro afetado, data/hora e detalhes.
- Pesquisa e filtro do histórico por termo e período.
- Exibição do número comercial do pedido nos registros de auditoria, quando disponível.

## Perfis e permissões

| Operação | Funcionário | Administrador |
|---|:---:|:---:|
| Login e recuperação da própria senha | Sim | Sim |
| Buscar e cadastrar clientes | Sim | Sim |
| Consultar e manter catálogo de produtos | Sim | Sim |
| Criar, editar pedidos em aberto e finalizar | Sim | Sim |
| Consultar pedidos e imprimir | Sim | Sim |
| Cadastrar e gerenciar colaboradores | Não | Sim |
| Redefinir senha de outro colaborador | Não | Sim |
| Acessar o painel administrativo | Não | Sim |
| Alterar ou excluir pedidos por função administrativa | Não | Sim |
| Corrigir valores de pedidos já registrados | Não | Sim |
| Consultar histórico de auditoria | Não | Sim |

As verificações de permissão das funções restritas são realizadas **no back-end**, não apenas ocultando botões no navegador.

## Tecnologias e arquitetura

| Camada | Tecnologia | Uso no projeto |
|---|---|---|
| Back-end | Java 21 | Regras de negócio e serviços |
| Framework | Spring Boot 4.1.1 / Spring Web MVC | API REST |
| Persistência | Spring Data MongoDB / MongoDB | Coleções e repositórios NoSQL |
| Validação e segurança | Spring Validation, BCrypt, interceptor de autorização | Validação, senhas e controle de acesso |
| E-mail | Spring Mail / SMTP | Recuperação de senha |
| Front-end | HTML5, CSS3 e JavaScript | Interface de operação |
| Integração | JSON, API REST e `fetch` | Comunicação entre navegador e servidor |
| CEP | ViaCEP | Consulta de endereço por CEP |
| Build e testes | Maven Wrapper, JUnit e Mockito | Execução e testes automatizados |
| Desenvolvimento | Git, GitHub, VS Code e Postman | Versionamento e validação da API |

### Fluxo da aplicação

```mermaid
flowchart LR
    U[Funcionário ou administrador] --> F[Front-end HTML / CSS / JavaScript]
    F -->|HTTP / JSON + Bearer Token| A[API Spring Boot]
    A --> S[Controllers / Services / Repositories]
    S --> M[(MongoDB)]
    F -->|Consulta de CEP| C[ViaCEP]
    A -->|Recuperação de senha| E[Servidor SMTP]
```

A organização do back-end segue a separação de responsabilidades entre **controllers**, **services**, **repositories**, **models**, **DTOs**, **configurações** e **tratamento de exceções**. O front-end é servido separadamente durante o desenvolvimento, normalmente com **Live Server**.

## Estrutura do projeto

```text
.
├── frontend/
│   ├── index.html                  # Login e recuperação de senha
│   ├── pedidos.html                # Novo pedido, edição e finalização
│   ├── cadastros.html              # Clientes e produtos
│   ├── historico.html              # Pedidos abertos, finalizados e impressão local
│   ├── admin.html                  # Colaboradores, pedidos e auditoria
│   ├── assets/                     # Identidade visual e imagens
│   ├── css/style.css               # Estilos compartilhados
│   └── js/                         # Comunicação com API e lógica das telas
├── src/
│   ├── main/
│   │   ├── java/PI/Pizzaria/
│   │   │   ├── config/             # CORS, autorização, admin inicial, migração
│   │   │   ├── controller/         # Endpoints REST
│   │   │   ├── dto/                # Objetos de entrada/saída da API
│   │   │   ├── exception/          # Tratamento de erros
│   │   │   ├── model/              # Entidades/documentos
│   │   │   ├── repository/         # Persistência MongoDB
│   │   │   ├── service/            # Regras de negócio
│   │   │   └── PizzariaApplication.java
│   │   └── resources/application.properties
│   └── test/java/PI/Pizzaria/      # Testes automatizados
├── diagrama/db/                    # Diagrama NoAM (draw.io e SVG)
├── docs/                           # Roteiro de testes, Postman e revisão
├── documentosProfFausto/          # Memorial do projeto
├── pom.xml                         # Dependências Maven
├── mvnw / mvnw.cmd                 # Maven Wrapper
└── README.md
```

## Instalação e execução

### Pré-requisitos

- **JDK 21** instalado e disponível no terminal (`java -version`).
- **MongoDB** local ou acesso válido a um cluster MongoDB Atlas.
- Navegador web atualizado.
- **VS Code com Live Server** ou outro servidor HTTP local para o front-end.
- Conexão de internet para baixar dependências Maven na primeira execução.
- Opcional: **Postman** para testar a API e **MongoDB Compass** para inspecionar as coleções.

### 1. Preparar o repositório

Abra a pasta do projeto no VS Code e, no terminal, entre no diretório que contém o `pom.xml`.

Se estiver usando Git, faça as alterações em uma branch própria. Antes de trocar para esta versão em uma base já utilizada, **faça backup do banco de dados**: há uma rotina que atribui números a pedidos antigos que ainda não possuam numeração comercial.

### 2. Configurar o banco e os preços

No **Windows (PowerShell)**, na pasta que contém o `pom.xml`:

```powershell
# MongoDB local (troque pela URI do Atlas, caso utilize Atlas)
$env:MONGODB_URI = "mongodb://localhost:27017/pizzaria"

# Valores de EXEMPLO para testes; confirmar preços reais com a pizzaria
$env:PIZZA_PRECO_BASE = "60.00"
$env:PIZZA_PRECO_PRE_ASSADA = "50.00"
$env:TAXA_ENTREGA = "7.00"
```

Caso já exista um MongoDB local no endereço `mongodb://localhost:27017/pizzaria`, a variável `MONGODB_URI` pode ser omitida. A URI, os preços e outras opções também podem ser ajustados em `src/main/resources/application.properties`.

### 3. Criar o primeiro administrador (apenas em banco novo)

Se a coleção de colaboradores estiver vazia, configure os dados iniciais **antes da primeira execução**:

```powershell
$env:BOOTSTRAP_ADMIN_NOME = "Administrador"
$env:BOOTSTRAP_ADMIN_MATRICULA = "MATRICULA_DEFINIDA_PELA_EQUIPE"
$env:BOOTSTRAP_ADMIN_EMAIL = "email.real@exemplo.com"
$env:BOOTSTRAP_ADMIN_SENHA = "DEFINA_UMA_SENHA_FORTE_AQUI"
```

A rotina de inicialização cria esse administrador **somente quando ainda não há colaboradores no banco**. Se a base já possui um administrador, utilize a matrícula e a senha existentes. **Nunca publique senhas reais em commits, prints ou arquivos versionados.**

### 4. Executar o back-end

**Windows:**

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

**Linux/macOS:**

```bash
./mvnw clean test
./mvnw spring-boot:run
```

A API é acessada, por padrão, em **`http://localhost:8080`**. Mantenha o processo do Spring Boot ativo durante o uso do front-end. Se a compilação ou os testes falharem, corrija o erro antes de considerar a aplicação validada.

### 5. Executar o front-end

Abra `frontend/index.html` com o **Live Server**. Dependendo da pasta usada como raiz, o endereço será semelhante a:

```text
http://127.0.0.1:5500/frontend/index.html
```

O endereço do servidor usado nas requisições está definido em `frontend/js/api.js`:

```javascript
const API_BASE_URL = 'http://localhost:8080';
```

Se executar a API em outra porta ou endereço, atualize essa constante. Se executar o front-end a partir de outra origem, confira também a configuração de CORS.

### 6. Primeiro acesso

1. Entre com a matrícula e a senha do administrador cadastrado.
2. Cadastre os colaboradores que utilizarão o sistema.
3. Cadastre produtos (pizzas, bebidas e outros itens).
4. Cadastre ou selecione um cliente.
5. Crie um pedido, confira os valores, salve e finalize.
6. Valide o resultado nas telas de pedidos anteriores e, como administrador, na auditoria.

## Configuração e variáveis de ambiente

A configuração principal está em `src/main/resources/application.properties`. As variáveis de ambiente têm preferência sobre os valores de fallback configurados no arquivo.

| Variável | Finalidade | Padrão do pacote analisado |
|---|---|---|
| `MONGODB_URI` | Conexão com MongoDB | `mongodb://localhost:27017/pizzaria` |
| `PIZZA_PRECO_BASE` | Pizza assada (inteira ou meio a meio) | `60.00` * |
| `PIZZA_PRECO_PRE_ASSADA` | Pizza pré-assada | `46.59` * |
| `TAXA_ENTREGA` | Taxa fixa para entregas | `10.00` * |
| `BOOTSTRAP_ADMIN_NOME` | Nome do administrador inicial | Vazio |
| `BOOTSTRAP_ADMIN_MATRICULA` | Matrícula do administrador inicial | Vazio |
| `BOOTSTRAP_ADMIN_EMAIL` | E-mail do administrador inicial | Vazio |
| `BOOTSTRAP_ADMIN_SENHA` | Senha do administrador inicial | Vazio |
| `CORS_ALLOWED_ORIGIN_PATTERNS` | Origens permitidas pelo back-end | `http://localhost:*,http://127.0.0.1:*` |
| `SMTP_HOST`, `SMTP_PORT` | Servidor e porta de e-mail | Host vazio, porta `587` |
| `SMTP_USERNAME`, `SMTP_PASSWORD` | Autenticação SMTP | Vazios |
| `SMTP_FROM` | Remetente dos e-mails | `SMTP_USERNAME`, quando definido |
| `SMTP_AUTH`, `SMTP_STARTTLS`, `SMTP_SSL` | Opções SMTP | `true`, `true`, `false` |

\* **Os preços 60.00, 46.59 e 10.00 são parâmetros de exemplo do pacote de desenvolvimento, não valores comerciais aprovados pela pizzaria.** Confirme-os com o cliente antes do uso real. No código, valores monetários devem utilizar **ponto decimal**, por exemplo `60.00`.

### Recuperação de senha por e-mail

Para testar envio real, configure um servidor SMTP válido. Exemplo de configuração para o provedor que aceite SMTP autenticado:

```powershell
$env:SMTP_HOST = "smtp.exemplo.com"
$env:SMTP_PORT = "587"
$env:SMTP_USERNAME = "conta.remetente@exemplo.com"
$env:SMTP_PASSWORD = "SENHA_SMTP_OU_SENHA_DE_APLICATIVO"
$env:SMTP_FROM = "conta.remetente@exemplo.com"
$env:SMTP_AUTH = "true"
$env:SMTP_STARTTLS = "true"
$env:SMTP_SSL = "false"
```

A recuperação gera um **código de seis dígitos**, válido por **15 minutos**, com limite de **cinco tentativas**. O código fica protegido por hash no banco e não é retornado pela API. **Sem SMTP configurado, a recuperação por e-mail não ficará operacional.**

## Regras de negócio

| Regra | Comportamento esperado |
|---|---|
| Pizza inteira/meio a meio | O número de sabores não altera o preço-base; meio a meio exige dois sabores distintos. |
| Tipo de preparo | `ASSADA` por padrão; `PRE_ASSADA` como alternativa. |
| Preço da pizza | Definido pela configuração do preparo no servidor, não pelo valor enviado pelo navegador. |
| Ingredientes removidos | Registrados na observação, sem desconto ou acréscimo. |
| Ingredientes adicionados | **R$ 5,00** por ingrediente adicional, considerando a quantidade do item. |
| Modalidade | `ENTREGA` por padrão; `RETIRADA` como alternativa. |
| Taxa | Aplicada em entregas; **zero para retirada**. |
| Pagamento | `DINHEIRO` por padrão; também `PIX`, `CREDITO` e `DEBITO`. |
| Total | Calculado pelo servidor considerando itens, quantidade, adicionais e taxa aplicável. |
| Situação do pedido | Inicia em `EM_ABERTO` e, após confirmação, passa a `FINALIZADO`. |
| Edição comum | Pedidos já finalizados não podem ser alterados pelo fluxo operacional comum. |
| Administração | Alteração, exclusão e correção de valor com autorização exclusiva; correção exige motivo. |
| Numeração | Número legível atribuído pelo MongoDB; editar e finalizar não alteram o número. |

A numeração comercial (`numero`) é distinta do **`id` técnico do documento MongoDB**. O ID interno continua sendo utilizado pela API, enquanto as telas mostram o número amigável, como **Pedido 6**.

## API REST

A aplicação fornece endpoints JSON sob `http://localhost:8080`. Exceto login, recuperação de senha e os demais caminhos públicos definidos pela aplicação, as rotas operacionais exigem autenticação.

### Autenticação

| Método | Endpoint | Finalidade |
|---|---|---|
| `POST` | `/auth/login` | Autenticar por matrícula e senha |
| `POST` | `/auth/recuperar-senha` | Solicitar código de recuperação por e-mail |
| `POST` | `/auth/redefinir-senha` | Redefinir senha com código válido |
| `POST` | `/auth/admin/redefinir-senha` | Redefinir senha de colaborador (admin) |
| `POST` | `/auth/logout` | Encerrar sessão autenticada |

Exemplo de login:

```http
POST /auth/login HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{
  "matricula": "MATRICULA_EXISTENTE",
  "senha": "SENHA_EXISTENTE"
}
```

Utilize o campo `token` da resposta nas requisições protegidas:

```http
Authorization: Bearer TOKEN_RETORNADO_NO_LOGIN
```

### Clientes, produtos e colaboradores

| Método | Endpoint | Finalidade |
|---|---|---|
| `POST` | `/clientes` | Cadastrar cliente |
| `GET` | `/clientes` | Listar clientes |
| `GET` | `/clientes/busca?termo=...` | Buscar por nome/telefone |
| `GET` / `PUT` | `/clientes/{id}` | Consultar/atualizar cliente |
| `POST` | `/produtos` | Cadastrar produto |
| `GET` | `/produtos` | Listar catálogo |
| `GET` | `/produtos/buscar?nome=...` | Pesquisar produto |
| `GET` | `/produtos/categoria/{categoria}` | Filtrar categoria |
| `GET` / `PUT` / `DELETE` | `/produtos/{id}` | Consultar/atualizar/excluir produto |
| `POST` / `GET` | `/colaboradores` | Cadastrar/listar colaboradores (admin) |
| `GET` / `PUT` / `DELETE` | `/colaboradores/{id}` | Consultar/alterar/excluir colaborador (admin) |
| `GET` | `/api/cep/{cep}` | Consultar CEP via back-end |

O código também mantém as rotas de leitura `/pizzas` e `/pizzas/{id}`; a interface principal utiliza o catálogo em `/produtos`.

### Pedidos

| Método | Endpoint | Finalidade |
|---|---|---|
| `POST` | `/pedidos` | Criar pedido |
| `GET` | `/pedidos` | Listar todos os pedidos |
| `GET` | `/pedidos/{id}` | Consultar pedido pelo ID técnico |
| `PUT` | `/pedidos/{id}` | Editar pedido em aberto |
| `POST` | `/pedidos/{id}/finalizar` | Finalizar pedido |
| `GET` | `/pedidos/abertos?termo=...` | Buscar pedidos abertos |
| `GET` | `/pedidos/finalizados?termo=...` | Buscar pedidos finalizados |
| `GET` | `/pedidos/precos` | Consultar preços configurados das pizzas |
| `GET` | `/pedidos/taxa-entrega` | Consultar taxa configurada |
| `PUT` | `/pedidos/admin/{id}` | Alterar pedido como administrador |
| `DELETE` | `/pedidos/admin/{id}` | Excluir pedido como administrador |
| `PUT` | `/pedidos/admin/{id}/corrigir-valor` | Corrigir total com motivo (admin) |
| `GET` | `/auditoria?termo=...&de=AAAA-MM-DD&ate=AAAA-MM-DD` | Consultar histórico (admin) |

Exemplo de pedido para **retirada**, após cadastrar um cliente e um produto:

```json
{
  "clienteId": "ID_REAL_DO_CLIENTE",
  "modalidade": "RETIRADA",
  "formaPagamento": "PIX",
  "telefoneContato": "16999999999",
  "nomeRetirada": "Cliente de teste",
  "itens": [
    {
      "produtoId": "ID_REAL_DA_PIZZA",
      "quantidade": 1,
      "meioAMeio": false,
      "tipoPreparo": "ASSADA",
      "acrescimos": ["Borda recheada"],
      "ingredientesRemovidos": ["Cebola"],
      "observacao": "Sem cebola"
    }
  ]
}
```

O `clienteId` e o `produtoId` precisam existir. Para **entrega**, informe `modalidade: "ENTREGA"` e um `enderecoEntrega` válido, conforme as regras de validação da API. **Não envie `total` ou `taxaEntrega` como valores confiáveis no JSON:** esses dados são calculados no servidor.

Exemplo de correção administrativa:

```json
{
  "novoValorTotal": 90.00,
  "motivo": "Correção de valor confirmada pelo administrador"
}
```

### Respostas e erros

A API utiliza códigos HTTP como `201` (criação), `400` (dados inválidos), `401` (não autenticado), `403` (sem permissão), `404` (registro ausente), `409` (conflito de estado) e `503` (dependência/configuração indisponível), conforme a operação. As mensagens de erro buscam orientar o usuário sem expor detalhes sensíveis.

## Banco de dados

A persistência é implementada com **MongoDB**, em documentos organizados em coleções, por exemplo:

| Coleção | Papel |
|---|---|
| `colaboradores` | Matrículas, perfis, credenciais protegidas e recuperação de senha |
| `clientes` | Dados cadastrais e endereço |
| `produtos` | Catálogo, preços, estoque, categoria e variações |
| `pedidos` | Cliente, atendente, itens, modalidade, pagamento, totais, status e número |
| `sequencias` | Controle persistente da numeração comercial |
| `auditorias` | Log de ações relevantes, usuário, registro e data/hora |
| `auditoria_produtos` | Histórico específico de operações do catálogo |
| `pizzas` | Estrutura de pizza mantida no projeto, distinta do catálogo principal |

O diagrama de modelagem NoSQL está disponível em:

- [Modelo NoAM — visualização](diagrama/db/NoAM.svg)
- [Modelo NoAM — arquivo editável](diagrama/db/NoAM.drawio)

### Migração de numeração de pedidos antigos

Na versão revisada, uma rotina executada na inicialização atribui números sequenciais aos pedidos antigos que não possuam `numero`. Ela preserva os IDs técnicos e os dados comerciais dos pedidos e mantém a sequência em coleção própria.

**Atenção:** faça backup do MongoDB antes da primeira execução sobre um banco com dados existentes. A migração está habilitada por padrão no código; em situações de diagnóstico, pode ser desativada definindo `app.pedido.migracao-habilitada=false` na configuração da aplicação. A reexecução deve ser avaliada cuidadosamente caso existam dados legados ou múltiplas instâncias simultâneas.

## Segurança e auditoria

O projeto implementa medidas de proteção adequadas ao contexto acadêmico da aplicação:

- **Hash BCrypt** para armazenamento de senhas.
- **Token de sessão** enviado no cabeçalho `Authorization: Bearer ...`.
- Sessões com duração de aproximadamente **8 horas**, mantidas **em memória** no servidor (não são JWTs).
- Consulta ao banco para revalidar o perfil do colaborador autenticado.
- **Interceptor no back-end** para bloquear operações administrativas não autorizadas.
- Código de recuperação protegido por hash, expiração e limite de tentativas.
- Registro de ações relevantes de pedidos, produtos e colaboradores.
- Restrição de consulta do histórico de auditoria ao perfil administrador.

**Importante para operação:** como as sessões ficam em memória, reiniciar a API invalida os tokens existentes. O projeto ainda requer revisão de infraestrutura e segurança antes de qualquer publicação em produção aberta na internet (HTTPS, armazenamento de segredos, políticas de acesso, backups, monitoramento, proteção dos dados pessoais etc.).

## Sprints e organização da equipe

O planejamento acadêmico divide o desenvolvimento em **seis sprints**, alinhadas ao Product Backlog `PB01` a `PB24`. Os itens `PB25` a `PB27` representam evolução futura (delivery externo).

| Sprint | Objetivo | Entregas previstas |
|---|---|---|
| **1 — Base técnica** | Preparar ambiente e estrutura | Spring Boot, repositório GitHub, NoSQL, front-end inicial e integração básica |
| **2 — Login e cadastros** | Controlar acesso e identificar clientes | Autenticação, perfis, colaboradores, recuperação de senha, busca/cadastro de clientes |
| **3 — Catálogo e pedidos** | Construir o núcleo do atendimento | Produtos, pizzas, pedido com itens, sabores, preparo, observações e acréscimos |
| **4 — Finalização** | Completar o fluxo comercial | Entrega/retirada, CEP, taxa, pagamento, cálculo, finalização e impressão |
| **5 — Administração** | Garantir controle e rastreabilidade | Gestão administrativa de pedidos, colaboradores, segurança e auditoria |
| **6 — Estabilização** | Integrar, testar e preparar demonstração | Correções, testes de API, revisão de requisitos e entrega acadêmica |

### Papéis da equipe

| Integrante | Papel principal | Responsabilidades no planejamento |
|---|---|---|
| **João Pedro** | Product Owner e Back-end | Backlog, regras de negócio, autenticação, fluxo de pedidos, precificação, permissões e validação |
| **Ronie** | Back-end | Endpoints REST, catálogo, clientes, integrações, operações administrativas, CEP e apoio aos testes |
| **Maria Eduarda (Duda)** | Banco de Dados | Modelagem NoSQL, coleções, índices, persistência, vínculos, integridade e logs |
| **Cauã** | Front-end | Interface, experiência de uso, telas de pedidos/cadastros, integração com API e painel administrativo |

A distribuição acima representa o **planejamento de responsabilidades**; a autoria efetiva de cada alteração deve ser conferida no histórico de commits do GitHub.

## Testes e validação

### Testes automatizados

Na raiz do projeto, execute:

```powershell
.\mvnw.cmd clean test
```

Há arquivos de testes em `src/test/java/PI/Pizzaria/` que abrangem serviços de clientes, produtos, pedidos, sessão, e-mail, auditoria, validação de DTOs e regras de autorização. Os cenários devem ser executados no ambiente da equipe e seus resultados conferidos no relatório do Maven.

Para testar manualmente os endpoints, importe a coleção:

[**Postman — Sprint 6**](docs/PI_Pizzaria_Sprint6_Postman.json)

### Checklist de testes manuais

- [ ] Login com funcionário e administrador; senha incorreta; logout.
- [ ] Acesso negado (`403`) às rotas administrativas com token de funcionário.
- [ ] Cadastro e busca de clientes; validação de CPF/telefone e campos obrigatórios.
- [ ] **Buscar CEP** no cadastro e no pedido; CEP inválido e edição manual.
- [ ] Cadastro, pesquisa e edição de pizzas, bebidas e outros produtos.
- [ ] Criação de pedido com vários itens e vínculo ao colaborador autenticado.
- [ ] Pizza inteira/meio a meio, assada/pré-assada, quantidade e adicionais.
- [ ] Entrega com taxa e retirada sem taxa; pagamentos e total calculado.
- [ ] Edição de pedido aberto, finalização e impressão pelo navegador.
- [ ] Número comercial consistente em edição, listagens, impressão e auditoria.
- [ ] Sequência dos pedidos preservada após reiniciar a aplicação.
- [ ] Correção administrativa de valor com justificativa; exclusão de pedido de teste.
- [ ] Auditoria de criação, alteração, finalização, exclusão e gestão de cadastros.
- [ ] Recuperação de senha com **e-mail real** (se SMTP estiver configurado).

Roteiro detalhado: [**docs/ENTREGA_FINAL_TESTES.md**](docs/ENTREGA_FINAL_TESTES.md).

> **Estado da validação:** o pacote de código foi revisado estaticamente (sintaxe Java/JavaScript). A execução integral do Maven, as operações reais com MongoDB, o envio SMTP e os testes de ponta a ponta **não foram comprovados neste ambiente**. Não interprete a existência dos testes ou o checklist como homologação de 100% das funcionalidades.

## Documentação e apresentação do PI

O projeto está associado aos seguintes artefatos de planejamento:

1. **Documento 01 — Visão do Produto e Escopo**: problema, objetivo, público-alvo, limites e critérios de sucesso.
2. **Documento 02 — Levantamento de Requisitos**: requisitos funcionais e não funcionais.
3. **Documento 03 — Product Backlog**: histórias, prioridades, critérios de aceite e dependências.
4. **Documento 04 — Planejamento de Sprints**: entregas, responsáveis e organização semanal.

Esses quatro documentos foram fornecidos pela equipe para a elaboração deste README, mas **não estão incluídos no ZIP de código usado como referência**; adicione-os ao repositório, em uma pasta de documentação, caso ainda não tenham sido versionados.

Outros materiais existentes neste projeto:

- [Memorial do projeto (PDF)](documentosProfFausto/MemorialProjeto_Pizzaria.pdf)
- [Modelo do banco NoAM](diagrama/db/NoAM.svg)
- [Coleção de testes Postman](docs/PI_Pizzaria_Sprint6_Postman.json)
- [Relatório de revisão das Sprints 1 a 6](docs/REVISAO_JOAO_SPRINTS_1_A_6.md)
- [Roteiro de validação final](docs/ENTREGA_FINAL_TESTES.md)

### Demonstração do projeto

**Vídeo no YouTube:** *link a inserir pela equipe antes da entrega final*.

Conforme o manual do PI da FATEC Franca (versão 4/2026), o vídeo deve ter **até cinco minutos**, estar **público no YouTube**, contar com a **fala de todos os integrantes** e priorizar a **demonstração prática do software**. Slides são opcionais. O link do vídeo deve aparecer no GitHub juntamente com a documentação.

**Repositório oficial:** *adicionar aqui o endereço do projeto na organização [FatecFranca](https://github.com/FatecFranca)*.

A entrega institucional é realizada por um integrante do grupo mediante formulário com o **link do GitHub e o link do YouTube**. O prazo deve ser confirmado no calendário acadêmico vigente e nas orientações dos professores.

## Limitações e próximos passos

O City Pizzas é um **projeto acadêmico em evolução**, não uma plataforma comercial homologada. Antes do uso real ou da publicação, recomenda-se:

- Executar a suíte de testes e validar todos os fluxos com MongoDB e credenciais de teste.
- Confirmar com a pizzaria os preços-base, preparo, taxa de entrega e regras operacionais.
- Validar o envio SMTP real e a disponibilidade do serviço de CEP.
- Fazer backup da base antes de migrações e definir rotina de recuperação de dados.
- Revisar privacidade, autorização, tempo de sessão e políticas de retenção dos logs.
- Preparar HTTPS, publicação segura e configuração de segredos para o ambiente de produção.
- Documentar resultados finais de testes e disponibilizar o vídeo de apresentação.

A futura área para o **cliente realizar pedidos pela internet**, com carrinho, recomendações e acompanhamento externo, é uma possibilidade de evolução, **fora do escopo obrigatório desta entrega**.

## Equipe

Desenvolvido pelo **Grupo 02 — PI do 3º semestre de DSM, FATEC Franca (2026/2)**:

- **João Pedro** — Product Owner e Back-end.
- **Ronie** — Back-end.
- **Maria Eduarda** — Banco de Dados.
- **Cauã** — Front-end.

---

<p align="center"><strong>City Pizzas</strong><br>Projeto Interdisciplinar · FATEC Franca · Desenvolvimento de Software Multiplataforma</p>
