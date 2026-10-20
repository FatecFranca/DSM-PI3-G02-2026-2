/**
 * api.js — ponto único de integração com a API REST (Spring Boot).
 *
 * Contrato REAL (conferido no código do back-end):
 *   POST /auth/login             { matricula, senha }  -> { matricula, perfil }   (401 + texto se errar)
 *   POST /auth/recuperar-senha   { email }             -> texto com o código
 *   POST /auth/redefinir-senha   { email, codigo, novaSenha } -> texto
 *   GET  /pizzas                 -> [{ id, nome, sabor, tamanho, preco }]
 *   POST /colaboradores          { nome, matricula, email, senha, perfil }
 *
 * Contrato PROPOSTO (back-end ainda não tem ClienteController):
 *   POST /clientes                        { nome, telefone, cpf, email?, endereco?, dataNascimento? }
 *   GET  /clientes/busca?nome=...         -> [{ id, nome, telefone, ... }]
 *   GET  /clientes/busca?telefone=...     -> [{ id, nome, telefone, ... }]
 */

const API_BASE_URL = 'http://localhost:8080';

/**
 * Requisição genérica. O back-end hoje devolve erros como TEXTO PURO
 * (ex.: 401 "Matricula ou Senha incorreta"), não JSON — por isso o corpo
 * é lido como texto e só convertido em JSON quando for possível.
 */
async function requisitar(caminho, opcoes = {}) {
  const cabecalhos = {
    'Content-Type': 'application/json',
    ...(opcoes.headers || {}),
  };

  // Quando o back-end passar a emitir token (JWT), ele já segue no header.
  const token = localStorageObterToken();
  if (token) {
    cabecalhos['Authorization'] = `Bearer ${token}`;
  }

  let resposta;
  try {
    resposta = await fetch(`${API_BASE_URL}${caminho}`, { ...opcoes, headers: cabecalhos });
  } catch (erroDeRede) {
    throw new ErroApi('Não foi possível conectar ao servidor. Verifique se o back-end está rodando.', 0, null);
  }

  const texto = await resposta.text();
  let corpo = texto;
  try {
    corpo = texto ? JSON.parse(texto) : null;
  } catch {
    corpo = texto; // resposta em texto puro
  }

  if (!resposta.ok) {
    throw new ErroApi(extrairMensagemDeErro(corpo, resposta.status), resposta.status, corpo);
  }

  return corpo;
}

function extrairMensagemDeErro(corpo, status) {
  if (corpo && typeof corpo === 'object') {
    // { mensagem } (formato combinado) ou { message } (erro padrão do Spring)
    const mensagem = corpo.mensagem || corpo.message;
    if (mensagem) return mensagem;
  }
  if (typeof corpo === 'string' && corpo.trim() && !corpo.trim().startsWith('<')) {
    return corpo.trim();
  }
  if (status === 401) return 'Matrícula ou senha incorreta.';
  if (status === 404) return 'Recurso não encontrado no servidor.';
  if (status >= 500) return 'Erro interno no servidor. Tente novamente ou avise a equipe de back-end.';
  return 'Ocorreu um erro inesperado.';
}

class ErroApi extends Error {
  constructor(mensagem, status, corpo) {
    super(mensagem);
    this.status = status;
    this.corpo = corpo;
  }
}

/* ---------- Autenticação ---------- */

/** POST /auth/login -> { matricula, perfil } ("administrador" | "funcionario") */
function login(matricula, senha) {
  return requisitar('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ matricula, senha }),
  });
}

/** POST /auth/recuperar-senha — o back-end usa E-MAIL (não matrícula) e responde com texto. */
function solicitarRecuperacaoSenha(email) {
  return requisitar('/auth/recuperar-senha', {
    method: 'POST',
    body: JSON.stringify({ email }),
  });
}

/** POST /auth/redefinir-senha */
function redefinirSenha(email, codigo, novaSenha) {
  return requisitar('/auth/redefinir-senha', {
    method: 'POST',
    body: JSON.stringify({ email, codigo, novaSenha }),
  });
}

/* ---------- Clientes ---------- */

/**
 * Busca por nome OU telefone (Documento 01). Se o termo for só números
 * (e tiver pelo menos 8 dígitos), busca por telefone; senão, por nome.
 * Telefones são guardados/buscados SOMENTE COM DÍGITOS (busca é exata no back-end).
 * ROTA PROPOSTA — confirmar com o Ronie.
 */
function buscarClientes(termo) {
  const digitos = somenteDigitos(termo);
  const ehTelefone = digitos.length >= 8 && digitos.length === termo.replace(/[\s()+-]/g, '').length;
  const consulta = ehTelefone
    ? `telefone=${encodeURIComponent(digitos)}`
    : `nome=${encodeURIComponent(termo.trim())}`;
  return requisitar(`/clientes/busca?${consulta}`, { method: 'GET' });
}

/**
 * POST /clientes — campos conforme model Cliente.java:
 *   obrigatórios: nome, telefone, cpf | opcionais: email, endereco (TEXTO único), dataNascimento (yyyy-MM-dd)
 */
function cadastrarCliente(dadosCliente) {
  return requisitar('/clientes', {
    method: 'POST',
    body: JSON.stringify(dadosCliente),
  });
}

/* ---------- Colaboradores (restrito a administrador) ---------- */

/** POST /colaboradores { nome, matricula, email, senha, perfil: "administrador" | "funcionario" } */
function cadastrarColaborador(dadosColaborador) {
  return requisitar('/colaboradores', {
    method: 'POST',
    body: JSON.stringify(dadosColaborador),
  });
}

/* ---------- Produtos ---------- */

/** GET /pizzas -> [{ id, nome, sabor, tamanho, preco }] */
function listarProdutos() {
  return requisitar('/pizzas', { method: 'GET' });
}

/* ---------- CEP (ViaCEP, direto do navegador) ---------- */

async function consultarCep(cep) {
  const cepLimpo = cep.replace(/\D/g, '');
  if (cepLimpo.length !== 8) return null;

  try {
    const resposta = await fetch(`https://viacep.com.br/ws/${cepLimpo}/json/`);
    const dados = await resposta.json();
    if (dados.erro) return null;
    return { rua: dados.logradouro, bairro: dados.bairro, cidade: dados.localidade };
  } catch {
    return null;
  }
}

/* ---------- Pedidos (ainda sem endpoint no back-end) ---------- */

function criarPedido(dadosPedido) {
  return requisitar('/pedidos', { method: 'POST', body: JSON.stringify(dadosPedido) });
}

function cancelarPedido(pedidoId) {
  return requisitar(`/pedidos/${pedidoId}`, { method: 'DELETE' });
}

/* ---------- Sessão local ---------- */
/*
 * O back-end ainda NÃO emite token: o login devolve só { matricula, perfil }.
 * Por isso "logado" = existe usuário salvo. Se virar JWT, o token também é guardado.
 */

function localStorageSalvarSessao({ matricula, perfil, nome, token, modoTeste }) {
  if (token) localStorage.setItem('cityPizzas_token', token);
  localStorage.setItem(
    'cityPizzas_usuario',
    JSON.stringify({ matricula, perfil, nome, modoTeste: Boolean(modoTeste) })
  );
}

function localStorageObterToken() {
  return localStorage.getItem('cityPizzas_token');
}

function localStorageObterUsuario() {
  const bruto = localStorage.getItem('cityPizzas_usuario');
  return bruto ? JSON.parse(bruto) : null;
}

function localStorageEncerrarSessao() {
  localStorage.removeItem('cityPizzas_token');
  localStorage.removeItem('cityPizzas_usuario');
}

function usuarioEstaLogado() {
  return Boolean(localStorageObterUsuario());
}

function usuarioEhAdministrador() {
  return localStorageObterUsuario()?.perfil === 'administrador';
}

/** Sessão criada pelo link "modo de teste" — só nela é permitido usar dados de exemplo. */
function emModoTeste() {
  return Boolean(localStorageObterUsuario()?.modoTeste);
}

/** Nome para exibir (o login ainda não devolve nome, então cai para a matrícula). */
function nomeDoUsuarioLogado() {
  const usuario = localStorageObterUsuario();
  return usuario?.nome || usuario?.matricula || 'Usuário';
}

/* ---------- Utilidades de formato ---------- */

function somenteDigitos(valor) {
  return String(valor || '').replace(/\D/g, '');
}

function formatarTelefone(valor) {
  const d = somenteDigitos(valor);
  if (d.length === 11) return `(${d.slice(0, 2)}) ${d.slice(2, 7)}-${d.slice(7)}`;
  if (d.length === 10) return `(${d.slice(0, 2)}) ${d.slice(2, 6)}-${d.slice(6)}`;
  return valor || '';
}

function validarCpf(valor) {
  const cpf = somenteDigitos(valor);
  if (cpf.length !== 11 || /^(\d)\1{10}$/.test(cpf)) return false;
  const digito = (base) => {
    let soma = 0;
    for (let i = 0; i < base; i++) soma += Number(cpf[i]) * (base + 1 - i);
    const resto = (soma * 10) % 11;
    return resto === 10 ? 0 : resto;
  };
  return digito(9) === Number(cpf[9]) && digito(10) === Number(cpf[10]);
}
