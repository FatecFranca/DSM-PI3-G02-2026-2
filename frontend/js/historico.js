document.addEventListener('DOMContentLoaded', () => {
  if (!usuarioEstaLogado()) {
    window.location.href = 'index.html';
    return;
  }
  configurarCabecalho();
  const formulario = document.getElementById('formBuscarPedidosAbertos');
  formulario.addEventListener('submit', (evento) => {
    evento.preventDefault();
    carregarPedidosAbertos(document.getElementById('termoPedidoAberto').value);
  });
  document.getElementById('formBuscarFinalizados').addEventListener('submit', (evento) => {
    evento.preventDefault();
    carregarPedidosFinalizados(document.getElementById('termoFinalizados').value);
  });
  carregarPedidosAbertos('');
  carregarPedidosFinalizados('');
  carregarHistoricoLocal();
});

/* ---------- Cards de pedido ---------- */
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

/* ---------- Pedidos abertos ---------- */
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
      const titulo = novoElemento('strong', rotuloPedido(p));
      const meta = novoElemento('p', `${formatarData(p.criadoEm)} · Situação: ${p.situacao || 'EM_ABERTO'}`, 'pedido-aberto__meta');
      const dados = novoElemento('div');
      dados.append(
        novoElemento('p', `Cliente: ${resumo.clienteNome || 'Não encontrado'} · ${formatarTelefone(resumo.clienteTelefone)}`)
      );
      const itens = novoElemento('ul', null, 'pedido-aberto__itens');
      (p.itens || []).forEach(item => {
        const primeiro = item.sabor || item.nomeProduto || 'Produto';
        const segundo = item.meioAMeio ? ` + ½ ${item.segundoSabor || '2º sabor'}` : '';
        const nome = item.meioAMeio ? `½ ${primeiro}${segundo}` : primeiro;
        itens.appendChild(novoElemento('li', `${item.quantidade || 1} × ${nome} — ${formatarMoeda(item.subtotal)}`));
      });
      const subtotal = novoElemento('p', `Subtotal: ${formatarMoeda(p.subtotalItens)} · Taxa: ${formatarMoeda(p.taxaEntrega)} · Total: ${p.total == null ? 'pendente de atualização' : formatarMoeda(p.total)}`);
      subtotal.style.fontWeight = '700';
      const reabrir = novoElemento('a', 'Abrir pedido para editar / finalizar', 'botao botao--preto');
      reabrir.href = `pedidos.html?pedido=${encodeURIComponent(p.id)}`;
      reabrir.style.cssText = 'display:inline-block; margin-top:12px; text-decoration:none;';
      cartao.append(titulo, meta, dados, itens, subtotal, reabrir);
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

/* ---------- Pedidos finalizados ---------- */
async function carregarPedidosFinalizados(termo) {
  const destino = document.getElementById('listaFinalizados');
  const aviso = document.getElementById('avisoFinalizados');
  destino.replaceChildren();
  aviso.textContent = 'Consultando histórico no servidor...';
  try {
    const resultados = await pesquisarPedidosFinalizados(termo);
    aviso.textContent = resultados.length
      ? `${resultados.length} pedido(s) finalizado(s).`
      : 'Nenhum pedido finalizado encontrado.';
    resultados.forEach(({pedido, clienteNome, clienteTelefone}) => {
      const cartao = montarCartao();
      const itens = novoElemento('ul', null, 'pedido-aberto__itens');
      (pedido.itens || []).forEach(item => {
        const nome = item.meioAMeio ? `${item.nomeProduto} + ${item.segundoSabor}` : item.nomeProduto;
        itens.appendChild(novoElemento('li', `${item.quantidade || 1} × ${nome || 'Produto'} — ${formatarMoeda(item.subtotal)}`));
      });
      cartao.append(
        novoElemento('strong', rotuloPedido(pedido)),
        novoElemento('p', `Finalizado em: ${formatarData(pedido.finalizadoEm)}`, 'pedido-aberto__meta'),
        novoElemento('p', `Cliente: ${clienteNome} · ${formatarTelefone(pedido.telefoneContato || clienteTelefone)}`),
        novoElemento('p', `Modalidade: ${pedido.modalidade === 'RETIRADA' ? 'Retirada' : 'Entrega'} · Pagamento: ${pedido.formaPagamento || '—'}`),
        itens,
        novoElemento('p', `Total: ${formatarMoeda(pedido.total)}`)
      );
      destino.appendChild(cartao);
    });
  } catch (erro) {
    aviso.textContent = `Não foi possível consultar finalizados: ${erro.message}`;
    if (erro.status === 401) {
      localStorageEncerrarSessao();
      window.location.href = 'index.html';
    }
  }
}

/* ---------- Histórico de impressão local ---------- */
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
    const titulo = novoElemento('strong', rotuloPedido(pedido));
    // O número salvo antigamente no navegador pode estar incorreto; consulta o MongoDB.
    if (pedido.id) {
      buscarPedidoPorId(pedido.id)
        .then(registro => { titulo.textContent = rotuloPedido(registro); })
        .catch(() => {});
    }
    cartao.append(
      titulo,
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
