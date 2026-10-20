let clienteSelecionado = null;
let itensPedido = [];
const TAXA_ENTREGA_PADRAO = 5.0;

document.addEventListener('DOMContentLoaded', async () => {
  if (!usuarioEstaLogado()) {
    window.location.href = 'index.html';
    return;
  }

  document.getElementById('nomeUsuario').textContent = `Olá, ${nomeDoUsuarioLogado()}`;
  document.getElementById('atendentePedido').textContent = `Atendente: ${nomeDoUsuarioLogado()}`;
  document.getElementById('numeroPedido').textContent = `pedido ${proximoNumeroPedido()}`;
  document.getElementById('valorTaxaEntrega').textContent = formatarMoeda(TAXA_ENTREGA_PADRAO);

  document.getElementById('linkSair').addEventListener('click', (evento) => {
    evento.preventDefault();
    localStorageEncerrarSessao();
    window.location.href = 'index.html';
  });

  configurarBuscaCliente();
  restaurarClienteRecemCadastrado();
  await configurarBuscaProduto();
  configurarModalidade();

  document.getElementById('selectModalidade').addEventListener('change', calcularTotal);
  document.getElementById('botaoNovoPedido').addEventListener('click', reiniciarPedido);
  document.getElementById('botaoCancelar').addEventListener('click', cancelarPedidoAtual);
  document.getElementById('botaoImprimir').addEventListener('click', finalizarEImprimir);

  renderizarItens();
});

function configurarBuscaCliente() {
  const botaoTrocar = document.getElementById('botaoTrocarCliente');
  const painel = document.getElementById('painelCliente');
  const campo = document.getElementById('campoBuscaCliente');
  const resultados = document.getElementById('resultadosCliente');
  let timeoutBusca = null;

  botaoTrocar.addEventListener('click', () => {
    painel.style.display = painel.style.display === 'none' ? 'block' : 'none';
    if (painel.style.display === 'block') campo.focus();
  });

  campo.addEventListener('input', () => {
    clearTimeout(timeoutBusca);
    const termo = campo.value.trim();
    if (termo.length < 2) {
      resultados.style.display = 'none';
      return;
    }
    timeoutBusca = setTimeout(async () => {
      try {
        const clientes = await buscarClientes(termo);
        renderizarResultadosCliente(clientes);
      } catch (erro) {
        if (emModoTeste()) {
          renderizarResultadosCliente(clientesDeExemplo(termo));
        } else {
          resultados.innerHTML = `<div style="padding:10px 14px; font-size:0.85rem; color:#C23A2D;">
            Não foi possível buscar clientes: ${erro.message}
          </div>`;
          resultados.style.display = 'block';
        }
      }
    }, 300);
  });

  function renderizarResultadosCliente(clientes) {
    if (!clientes.length) {
      resultados.innerHTML = `<div style="padding:10px 14px; font-size:0.85rem; color:#4A4A4A;">
        Nenhum cliente encontrado. <a href="cadastros.html?origem=pedidos">Cadastrar novo cliente</a>
      </div>`;
      resultados.style.display = 'block';
      return;
    }

    resultados.innerHTML = clientes
      .map((c) => `<button type="button" data-cliente-id="${c.id}">${c.nome} — ${formatarTelefone(c.telefone)}</button>`)
      .join('');

    resultados.querySelectorAll('button[data-cliente-id]').forEach((botao, indice) => {
      botao.addEventListener('click', () => {
        clienteSelecionado = clientes[indice];
        document.getElementById('nomeCliente').textContent = clienteSelecionado.nome;
        resultados.style.display = 'none';
        painel.style.display = 'none';
        campo.value = '';
      });
    });

    resultados.style.display = 'block';
  }
}

/** Depois de cadastrar um cliente (cadastros.html?origem=pedidos), ele volta já selecionado. */
function restaurarClienteRecemCadastrado() {
  const bruto = sessionStorage.getItem('cityPizzas_clienteSelecionado');
  if (!bruto) return;
  sessionStorage.removeItem('cityPizzas_clienteSelecionado');
  clienteSelecionado = JSON.parse(bruto);
  document.getElementById('nomeCliente').textContent = clienteSelecionado.nome;
}

/** Só usado no "modo de teste" (sessão sem back-end). */
function clientesDeExemplo(termo) {
  return [
    { id: 1, nome: 'Cliente 1', telefone: '(16) 99101-1234' },
    { id: 2, nome: 'Cliente 002', telefone: '(16) 99361-3252' },
  ].filter((c) => c.nome.toLowerCase().includes(termo.toLowerCase()));
}

/* ---------- Produtos ---------- */

let catalogoProdutos = [];

async function configurarBuscaProduto() {
  const campo = document.getElementById('campoBuscaProduto');
  const resultados = document.getElementById('resultadosProduto');
  try {
    catalogoProdutos = await listarProdutos();
  } catch (erro) {
    if (emModoTeste()) {
      catalogoProdutos = produtosDeExemplo();
    } else {
      catalogoProdutos = [];
      mostrarErro(`Não foi possível carregar os produtos: ${erro.message}`);
    }
  }

  campo.addEventListener('input', () => {
    const termo = campo.value.trim().toLowerCase();
    if (termo.length < 2) {
      resultados.style.display = 'none';
      return;
    }

    const encontrados = catalogoProdutos.filter(
      (p) => p.nome.toLowerCase().includes(termo) || (p.sabor || '').toLowerCase().includes(termo)
    );
    renderizarResultadosProduto(encontrados);
  });

  function renderizarResultadosProduto(produtos) {
    if (!produtos.length) {
      resultados.innerHTML = `<div style="padding:10px 14px; font-size:0.85rem; color:#4A4A4A;">Nenhum produto encontrado.</div>`;
      resultados.style.display = 'block';
      return;
    }

    resultados.innerHTML = produtos
      .map((p, i) => `<button type="button" data-indice="${i}">${nomeExibicaoProduto(p)} — ${formatarMoeda(p.preco)}</button>`)
      .join('');

    resultados.querySelectorAll('button[data-indice]').forEach((botao, indice) => {
      botao.addEventListener('click', () => {
        adicionarItem(produtos[indice]);
        resultados.style.display = 'none';
        campo.value = '';
      });
    });

    resultados.style.display = 'block';
  }
}

function nomeExibicaoProduto(produto) {
  if (produto.sabor && produto.sabor !== produto.nome) {
    return `${produto.nome} (${produto.sabor})`;
  }
  return produto.nome;
}

function produtosDeExemplo() {
  return [
    { id: '1', nome: 'Moda da Casa c/ Catupiry', sabor: 'Moda da Casa', tamanho: 2, preco: 59.0 },
    { id: '2', nome: 'Calabresa c/ Catupiry', sabor: 'Calabresa', tamanho: 2, preco: 59.0 },
    { id: '3', nome: 'Margherita', sabor: 'Margherita', tamanho: 2, preco: 54.0 },
  ];
}

/* ---------- Itens do pedido ---------- */

function adicionarItem(produto) {
  itensPedido.push({
    uid: proximoUid++,
    produto: { id: produto.id, nome: produto.nome, sabor: produto.sabor, preco: produto.preco },
    tipoPreparo: 'ASSADA',
    observacao: '',
    acrescimos: [],
  });
  renderizarItens();
}

function removerItem(uid) {
  itensPedido = itensPedido.filter((item) => item.uid !== uid);
  renderizarItens();
}

function renderizarItens() {
  const container = document.getElementById('itensPedido');
  const vazio = document.getElementById('pedidoVazio');

  if (itensPedido.length === 0) {
    container.innerHTML = '';
    container.appendChild(vazio);
    vazio.style.display = 'block';
    calcularTotal();
    return;
  }

  container.innerHTML = '';

  itensPedido.forEach((item) => {
    const linha = document.createElement('div');
    linha.className = 'item-pedido';
    linha.innerHTML = `
      <div class="item-pedido__imagem" aria-hidden="true"></div>

      <div class="item-pedido__info">
        <h3>${nomeExibicaoProduto(item.produto)}</h3>
        <div class="item-pedido__obs">
          <span>obs:</span>
          <select data-uid="${item.uid}" class="select-observacao">
            <option value="">Nenhuma</option>
            <option value="SEM MILHO">Sem milho</option>
            <option value="SEM CEBOLA">Sem cebola</option>
            <option value="COM CEBOLA">Com cebola</option>
            <option value="SEM TOMATE">Sem tomate</option>
          </select>
          <button type="button" class="botao--link" data-uid="${item.uid}" data-acao="adicionar-acrescimo" style="background:none; border:none; text-decoration:underline; cursor:pointer; font-size:0.8rem;">+ acréscimo</button>
          ${item.acrescimos
            .map(
              (ac, i) =>
                `<span class="item-pedido__acrescimo-tag">ACRÉSCIMO: ${ac} <button type="button" data-uid="${item.uid}" data-acrescimo-indice="${i}" style="border:none;background:none;cursor:pointer;color:#C23A2D;">✕</button></span>`
            )
            .join('')}
        </div>
      </div>

      <div class="item-pedido__preparo">
        <select data-uid="${item.uid}" class="select-preparo">
          <option value="ASSADA" ${item.tipoPreparo === 'ASSADA' ? 'selected' : ''}>Assada</option>
          <option value="PRE_ASSADA" ${item.tipoPreparo === 'PRE_ASSADA' ? 'selected' : ''}>Pré-assada</option>
        </select>
        <div class="item-pedido__preparo-alt">${item.tipoPreparo === 'ASSADA' ? 'Pré-assadas' : 'Assadas'}</div>
      </div>

      <div class="item-pedido__precos">
        ${item.acrescimos.map(() => `<div>${formatarMoeda(5)}</div>`).join('')}
        <div>${formatarMoeda(item.produto.preco)}</div>
        <button type="button" class="item-pedido__remover" data-uid="${item.uid}" data-acao="remover">remover item</button>
      </div>
    `;
    container.appendChild(linha);
  });

  // Listeners
  container.querySelectorAll('.select-observacao').forEach((select) => {
    select.value = itensPedido.find((i) => i.uid == select.dataset.uid)?.observacao || '';
    select.addEventListener('change', () => {
      const item = itensPedido.find((i) => i.uid == select.dataset.uid);
      item.observacao = select.value;
    });
  });

  container.querySelectorAll('.select-preparo').forEach((select) => {
    select.addEventListener('change', () => {
      const item = itensPedido.find((i) => i.uid == select.dataset.uid);
      item.tipoPreparo = select.value;
      renderizarItens();
    });
  });

  container.querySelectorAll('[data-acao="remover"]').forEach((botao) => {
    botao.addEventListener('click', () => removerItem(Number(botao.dataset.uid)));
  });

  container.querySelectorAll('[data-acao="adicionar-acrescimo"]').forEach((botao) => {
    botao.addEventListener('click', () => {
      const nomeIngrediente = prompt('Nome do ingrediente para acréscimo (R$ 5,00):');
      if (!nomeIngrediente) return;
      const item = itensPedido.find((i) => i.uid == botao.dataset.uid);
      item.acrescimos.push(nomeIngrediente.trim().toUpperCase());
      renderizarItens();
    });
  });

  container.querySelectorAll('[data-acrescimo-indice]').forEach((botao) => {
    botao.addEventListener('click', () => {
      const item = itensPedido.find((i) => i.uid == botao.dataset.uid);
      item.acrescimos.splice(Number(botao.dataset.acrescimoIndice), 1);
      renderizarItens();
    });
  });

  calcularTotal();
}

/* ---------- Modalidade (Entrega / Retirada) ---------- */

function configurarModalidade() {
  const select = document.getElementById('selectModalidade');
  const camposEntrega = document.getElementById('camposEntrega');
  const camposRetirada = document.getElementById('camposRetirada');

  select.addEventListener('change', () => {
    const ehEntrega = select.value === 'ENTREGA';
    camposEntrega.style.display = ehEntrega ? 'grid' : 'none';
    camposRetirada.style.display = ehEntrega ? 'none' : 'grid';
  });
}

/* ---------- Total ---------- */

function calcularTotal() {
  const totalItens = itensPedido.reduce((soma, item) => {
    const totalAcrescimos = item.acrescimos.length * 5.0;
    return soma + item.produto.preco + totalAcrescimos;
  }, 0);

  const modalidade = document.getElementById('selectModalidade').value;
  const taxaEntrega = modalidade === 'ENTREGA' ? TAXA_ENTREGA_PADRAO : 0;
  const total = totalItens + taxaEntrega;

  document.getElementById('valorTotal').textContent = formatarMoeda(total);
  return total;
}

/* ---------- Ações: novo / cancelar / imprimir ---------- */

function reiniciarPedido() {
  clienteSelecionado = null;
  itensPedido = [];
  document.getElementById('nomeCliente').textContent = 'Cliente não selecionado';
  document.getElementById('numeroPedido').textContent = `pedido ${proximoNumeroPedido()}`;
  document.getElementById('observacaoGeral').value = '';
  document.getElementById('selectModalidade').value = 'ENTREGA';
  document.getElementById('camposEntrega').style.display = 'grid';
  document.getElementById('camposRetirada').style.display = 'none';
  esconderMensagens();
  renderizarItens();
}

function cancelarPedidoAtual() {
  if (itensPedido.length === 0 && !clienteSelecionado) return;
  if (confirm('Cancelar este pedido? Os itens adicionados serão perdidos.')) {
    reiniciarPedido();
  }
}

async function finalizarEImprimir() {
  esconderMensagens();

  if (!clienteSelecionado) {
    mostrarErro('Selecione um cliente antes de finalizar o pedido.');
    return;
  }

  if (itensPedido.length === 0) {
    mostrarErro('Adicione pelo menos um item ao pedido.');
    return;
  }

  const modalidade = document.getElementById('selectModalidade').value;

  const dadosPedido = {
    clienteId: clienteSelecionado.id,
    itens: itensPedido.map((item) => ({
      produtoId: item.produto.id,
      meioAMeio: false,
      tipoPreparo: item.tipoPreparo,
      observacao: item.observacao || undefined,
      acrescimos: item.acrescimos,
    })),
    modalidade,
    endereco:
      modalidade === 'ENTREGA'
        ? {
            rua: document.getElementById('endRua').value.trim(),
            numero: document.getElementById('endNumero').value.trim(),
            bairro: document.getElementById('endBairro').value.trim(),
          }
        : undefined,
    contatoRetirada:
      modalidade === 'RETIRADA'
        ? {
            nome: document.getElementById('retiradaNome').value.trim(),
            telefone: document.getElementById('retiradaTelefone').value.trim(),
          }
        : undefined,
    formaPagamento: document.getElementById('selectPagamento').value,
    observacaoGeral: document.getElementById('observacaoGeral').value.trim() || undefined,
  };

  try {
    await criarPedido(dadosPedido);
    mostrarSucesso('Pedido registrado com sucesso.');
  } catch (erro) {
    mostrarErro(`Pedido não sincronizado com o servidor (${erro.message}). Imprimindo mesmo assim.`);
  }

  salvarPedidoNoHistoricoLocal({
    numero: document.getElementById('numeroPedido').textContent,
    cliente: clienteSelecionado.nome,
    itens: itensPedido.map((item) => ({
      nome: nomeExibicaoProduto(item.produto),
      preparo: item.tipoPreparo === 'ASSADA' ? 'Assada' : 'Pré-assada',
      observacao: item.observacao,
      acrescimos: item.acrescimos,
      preco: item.produto.preco,
    })),
    modalidade,
    endereco: dadosPedido.endereco,
    contatoRetirada: dadosPedido.contatoRetirada,
    formaPagamento: dadosPedido.formaPagamento,
    total: calcularTotal(),
    atendente: localStorageObterUsuario()?.nome || 'Usuário',
    criadoEm: new Date().toISOString(),
  });

  window.print();
}

function salvarPedidoNoHistoricoLocal(pedido) {
  const historico = JSON.parse(localStorage.getItem('cityPizzas_historicoPedidos') || '[]');
  historico.unshift(pedido);
  localStorage.setItem('cityPizzas_historicoPedidos', JSON.stringify(historico.slice(0, 50)));
}

/* ---------- Utilidades ---------- */

function proximoNumeroPedido() {
  const atual = Number(localStorage.getItem('cityPizzas_ultimoPedido') || '0') + 1;
  localStorage.setItem('cityPizzas_ultimoPedido', String(atual));
  return String(atual).padStart(3, '0');
}

function formatarMoeda(valor) {
  return Number(valor).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

function mostrarErro(texto) {
  const el = document.getElementById('mensagemErro');
  el.textContent = texto;
  el.style.display = 'block';
}

function mostrarSucesso(texto) {
  const el = document.getElementById('mensagemSucesso');
  el.textContent = texto;
  el.style.display = 'block';
}

function esconderMensagens() {
  document.getElementById('mensagemErro').style.display = 'none';
  document.getElementById('mensagemSucesso').style.display = 'none';
}
