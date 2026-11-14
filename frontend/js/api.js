const API_BASE_URL = 'http://localhost:8080';

async function requisitar(caminho, opcoes = {}) {
  const cabecalhos = {
    'Content-Type': 'application/json',
    ...(opcoes.headers || {}),
  };

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
    corpo = texto;
  }

  if (!resposta.ok) {
    throw new ErroApi(extrairMensagemDeErro(corpo, resposta.status), resposta.status, corpo);
  }

  return corpo;
}

function extrairMensagemDeErro(corpo, status) {
  if (corpo && typeof corpo === 'object') {
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

function login(matricula, senha) {
  return requisitar('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ matricula, senha }),
  });
}

function solicitarRecuperacaoSenha(email) {
  return requisitar('/auth/recuperar-senha', {
    method: 'POST',
    body: JSON.stringify({ email }),
  });
}

function redefinirSenha(email, codigo, novaSenha) {
  return requisitar('/auth/redefinir-senha', {
    method: 'POST',
    body: JSON.stringify({ email, codigo, novaSenha }),
  });
}

/* ---------- Clientes ---------- */

function buscarClientes(termo) {
  return requisitar(`/clientes/busca?termo=${encodeURIComponent(termo.trim())}`, { method: 'GET' });
}

function cadastrarCliente(dadosCliente) {
  return requisitar('/clientes', {
    method: 'POST',
    body: JSON.stringify(dadosCliente),
  });
}

function cadastrarColaborador(dadosColaborador) {
  return requisitar('/colaboradores', {
    method: 'POST',
    body: JSON.stringify(dadosColaborador),
  });
}

function listarColaboradores() {
  return requisitar('/colaboradores', { method: 'GET' });
}

function excluirColaborador(colaboradorId) {
  return requisitar(`/colaboradores/${encodeURIComponent(colaboradorId)}`, { method: 'DELETE' });
}

function consultarAuditoria({ termo = '', de = '', ate = '' } = {}) {
  const parametros = new URLSearchParams();
  if (termo.trim()) parametros.set('termo', termo.trim());
  if (de) parametros.set('de', de);
  if (ate) parametros.set('ate', ate);
  const consulta = parametros.toString();
  return requisitar(`/auditoria${consulta ? `?${consulta}` : ''}`, { method: 'GET' });
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

function listarProdutos() {
  return requisitar('/produtos', { method: 'GET' });
}

function cadastrarProduto(dadosProduto) {
  return requisitar('/produtos', { method: 'POST', body: JSON.stringify(dadosProduto) });
}

function atualizarProduto(produtoId, dadosProduto) {
  return requisitar(`/produtos/${encodeURIComponent(produtoId)}`, {
    method: 'PUT', body: JSON.stringify(dadosProduto),
  });
}

function excluirProduto(produtoId) {
  return requisitar(`/produtos/${encodeURIComponent(produtoId)}`, { method: 'DELETE' });
}

function pesquisarProdutos(termo) {
  return requisitar(`/produtos/buscar?nome=${encodeURIComponent(termo.trim())}`, { method: 'GET' });
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

/* ---------- Pedidos ---------- */

function criarPedido(dadosPedido) {
  return requisitar('/pedidos', { method: 'POST', body: JSON.stringify(dadosPedido) });
}

function pesquisarPedidosAbertos(termo = '') {
  return requisitar(`/pedidos/abertos?termo=${encodeURIComponent(termo.trim())}`, { method: 'GET' });
}

function pesquisarPedidosFinalizados(termo = '') {
  return requisitar(`/pedidos/finalizados?termo=${encodeURIComponent(termo.trim())}`, { method: 'GET' });
}

function consultarPrecosPizza() {
  return requisitar('/pedidos/precos', { method: 'GET' });
}

function buscarPedidoPorId(pedidoId) {
  return requisitar(`/pedidos/${encodeURIComponent(pedidoId)}`, { method: 'GET' });
}

function buscarClientePorId(clienteId) {
  return requisitar(`/clientes/${encodeURIComponent(clienteId)}`, { method: 'GET' });
}

function atualizarPedido(pedidoId, dadosPedido) {
  return requisitar(`/pedidos/${encodeURIComponent(pedidoId)}`, {
    method: 'PUT', body: JSON.stringify(dadosPedido),
  });
}

function finalizarPedido(pedidoId) {
  return requisitar(`/pedidos/${encodeURIComponent(pedidoId)}/finalizar`, { method: 'POST' });
}

function consultarTaxaEntrega() {
  return requisitar('/pedidos/taxa-entrega', { method: 'GET' });
}

function cancelarPedido(pedidoId) {
  return requisitar(`/pedidos/admin/${encodeURIComponent(pedidoId)}`, { method: 'DELETE' });
}

function corrigirValorPedido(pedidoId, novoValorTotal, motivo) {
  return requisitar(`/pedidos/admin/${encodeURIComponent(pedidoId)}/corrigir-valor`, {
    method: 'PUT', body: JSON.stringify({ novoValorTotal, motivo }),
  });
}

/** Identificação comercial: nunca exibir o ID técnico do MongoDB ao usuário. */
function rotuloPedido(pedido) {
  const numero = pedido?.numero;
  if (Number.isSafeInteger(Number(numero)) && Number(numero) > 0 && String(numero).trim() !== '') {
    return `Pedido ${Number(numero)}`;
  }
  // Compatibilidade com comprovantes impressos localmente em versões antigas.
  if (typeof numero === 'string' && /^Pedido\s+[0-9]+$/i.test(numero.trim())) {
    return `Pedido ${Number(numero.trim().split(/\s+/).pop())}`;
  }
  return 'Pedido antigo (sem numeração)';
}

/* ---------- Sessão local ---------- */

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
  localStorage.removeItem('cityPizzas_historicoPedidos');
  sessionStorage.removeItem('cityPizzas_clienteSelecionado');
}

async function sairDoSistema() {
  try { await encerrarSessaoNoServidor(); } catch (_) { }
  localStorageEncerrarSessao();
  window.location.href = 'index.html';
}

/* ---------- Permissões na interface ---------- */

function configurarCabecalho() {
  if (!usuarioEhAdministrador()) {
    document.querySelectorAll('[data-somente-admin]').forEach((elemento) => elemento.remove());
  }
  const nome = document.getElementById('nomeUsuario');
  if (nome) nome.textContent = `Olá, ${nomeDoUsuarioLogado()}`;
  const sair = document.getElementById('linkSair');
  if (sair) {
    sair.addEventListener('click', (evento) => {
      evento.preventDefault();
      sairDoSistema();
    });
  }
}

function dadosMinimosDoCliente(cliente) {
  return {
    id: cliente?.id,
    nome: cliente?.nome,
    telefone: cliente?.telefone,
    endereco: cliente?.endereco,
  };
}

function usuarioEstaLogado() {
  return Boolean(localStorageObterUsuario() && localStorageObterToken());
}

function usuarioEhAdministrador() {
  return localStorageObterUsuario()?.perfil === 'administrador';
}

function emModoTeste() { return false; }

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
