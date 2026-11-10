/** Histórico de impressão local + pesquisa de pedidos abertos no MongoDB. */
document.addEventListener('DOMContentLoaded', () => {
  if (!usuarioEstaLogado()) {
    window.location.href = 'index.html';
    return;
  }
  const formulario = document.getElementById('formBuscarPedidosAbertos');
  formulario.addEventListener('submit', (evento) => {
    evento.preventDefault();
    carregarPedidosAbertos(document.getElementById('termoPedidoAberto').value);
  });
  carregarPedidosAbertos('');
  carregarHistoricoLocal();
});

function novoElemento(tag, texto, classe) {
  const elemento = document.createElement(tag);
  if (texto != null) elemento.textContent = String(texto);
  if (classe) elemento.className = classe;
  return elemento;
}

function montarCartao() {
  return novoElemento('article', null, 'cadastro__cartao');
}

function formatarMoeda(valor) {
  return Number(valor || 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}
function formatarData(data) {
  if (!data) return 'Data não informada';
  const valor = new Date(data);
  return Number.isNaN(valor.getTime()) ? 'Data inválida' : valor.toLocaleString('pt-BR');
}

async function carregarPedidosAbertos(termo) {
  const destino = document.getElementById('listaPedidosAbertos');
  const aviso = document.getElementById('avisoPedidosAbertos');
  destino.replaceChildren();
  aviso.textContent = 'Consultando pedidos no servidor...';
  try {
    const resultados = await pesquisarPedidosAbertos(termo);
    aviso.textContent = resultados.length
      ? `${resultados.length} pedido(s) em aberto encontrado(s).`
      : 'Nenhum pedido em aberto encontrado para essa pesquisa.';
    resultados.forEach((resumo) => {
      const p = resumo.pedido;
      const cartao = montarCartao();
      const titulo = novoElemento('strong', `Pedido ${p.id || '(sem ID)'}`);
      const meta = novoElemento('p', `${formatarData(p.criadoEm)} · Situação: ${p.situacao || 'EM_ABERTO'}`, 'pedido-aberto__meta');
      const dados = novoElemento('div');
      dados.append(
        novoElemento('p', `Cliente: ${resumo.clienteNome || 'Não encontrado'} · ${formatarTelefone(resumo.clienteTelefone)}`),
        novoElemento('p', `Atendente (matrícula): ${p.colaboradorMatricula || '—'}`)
      );
      const itens = novoElemento('ul', null, 'pedido-aberto__itens');
      (p.itens || []).forEach(item => {
        const primeiro = item.sabor || item.nomeProduto || 'Produto';
        const segundo = item.meioAMeio ? ` + ½ ${item.segundoSabor || '2º sabor'}` : '';
        const nome = item.meioAMeio ? `½ ${primeiro}${segundo}` : primeiro;
        itens.appendChild(novoElemento('li', `${item.quantidade || 1} × ${nome} — ${formatarMoeda(item.subtotal)}`));
      });
      const subtotal = novoElemento('p', `Subtotal dos itens: ${formatarMoeda(p.subtotalItens)}`);
      subtotal.style.fontWeight = '700';
      cartao.append(titulo, meta, dados, itens, subtotal);
      destino.appendChild(cartao);
    });
  } catch (erro) {
    aviso.textContent = `Não foi possível consultar pedidos: ${erro.message}`;
    if (erro.status === 401) {
      localStorageEncerrarSessao();
      window.location.href = 'index.html';
    }
  }
}

function carregarHistoricoLocal() {
  let historico = [];
  try { historico = JSON.parse(localStorage.getItem('cityPizzas_historicoPedidos') || '[]'); }
  catch (_) { historico = []; }
  const container = document.getElementById('listaPedidos');
  const listaVazia = document.getElementById('listaVazia');
  if (!Array.isArray(historico) || !historico.length) {
    listaVazia.style.display = 'block';
    return;
  }
  historico.forEach(pedido => {
    const cartao = montarCartao();
    cartao.append(
      novoElemento('strong', pedido.numero || pedido.id || 'Pedido'),
      novoElemento('p', formatarData(pedido.criadoEm), 'pedido-aberto__meta'),
      novoElemento('p', `Cliente: ${pedido.cliente || '—'}`),
      novoElemento('p', `Modalidade: ${pedido.modalidade === 'ENTREGA' ? 'Entrega' : 'Retirada'}`),
      novoElemento('p', `Atendente: ${pedido.atendente || '—'}`)
    );
    const itens = novoElemento('ul', null, 'pedido-aberto__itens');
    (pedido.itens || []).forEach(item => {
      const qtd = item.quantidade || 1;
      itens.appendChild(novoElemento('li', `${qtd} × ${item.nome || 'Produto'} — ${formatarMoeda(item.preco)} cada`));
    });
    const subtotal = novoElemento('p', `Subtotal impresso: ${formatarMoeda(pedido.total)}`);
    subtotal.style.fontWeight = '700';
    cartao.append(itens, subtotal);
    container.appendChild(cartao);
  });
}
