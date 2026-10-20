document.addEventListener('DOMContentLoaded', () => {
  const historico = JSON.parse(localStorage.getItem('cityPizzas_historicoPedidos') || '[]');
  const container = document.getElementById('listaPedidos');
  const listaVazia = document.getElementById('listaVazia');

  if (historico.length === 0) {
    listaVazia.style.display = 'block';
    return;
  }

  container.innerHTML = historico.map(renderizarPedido).join('');
});

function renderizarPedido(pedido) {
  const dataFormatada = new Date(pedido.criadoEm).toLocaleString('pt-BR');
  const modalidadeTexto = pedido.modalidade === 'ENTREGA' ? 'Entrega' : 'Retirada';
  const enderecoOuContato =
    pedido.modalidade === 'ENTREGA'
      ? `${pedido.endereco?.rua || ''}, ${pedido.endereco?.numero || ''} — ${pedido.endereco?.bairro || ''}`
      : `${pedido.contatoRetirada?.nome || ''} — ${pedido.contatoRetirada?.telefone || ''}`;

  const itensHtml = pedido.itens
    .map((item) => {
      const acrescimosTexto = item.acrescimos?.length
        ? ` + acréscimo: ${item.acrescimos.join(', ')}`
        : '';
      const obsTexto = item.observacao ? ` (${item.observacao})` : '';
      return `<li>${item.nome} — ${item.preparo}${obsTexto}${acrescimosTexto} — ${formatarMoeda(item.preco)}</li>`;
    })
    .join('');

  return `
    <div class="cadastro__cartao">
      <div style="display:flex; justify-content:space-between; align-items:baseline; margin-bottom:8px;">
        <strong>${pedido.numero}</strong>
        <span style="font-size:0.82rem; color:#4A4A4A;">${dataFormatada}</span>
      </div>
      <div style="font-size:0.9rem; margin-bottom:10px;">
        <div><strong>Cliente:</strong> ${pedido.cliente}</div>
        <div><strong>${modalidadeTexto}:</strong> ${enderecoOuContato}</div>
        <div><strong>Pagamento:</strong> ${traduzirPagamento(pedido.formaPagamento)}</div>
        <div><strong>Atendente:</strong> ${pedido.atendente}</div>
      </div>
      <ul style="margin:0 0 10px; padding-left:18px; font-size:0.88rem;">${itensHtml}</ul>
      <div style="text-align:right; font-weight:700;">Total: ${formatarMoeda(pedido.total)}</div>
    </div>
  `;
}

function traduzirPagamento(codigo) {
  const mapa = { DINHEIRO: 'Dinheiro', PIX: 'Pix', CREDITO: 'Crédito', DEBITO: 'Débito' };
  return mapa[codigo] || codigo;
}

function formatarMoeda(valor) {
  return Number(valor).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}
