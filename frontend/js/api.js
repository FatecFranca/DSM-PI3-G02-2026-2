/**
 * api.js — ponto único de integração com a API REST (Spring Boot).
 *
 * Contrato Sprint 2: sessão no back-end e autorização por token.
 *   POST /auth/login             { matricula, senha }  -> { matricula, perfil }   (401 + texto se errar)
 *   POST /auth/recuperar-senha   { email }             -> texto com o código
 *   POST /auth/redefinir-senha   { email, codigo, novaSenha } -> texto
 *   GET  /pizzas                 -> [{ id, nome, sabor, tamanho, preco }]
 *   POST /colaboradores          { nome, matricula, email, senha, perfil }
 *
 *   GET  /clientes/busca?termo=... -> [{ id, nome, telefone, endereco, ... }]
 *   POST /clientes                    -> cliente cadastrado
 *   GET  /colaboradores               -> lista (somente administrador)
 *   POST /auth/admin/redefinir-senha  -> redefinição pelo administrador
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

  // Token opaco gerado pelo back-end; não é um JWT.
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

/** GET /clientes/busca?termo=... — pesquisa parcial de nome ou telefone. */
function buscarClientes(termo) {
  return requisitar(`/clientes/busca?termo=${encodeURIComponent(termo.trim())}`, { method: 'GET' });
}

/** POST /clientes — endereço é um objeto (rua, número, bairro, cidade, CEP). */
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

function listarColaboradores() {
  return requisitar('/colaboradores', { method: 'GET' });
}

function redefinirSenhaPeloAdministrador(colaboradorId, novaSenha) {
  return requisitar('/auth/admin/redefinir-senha', {
    method: 'POST',
    body: JSON.stringify({ colaboradorId, novaSenha }),
  });
}

function encerrarSessaoNoServidor() {
  return requisitar('/auth/logout', { method: 'POST' });
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
 * A sessão só é considerada válida no front-end com token; o back-end confirma
 * o token e as permissões a cada requisição protegida.
 */

function localStorageSalvarSessao({ matricula, perfil, nome, token }) {
  if (!token || !matricula || !perfil) throw new Error('Sessão inválida recebida do servidor.');
  localStorage.setItem('cityPizzas_token', token);
  localStorage.setItem('cityPizzas_usuario', JSON.stringify({ matricula, perfil, nome }));
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
  return Boolean(localStorageObterUsuario() && localStorageObterToken());
}

function usuarioEhAdministrador() {
  return localStorageObterUsuario()?.perfil === 'administrador';
}

/** Mantido para compatibilidade com o protótipo de pedidos, mas sem acesso de teste. */
function emModoTeste() { return false; }

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
