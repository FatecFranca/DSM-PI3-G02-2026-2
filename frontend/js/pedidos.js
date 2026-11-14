let clienteSelecionado = null;
let itensPedido = [];
let proximoUid = 1;
let catalogoProdutos = [];
let enviandoPedido = false;
let pedidoEmEdicaoId = null;
let taxaEntrega = null;
let precosPizza = { precoBase: null, precoPreAssada: null };

const VALOR_ACRESCIMO = 5.0;
const QUANTIDADE_MIN = 1;
const QUANTIDADE_MAX = 100;
const SUGESTOES_OBSERVACAO = ['SEM MILHO', 'SEM CEBOLA', 'COM CEBOLA', 'SEM TOMATE', 'SEM AZEITONA', 'BORDA RECHEADA'];

document.addEventListener('DOMContentLoaded', async () => {
  if (!usuarioEstaLogado()) {
    window.location.href = 'index.html';
    return;
  }

  configurarCabecalho();
  document.getElementById('atendentePedido').textContent = `Atendente: ${nomeDoUsuarioLogado()}`;
  atualizarNumeroPedido();

  configurarBuscaCliente();
  restaurarClienteRecemCadastrado();
  await carregarPrecosPizza();
  await configurarBuscaProduto();
  configurarModalidade();
  await carregarTaxaEntrega();
  await abrirPedidoDaUrl();

  document.getElementById('botaoNovoPedido').addEventListener('click', reiniciarPedido);
  document.getElementById('botaoCancelar').addEventListener('click', cancelarPedidoAtual);
  document.getElementById('botaoImprimir').addEventListener('click', abrirPopupDeFinalizacao);
  document.getElementById('botaoSalvarAberto').addEventListener('click', salvarComoAberto);

  renderizarItens();
});

/* ---------- Cliente ---------- */

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
        resultados.replaceChildren(criarAviso(`Não foi possível buscar clientes: ${erro.message}`, true));
        resultados.style.display = 'block';
      }
    }, 300);
  });

  function renderizarResultadosCliente(clientes) {
    resultados.replaceChildren();
    if (!clientes.length) {
      const aviso = criarAviso('Nenhum cliente encontrado. ');
      const link = document.createElement('a');
      link.href = 'cadastros.html?origem=pedidos';
      link.textContent = 'Cadastrar novo cliente';
      aviso.appendChild(link);
      resultados.appendChild(aviso);
      resultados.style.display = 'block';
      return;
    }

    resultados.replaceChildren();
    clientes.forEach((cliente) => {
      const botao = document.createElement('button');
      botao.type = 'button';
      botao.textContent = `${cliente.nome} — ${formatarTelefone(cliente.telefone)}`;
      botao.addEventListener('click', () => {
        selecionarCliente(cliente);
        resultados.style.display = 'none';
        painel.style.display = 'none';
        campo.value = '';
      });
      resultados.appendChild(botao);
    });

    resultados.style.display = 'block';
  }
}

function restaurarClienteRecemCadastrado() {
  const bruto = sessionStorage.getItem('cityPizzas_clienteSelecionado');
  if (!bruto) return;
  sessionStorage.removeItem('cityPizzas_clienteSelecionado');
  try {
    selecionarCliente(JSON.parse(bruto));
  } catch (_) {
    clienteSelecionado = null;
  }
}

function selecionarCliente(cliente) {
  clienteSelecionado = cliente;
  document.getElementById('nomeCliente').textContent = cliente?.nome || 'Cliente não selecionado';
  document.getElementById('telefoneContato').value = formatarTelefone(cliente?.telefone || '');
  document.getElementById('retiradaNome').value = cliente?.nome || '';
  preencherEndereco(cliente?.endereco || {});
}

/* ---------- Endereço e taxa de entrega ---------- */
const CAMPOS_ENDERECO = {
  rua: 'endRua', numero: 'endNumero', bairro: 'endBairro',
  cidade: 'endCidade', cep: 'endCep', complemento: 'endComplemento',
};

function preencherEndereco(endereco = {}) {
  Object.entries(CAMPOS_ENDERECO).forEach(([chave, id]) => {
    document.getElementById(id).value = endereco?.[chave] || '';
  });
}

function lerEndereco() {
  const endereco = {};
  Object.entries(CAMPOS_ENDERECO).forEach(([chave, id]) => {
    endereco[chave] = document.getElementById(id).value.trim();
  });
  return endereco;
}

async function carregarTaxaEntrega() {
  try {
    const dados = await consultarTaxaEntrega();
    taxaEntrega = dados.taxaEntrega === null || dados.taxaEntrega === undefined
      ? null : Number(dados.taxaEntrega);
    if (taxaEntrega !== null && (!Number.isFinite(taxaEntrega) || taxaEntrega < 0)) {
      taxaEntrega = null;
    }
  } catch (erro) {
    taxaEntrega = null;
    mostrarErro(`Taxa de entrega indisponível: ${erro.message}`);
  }
  atualizarTotal();
}

/* ---------- Reabrir pedido em aberto ---------- */
async function abrirPedidoDaUrl(id = new URLSearchParams(window.location.search).get('pedido')) {
  if (!id) return;
  try {
    const pedido = await buscarPedidoPorId(id);
    if (pedido.situacao !== 'EM_ABERTO') {
      mostrarErro('Este pedido já foi finalizado e não pode ser reaberto.');
      return;
    }
    const cliente = await buscarClientePorId(pedido.clienteId);
    selecionarCliente(cliente);
    pedidoEmEdicaoId = pedido.id;
    document.getElementById('numeroPedido').textContent = `pedido ${pedido.id}`;
    itensPedido = (pedido.itens || []).map((salvo) => {
      const produto = catalogoProdutos.find((p) => String(p.id) === String(salvo.produtoId));
      const segundoProduto = salvo.segundoProdutoId
        ? catalogoProdutos.find((p) => String(p.id) === String(salvo.segundoProdutoId))
        : null;
      if (!produto || (salvo.meioAMeio && !segundoProduto)) {
        throw new Error(`Um produto do pedido (${salvo.nomeProduto || salvo.produtoId}) não existe mais no catálogo. Não altere este pedido até revisar o catálogo.`);
      }
      return {
        uid: proximoUid++, produto, segundoProduto: segundoProduto || null,
        meioAMeio: Boolean(salvo.meioAMeio), quantidade: salvo.quantidade || 1,
        tipoPreparo: salvo.tipoPreparo || 'ASSADA', observacao: salvo.observacao || '',
        acrescimos: Array.isArray(salvo.acrescimos) ? [...salvo.acrescimos] : [],
        ingredientesRemovidos: Array.isArray(salvo.ingredientesRemovidos) ? [...salvo.ingredientesRemovidos] : [],
      };
    });
    if (pedido.telefoneContato) document.getElementById('telefoneContato').value = formatarTelefone(pedido.telefoneContato);
    document.getElementById('selectModalidade').value = pedido.modalidade || 'ENTREGA';
    if (pedido.enderecoEntrega) preencherEndereco(pedido.enderecoEntrega);
    document.getElementById('retiradaNome').value = pedido.nomeRetirada || cliente.nome || '';
    document.getElementById('selectPagamento').value = pedido.formaPagamento || 'DINHEIRO';
    document.getElementById('observacaoGeral').value = pedido.observacaoGeral || '';
    atualizarCamposModalidade();
    renderizarItens();
    mostrarSucesso(`Pedido ${pedido.id} reaberto para edição. Salve as alterações ou finalize quando estiver pronto.`);
  } catch (erro) {
    mostrarErro(`Não foi possível reabrir o pedido: ${erro.message}`);
    pedidoEmEdicaoId = null;
    itensPedido = [];
    atualizarNumeroPedido();
    renderizarItens();
  }
}

/* ---------- Catálogo e busca de produtos ---------- */

function ehPizza(produto) {
  return String(produto?.categoria || '').trim().toUpperCase() === 'PIZZA';
}

async function carregarPrecosPizza() {
  try {
    const precos = await consultarPrecosPizza();
    precosPizza.precoBase = precos.precoBase == null ? null : Number(precos.precoBase);
    precosPizza.precoPreAssada = precos.precoPreAssada == null ? null : Number(precos.precoPreAssada);
  } catch (erro) {
    mostrarErro(`Preços de pizza indisponíveis: ${erro.message}. Configure PIZZA_PRECO_BASE no servidor.`);
  }
}

function precoDoProduto(produto) {
  const preco = Number(produto?.preco);
  return Number.isFinite(preco) ? preco : 0;
}

function nomeExibicaoProduto(produto) {
  if (!produto) return '';
  if (produto.sabor && produto.sabor !== produto.nome) {
    return `${produto.nome} (${produto.sabor})`;
  }
  return [produto.nome, produto.apresentacaoVolume].filter(Boolean).join(' — ');
}

function normalizarTexto(texto) {
  return String(texto || '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase();
}

function filtrarCatalogo(termo) {
  const busca = normalizarTexto(termo.trim());
  if (!busca) return [];
  return catalogoProdutos.filter((p) =>
    [p.nome, p.sabor, p.categoria, p.apresentacaoVolume].some((campo) => normalizarTexto(campo).includes(busca))
  );
}

async function configurarBuscaProduto() {
  const campo = document.getElementById('campoBuscaProduto');
  const resultados = document.getElementById('resultadosProduto');

  try {
    const lista = await listarProdutos();
    catalogoProdutos = Array.isArray(lista) ? lista : [];
    if (!catalogoProdutos.length) {
      mostrarErro('Nenhum produto cadastrado no catálogo. Peça o cadastro dos produtos antes de montar pedidos.');
    }
  } catch (erro) {
    catalogoProdutos = [];
    if (erro.status === 401) return sessaoExpirada();
    mostrarErro(`Não foi possível carregar os produtos: ${erro.message}`);
  }

  let encontrados = [];

  campo.addEventListener('input', () => {
    if (!campo.value.trim()) {
      resultados.style.display = 'none';
      return;
    }
    encontrados = filtrarCatalogo(campo.value);
    renderizarResultadosProduto(encontrados);
  });

  campo.addEventListener('keydown', (evento) => {
    if (evento.key === 'Enter') {
      evento.preventDefault();
      if (encontrados.length && resultados.style.display === 'block') escolher(encontrados[0]);
    } else if (evento.key === 'Escape') {
      resultados.style.display = 'none';
    }
  });

  document.addEventListener('click', (evento) => {
    if (!evento.target.closest('.busca-produto')) resultados.style.display = 'none';
  });

  function escolher(produto) {
    adicionarItem(produto);
    resultados.style.display = 'none';
    campo.value = '';
    encontrados = [];
  }

  function renderizarResultadosProduto(produtos) {
    resultados.replaceChildren();
    if (!produtos.length) {
      resultados.appendChild(criarAviso('Nenhum produto encontrado.'));
      resultados.style.display = 'block';
      return;
    }

    produtos.slice(0, 30).forEach((produto) => {
      const botao = document.createElement('button');
      botao.type = 'button';
      const preco = ehPizza(produto) ? precosPizza.precoBase : precoDoProduto(produto);
      botao.textContent = `${nomeExibicaoProduto(produto)} — ${
        preco != null && Number.isFinite(preco) ? formatarMoeda(preco) : 'preço a configurar'}`;
      botao.addEventListener('click', () => escolher(produto));
      resultados.appendChild(botao);
    });
    resultados.style.display = 'block';
  }
}

function criarAviso(texto, erro = false) {
  const div = document.createElement('div');
  div.style.cssText = `padding:10px 14px; font-size:0.85rem; color:${erro ? '#C23A2D' : '#4A4A4A'};`;
  div.textContent = texto;
  return div;
}

/* ---------- Itens do pedido ---------- */

function adicionarItem(produto) {
  itensPedido.push({
    uid: proximoUid++,
    produto,
    segundoProduto: null,
    meioAMeio: false,
    quantidade: 1,
    tipoPreparo: 'ASSADA',
    observacao: '',
    acrescimos: [],
    ingredientesRemovidos: [],
  });
  esconderMensagens();
  renderizarItens();
}

function removerItem(uid) {
  itensPedido = itensPedido.filter((item) => item.uid !== uid);
  renderizarItens();
}

function itemPorUid(uid) {
  return itensPedido.find((item) => item.uid === uid);
}

function valorUnitarioItem(item) {
  let preco = precoDoProduto(item.produto);
  if (ehPizza(item.produto)) {
    preco = item.tipoPreparo === 'PRE_ASSADA' ? precosPizza.precoPreAssada : precosPizza.precoBase;
    if (preco === null || !Number.isFinite(preco)) return null;
  }
  return preco + item.acrescimos.length * VALOR_ACRESCIMO;
}

function subtotalItem(item) {
  return (valorUnitarioItem(item) ?? 0) * item.quantidade;
}

function nomeItemCompleto(item) {
  if (item.meioAMeio) {
    const segundo = item.segundoProduto ? nomeExibicaoProduto(item.segundoProduto) : '(2º sabor pendente)';
    return `½ ${nomeExibicaoProduto(item.produto)} + ½ ${segundo}`;
  }
  return nomeExibicaoProduto(item.produto);
}

function el(tag, props = {}, ...filhos) {
  const no = document.createElement(tag);
  Object.entries(props).forEach(([chave, valor]) => {
    if (chave === 'className') no.className = valor;
    else if (chave === 'text') no.textContent = valor;
    else if (chave.startsWith('on')) no.addEventListener(chave.slice(2).toLowerCase(), valor);
    else if (valor !== undefined && valor !== null && valor !== false) no.setAttribute(chave, valor === true ? '' : valor);
  });
  filhos.flat().forEach((f) => f != null && no.appendChild(typeof f === 'string' ? document.createTextNode(f) : f));
  return no;
}

function renderizarItens() {
  const container = document.getElementById('itensPedido');
  container.replaceChildren();

  if (itensPedido.length === 0) {
    const vazio = el('p', { className: 'pedido__vazio', id: 'pedidoVazio', text: 'Nenhum item adicionado ainda. Busque um produto acima.' });
    container.appendChild(vazio);
    atualizarTotal();
    return;
  }

  itensPedido.forEach((item) => container.appendChild(criarLinhaItem(item)));
  atualizarTotal();
}

function criarLinhaItem(item) {
  const pizza = ehPizza(item.produto);
  const info = el('div', { className: 'item-pedido__info' }, el('h3', { 'data-nome-item': item.uid, text: nomeItemCompleto(item) }));

  if (pizza) {
    info.appendChild(criarControleTamanho(item));
    if (item.meioAMeio) info.appendChild(criarSeletorSegundoSabor(item));
  }

  info.appendChild(criarCampoObservacao(item));
  if (pizza) info.appendChild(criarAcrescimos(item));

  const preparo = el('div', { className: 'item-pedido__preparo' });
  if (pizza) preparo.appendChild(criarControlePreparo(item));
  preparo.appendChild(criarControleQuantidade(item));

  const precos = el(
    'div',
    { className: 'item-pedido__precos' },
    el('div', { 'data-subtotal-item': item.uid, text: valorUnitarioItem(item) == null ? 'A configurar' : formatarMoeda(subtotalItem(item)) }),
    el('div', { className: 'item-pedido__detalhe-preco', 'data-detalhe-item': item.uid, text: textoDetalhePreco(item) }),
    el('button', {
      type: 'button',
      className: 'item-pedido__remover',
      text: 'remover item',
      onClick: () => removerItem(item.uid),
    })
  );

  const classeImagem = !pizza ? 'item-pedido__imagem--produto'
    : item.meioAMeio ? 'item-pedido__imagem--meio' : '';
  return el('div', { className: 'item-pedido', 'data-uid': item.uid },
    el('div', { className: `item-pedido__imagem ${classeImagem}`.trim(), 'aria-hidden': 'true' }),
    info, preparo, precos);
}

function textoDetalhePreco(item) {
  const preco = valorUnitarioItem(item);
  if (preco == null) return 'Preço das pizzas pendente de configuração';
  const partes = [`${item.quantidade} × ${formatarMoeda(preco)}`];
  if (item.acrescimos.length) partes.push(`inclui ${item.acrescimos.length} × ${formatarMoeda(VALOR_ACRESCIMO)} de acréscimo`);
  return partes.join(' · ');
}

function atualizarLinha(item) {
  const nome = document.querySelector(`[data-nome-item="${item.uid}"]`);
  const subtotal = document.querySelector(`[data-subtotal-item="${item.uid}"]`);
  const detalhe = document.querySelector(`[data-detalhe-item="${item.uid}"]`);
  if (nome) nome.textContent = nomeItemCompleto(item);
  if (subtotal) subtotal.textContent = valorUnitarioItem(item) == null ? 'A configurar' : formatarMoeda(subtotalItem(item));
  if (detalhe) detalhe.textContent = textoDetalhePreco(item);
  atualizarTotal();
}

/* ---------- Pizza inteira / meio a meio ---------- */

function criarControleTamanho(item) {
  const nome = `tamanho-${item.uid}`;
  const opcao = (valor, rotulo, marcado) =>
    el(
      'label',
      { className: 'opcao-radio' },
      el('input', {
        type: 'radio',
        name: nome,
        value: valor,
        checked: marcado,
        onChange: () => {
          item.meioAMeio = valor === 'meio';
          if (!item.meioAMeio) item.segundoProduto = null;
          renderizarItens();
        },
      }),
      rotulo
    );

  return el('div', { className: 'item-pedido__linha', role: 'radiogroup', 'aria-label': 'Tipo de pizza' }, opcao('inteira', 'Inteira', !item.meioAMeio), opcao('meio', 'Meio a meio', item.meioAMeio));
}

function criarSeletorSegundoSabor(item) {
  const select = el('select', {
    'aria-label': 'Segundo sabor',
    onChange: () => {
      item.segundoProduto = catalogoProdutos.find((p) => String(p.id) === select.value) || null;
      select.classList.remove('campo-invalido');
      atualizarLinha(item);
    },
  });

  select.appendChild(el('option', { value: '', text: 'Escolha o 2º sabor' }));
  catalogoProdutos
    .filter((p) => ehPizza(p) && String(p.id) !== String(item.produto.id))
    .forEach((p) => {
      select.appendChild(el('option', { value: p.id, text: nomeExibicaoProduto(p), selected: item.segundoProduto && String(item.segundoProduto.id) === String(p.id) }));
    });

  return el('div', { className: 'item-pedido__linha' }, el('span', { text: '2º sabor:' }), select);
}

/* ---------- Assada / pré-assada ---------- */

function criarControlePreparo(item) {
  const select = el(
    'select',
    {
      'aria-label': 'Tipo de preparo',
      onChange: () => {
        item.tipoPreparo = select.value;
        const alt = select.parentElement.querySelector('.item-pedido__preparo-alt');
        if (alt) alt.textContent = item.tipoPreparo === 'ASSADA' ? 'sai pronta (assada)' : 'sai para terminar em casa (pré-assada)';
        atualizarLinha(item);
      },
    },
    el('option', { value: 'ASSADA', text: 'Assada', selected: item.tipoPreparo === 'ASSADA' }),
    el('option', { value: 'PRE_ASSADA', text: 'Pré-assada', selected: item.tipoPreparo === 'PRE_ASSADA' })
  );

  return el(
    'div',
    { className: 'item-pedido__bloco' },
    select,
    el('div', { className: 'item-pedido__preparo-alt', text: item.tipoPreparo === 'ASSADA' ? 'sai pronta (assada)' : 'sai para terminar em casa (pré-assada)' })
  );
}

/* ---------- Quantidade ---------- */

function criarControleQuantidade(item) {
  const campo = el('input', {
    type: 'number',
    min: QUANTIDADE_MIN,
    max: QUANTIDADE_MAX,
    step: 1,
    value: item.quantidade,
    'aria-label': 'Quantidade',
    className: 'quantidade__campo',
  });

  const aplicar = (valor) => {
    let numero = Math.trunc(Number(valor));
    if (!Number.isFinite(numero)) numero = QUANTIDADE_MIN;
    item.quantidade = Math.min(QUANTIDADE_MAX, Math.max(QUANTIDADE_MIN, numero));
    campo.value = item.quantidade;
    atualizarLinha(item);
  };

  campo.addEventListener('change', () => aplicar(campo.value));
  campo.addEventListener('input', () => {
    const numero = Number(campo.value);
    if (Number.isInteger(numero) && numero >= QUANTIDADE_MIN && numero <= QUANTIDADE_MAX) {
      item.quantidade = numero;
      atualizarLinha(item);
    }
  });

  return el(
    'div',
    { className: 'quantidade' },
    el('span', { text: 'Qtd:' }),
    el('button', { type: 'button', className: 'quantidade__botao', 'aria-label': 'Diminuir quantidade', text: '−', onClick: () => aplicar(item.quantidade - 1) }),
    campo,
    el('button', { type: 'button', className: 'quantidade__botao', 'aria-label': 'Aumentar quantidade', text: '+', onClick: () => aplicar(item.quantidade + 1) })
  );
}

/* ---------- Observação (texto livre com sugestões) ---------- */

function criarCampoObservacao(item) {
  const idLista = `sugestoes-${item.uid}`;
  const campo = el('input', {
    type: 'text',
    list: idLista,
    maxlength: 120,
    placeholder: 'ex.: sem cebola',
    value: item.observacao,
    'aria-label': 'Observação do item',
    className: 'observacao__campo',
    onInput: () => {
      item.observacao = campo.value;
    },
  });
  const lista = el('datalist', { id: idLista }, SUGESTOES_OBSERVACAO.map((s) => el('option', { value: s })));
  return el('div', { className: 'item-pedido__linha' }, el('span', { text: 'obs:' }), campo, lista);
}

/* ---------- Acréscimos (R$ 5,00 por ingrediente) ---------- */

function criarAcrescimos(item) {
  const campo = el('input', {
    type: 'text',
    maxlength: 40,
    placeholder: 'ingrediente extra',
    'aria-label': 'Ingrediente de acréscimo',
    className: 'acrescimo__campo',
  });

  const adicionar = () => {
    const nome = campo.value.trim().toUpperCase();
    if (!nome) return;
    if (item.acrescimos.includes(nome)) {
      campo.value = '';
      return;
    }
    item.acrescimos.push(nome);
    renderizarItens();

    document.querySelector(`.item-pedido[data-uid="${item.uid}"] .acrescimo__campo`)?.focus();
  };

  campo.addEventListener('keydown', (evento) => {
    if (evento.key === 'Enter') {
      evento.preventDefault();
      adicionar();
    }
  });

  const tags = item.acrescimos.map((nome, indice) =>
    el(
      'span',
      { className: 'item-pedido__acrescimo-tag' },
      `ACRÉSCIMO: ${nome} (${formatarMoeda(VALOR_ACRESCIMO)}) `,
      el('button', {
        type: 'button',
        'aria-label': `Remover acréscimo ${nome}`,
        className: 'acrescimo__remover',
        text: '✕',
        onClick: () => {
          item.acrescimos.splice(indice, 1);
          renderizarItens();
        },
      })
    )
  );

  return el(
    'div',
    { className: 'item-pedido__linha' },
    campo,
    el('button', { type: 'button', className: 'botao--link acrescimo__adicionar', text: `+ acréscimo (${formatarMoeda(VALOR_ACRESCIMO)})`, onClick: adicionar }),
    tags
  );
}

/* ---------- Modalidade e total ---------- */

function configurarModalidade() {
  document.getElementById('selectModalidade').addEventListener('change', atualizarCamposModalidade);
  const cep = document.getElementById('endCep');
  cep.addEventListener('blur', async () => {
    const digitado = somenteDigitos(cep.value);
    if (!digitado) return;
    if (digitado.length !== 8) {
      mostrarErro('O CEP deve conter 8 dígitos. Você também pode preencher o endereço manualmente.');
      return;
    }
    const endereco = await consultarCep(digitado);

    if (!endereco || somenteDigitos(cep.value) !== digitado) return;
    [['endRua', endereco.rua], ['endBairro', endereco.bairro], ['endCidade', endereco.cidade]]
      .forEach(([id, valor]) => { if (valor) document.getElementById(id).value = valor; });
  });
  atualizarCamposModalidade();
}

function atualizarCamposModalidade() {
  const entrega = document.getElementById('selectModalidade').value === 'ENTREGA';
  document.getElementById('camposEntrega').style.display = entrega ? 'grid' : 'none';
  document.getElementById('camposRetirada').style.display = entrega ? 'none' : 'grid';
  atualizarTotal();
}

function calcularSubtotal() {
  return itensPedido.reduce((soma, item) => soma + subtotalItem(item), 0);
}

function calcularTotal() {
  const subtotal = calcularSubtotal();
  return subtotal + (document.getElementById('selectModalidade').value === 'ENTREGA' ? (taxaEntrega || 0) : 0);
}

function atualizarTotal() {
  const entrega = document.getElementById('selectModalidade').value === 'ENTREGA';
  const subtotal = calcularSubtotal();
  document.getElementById('valorSubtotal').textContent = formatarMoeda(subtotal);
  const taxaTexto = entrega && taxaEntrega === null ? 'A configurar' : formatarMoeda(entrega ? taxaEntrega : 0);
  document.getElementById('valorTaxaEntrega').textContent = `Taxa: ${taxaTexto}`;
  document.getElementById('valorTaxaResumo').textContent = taxaTexto;
  document.getElementById('valorTotal').textContent = entrega && taxaEntrega === null
    ? 'A configurar' : formatarMoeda(calcularTotal());
  return calcularTotal();
}

/* ---------- Ações: novo / cancelar / imprimir ---------- */

function reiniciarPedido() {
  pedidoEmEdicaoId = null;
  clienteSelecionado = null;
  itensPedido = [];
  proximoUid = 1;
  document.getElementById('nomeCliente').textContent = 'Cliente não selecionado';
  document.getElementById('telefoneContato').value = '';
  document.getElementById('retiradaNome').value = '';
  preencherEndereco({});
  atualizarNumeroPedido();
  document.getElementById('observacaoGeral').value = '';
  document.getElementById('selectPagamento').value = 'DINHEIRO';
  document.getElementById('selectModalidade').value = 'ENTREGA';
  atualizarCamposModalidade();
  window.history.replaceState({}, '', 'pedidos.html');
  esconderMensagens();
  renderizarItens();
}

function cancelarPedidoAtual() {
  if (itensPedido.length === 0 && !clienteSelecionado) return;
  if (confirm(pedidoEmEdicaoId
    ? 'Descartar alterações desta tela? O pedido salvo no servidor continuará em aberto.'
    : 'Descartar os itens deste pedido ainda não salvo?')) reiniciarPedido();
}

function validarPedido() {
  if (!clienteSelecionado) return 'Selecione um cliente antes de salvar o pedido.';
  if (itensPedido.length === 0) return 'Adicione pelo menos um item ao pedido.';
  const telefone = somenteDigitos(document.getElementById('telefoneContato').value);
  if (!/^\d{10,11}$/.test(telefone)) return 'Informe um telefone de contato com DDD (10 ou 11 dígitos).';
  if (document.getElementById('selectModalidade').value === 'ENTREGA') {
    if (taxaEntrega === null) return 'A taxa de entrega ainda não foi configurada. Defina TAXA_ENTREGA no backend.';
    const end = lerEndereco();
    if (!end.rua || !end.numero || !end.bairro || !end.cidade) return 'Para entrega, informe rua, número, bairro e cidade.';
    if (end.cep && somenteDigitos(end.cep).length !== 8) return 'Informe um CEP válido ou deixe o campo vazio.';
  }
  for (const item of itensPedido) {
    const nome = nomeExibicaoProduto(item.produto);
    if (!Number.isInteger(item.quantidade) || item.quantidade < QUANTIDADE_MIN || item.quantidade > QUANTIDADE_MAX) {
      return `Quantidade inválida em "${nome}": use um valor de ${QUANTIDADE_MIN} a ${QUANTIDADE_MAX}.`;
    }
    if (ehPizza(item.produto) && valorUnitarioItem(item) == null) {
      return 'O preço da pizza não está configurado. Defina PIZZA_PRECO_BASE e, se necessário, PIZZA_PRECO_PRE_ASSADA.';
    }
    if (item.meioAMeio && !item.segundoProduto) {
      document.querySelector(`.item-pedido[data-uid="${item.uid}"] select[aria-label="Segundo sabor"]`)?.classList.add('campo-invalido');
      return `Escolha o 2º sabor da pizza meio a meio "${nome}".`;
    }
  }
  return null;
}

function montarPayloadPedido() {
  const modalidade = document.getElementById('selectModalidade').value;
  return {
    clienteId: clienteSelecionado.id,
    modalidade,
    enderecoEntrega: modalidade === 'ENTREGA' ? lerEndereco() : null,
    telefoneContato: somenteDigitos(document.getElementById('telefoneContato').value),
    nomeRetirada: modalidade === 'RETIRADA' ? document.getElementById('retiradaNome').value.trim() : null,
    formaPagamento: document.getElementById('selectPagamento').value,
    observacaoGeral: document.getElementById('observacaoGeral').value.trim(),
    itens: itensPedido.map((item) => {
      const pizza = ehPizza(item.produto);
      const dto = {
        produtoId: item.produto.id,
        meioAMeio: pizza && item.meioAMeio,
        quantidade: item.quantidade,
      };
      if (pizza && item.meioAMeio) dto.segundoProdutoId = item.segundoProduto.id;
      if (pizza) dto.tipoPreparo = item.tipoPreparo;
      const observacao = item.observacao.trim();
      if (observacao) dto.observacao = observacao;
      if (pizza && item.acrescimos.length) dto.acrescimos = [...item.acrescimos];
      if (item.ingredientesRemovidos?.length) dto.ingredientesRemovidos = [...item.ingredientesRemovidos];
      return dto;
    }),
  };
}

/* ---------- Salvar e finalizar pedido ---------- */
async function persistirPedidoAtual() {
  if (enviandoPedido) return null;
  esconderMensagens();
  const problema = validarPedido();
  if (problema) { mostrarErro(problema); return null; }
  enviandoPedido = true;
  document.querySelectorAll('#botaoSalvarAberto, #botaoImprimir').forEach((b) => b.disabled = true);
  try {
    const resultado = pedidoEmEdicaoId
      ? await atualizarPedido(pedidoEmEdicaoId, montarPayloadPedido())
      : await criarPedido(montarPayloadPedido());
    pedidoEmEdicaoId = resultado.id;
    document.getElementById('numeroPedido').textContent = `pedido ${resultado.id}`;
    window.history.replaceState({}, '', `pedidos.html?pedido=${encodeURIComponent(resultado.id)}`);

    const totalServidor = Number(resultado.total);
    if (!Number.isFinite(totalServidor)) throw new Error('O servidor não devolveu o total do pedido.');
    const totalTela = calcularTotal();
    if (Math.abs(totalServidor - totalTela) > 0.005) {
      await Promise.all([carregarPrecosPizza(), carregarTaxaEntrega()]);
      mostrarErro(`O servidor calculou ${formatarMoeda(totalServidor)} e a tela mostrava ${formatarMoeda(totalTela)}. Verifique o valor atualizado antes de finalizar.`);
      return null;
    }
    return resultado;
  } catch (erro) {
    if (erro.status === 401) { sessaoExpirada(); return null; }
    mostrarErro(`Não foi possível salvar o pedido: ${erro.message}`);
    return null;
  } finally {
    enviandoPedido = false;
    document.querySelectorAll('#botaoSalvarAberto, #botaoImprimir').forEach((b) => b.disabled = false);
  }
}

async function salvarComoAberto() {
  const pedido = await persistirPedidoAtual();
  if (pedido) mostrarSucesso(`Pedido ${pedido.id} salvo em aberto. Você pode reabri-lo em "Pedidos anteriores".`);
}

/* ---------- Popup de finalização ---------- */
function abrirPopup(titulo, descricao, opcoes) {
  const popup = document.getElementById('popupPedido');
  document.getElementById('popupTitulo').textContent = titulo;
  document.getElementById('popupDescricao').textContent = descricao;
  const acoes = document.getElementById('popupAcoes');
  acoes.replaceChildren();
  opcoes.forEach((opcao) => {
    acoes.appendChild(el('button', {
      type: 'button', className: `botao ${opcao.classe || 'botao--contorno'}`,
      text: opcao.rotulo, onClick: opcao.acao,
    }));
  });
  popup.hidden = false;
  popup.querySelector('button')?.focus();
}

function fecharPopup() {
  document.getElementById('popupPedido').hidden = true;
}

function abrirPopupDeFinalizacao() {
  if (enviandoPedido) return;
  esconderMensagens();
  const erro = validarPedido();
  if (erro) { mostrarErro(erro); return; }
  abrirPopup('Como deseja concluir o pedido?',
    'Você pode imprimir e finalizar, confirmar sem imprimir ou voltar para editar.', [
      { rotulo: 'Imprimir e finalizar', classe: 'botao--verde', acao: iniciarImpressao },
      { rotulo: 'Confirmar sem imprimir', classe: 'botao--preto', acao: confirmarSemImprimir },
      { rotulo: 'Voltar', acao: fecharPopup },
    ]);
}

async function confirmarSemImprimir() {
  fecharPopup();
  const pedido = await persistirPedidoAtual();
  if (pedido) await concluirPedido(pedido, false);
}

async function iniciarImpressao() {
  fecharPopup();
  const pedido = await persistirPedidoAtual();
  if (!pedido) return;
  construirComprovante(pedido);
  window.addEventListener('afterprint', () => {
    abrirPopup('A impressão foi concluída?',
      'O navegador não informa se você realmente imprimiu ou cancelou a janela. Confirme para finalizar este pedido.', [
        { rotulo: 'Sim, finalizar', classe: 'botao--verde', acao: async () => {
          fecharPopup();
          await concluirPedido(pedido, true);
        } },
        { rotulo: 'Não, manter em aberto', acao: () => {
          fecharPopup();
          mostrarSucesso(`Pedido ${pedido.id} continua em aberto para reimpressão ou edição.`);
        } },
      ]);
  }, { once: true });
  try { window.print(); }
  catch (erro) { mostrarErro(`Não foi possível abrir a impressão: ${erro.message}`); }
}

async function concluirPedido(pedidoSalvo, foiImpresso) {
  if (enviandoPedido) return;
  enviandoPedido = true;
  try {
    const finalizado = await finalizarPedido(pedidoSalvo.id);
    if (foiImpresso) salvarPedidoNoHistoricoLocal(registroHistorico(finalizado));
    reiniciarPedido();
    mostrarSucesso(`Pedido ${finalizado.id} finalizado com sucesso${foiImpresso ? ' e marcado como impresso' : ' sem impressão'}. Total: ${formatarMoeda(finalizado.total)}.`);
  } catch (erro) {
    if (erro.status === 401) return sessaoExpirada();
    mostrarErro(`Não foi possível finalizar: ${erro.message}. O pedido permanece disponível para consulta.`);
  } finally { enviandoPedido = false; }
}

/* ---------- Histórico local e comprovante de impressão ---------- */
function registroHistorico(pedido) {
  return {
    id: pedido.id, numero: `Pedido ${pedido.id}`, cliente: clienteSelecionado?.nome,
    modalidade: pedido.modalidade, total: pedido.total, atendente: nomeDoUsuarioLogado(),
    criadoEm: pedido.criadoEm, itens: (pedido.itens || []).map((item) => ({
      nome: item.meioAMeio ? `${item.nomeProduto} + ${item.segundoSabor}` : item.nomeProduto,
      quantidade: item.quantidade, preco: item.precoUnitario,
    })),
  };
}

function construirComprovante(pedido) {
  const destino = document.getElementById('comprovanteImpressao');
  destino.replaceChildren();
  const linha = (texto) => destino.appendChild(el('p', { text: texto }));
  destino.appendChild(el('h1', { text: 'CITY PIZZAS' }));
  linha(`Pedido ${pedido.id}`);
  linha(`Data: ${new Date(pedido.criadoEm).toLocaleString('pt-BR')}`);
  linha(`Atendente: ${nomeDoUsuarioLogado()}`);
  linha(`Cliente: ${clienteSelecionado?.nome || pedido.clienteId}`);
  linha(`Contato: ${formatarTelefone(pedido.telefoneContato)}`);
  destino.appendChild(el('hr'));
  (pedido.itens || []).forEach((item) => {
    const nome = item.meioAMeio
      ? `${item.nomeProduto} / ${item.segundoSabor}` : item.nomeProduto;
    linha(`${item.quantidade} × ${nome} — ${formatarMoeda(item.subtotal)}`);
    if (item.categoria?.toUpperCase() === 'PIZZA') linha(`Preparo: ${item.tipoPreparo === 'PRE_ASSADA' ? 'Pré-assada' : 'Assada'}`);
    if (item.acrescimos?.length) linha(`Acréscimos: ${item.acrescimos.join(', ')}`);
    if (item.ingredientesRemovidos?.length) linha(`Retirar: ${item.ingredientesRemovidos.join(', ')}`);
    if (item.observacao) linha(`Obs.: ${item.observacao}`);
  });
  destino.appendChild(el('hr'));
  linha(`Modalidade: ${pedido.modalidade === 'RETIRADA' ? 'Retirada' : 'Entrega'}`);
  if (pedido.modalidade === 'ENTREGA' && pedido.enderecoEntrega) {
    const e = pedido.enderecoEntrega;
    linha(`Endereço: ${e.rua}, ${e.numero} — ${e.bairro}, ${e.cidade}`);
    if (e.cep) linha(`CEP: ${e.cep}`);
    if (e.complemento) linha(`Complemento: ${e.complemento}`);
  }
  if (pedido.modalidade === 'RETIRADA' && pedido.nomeRetirada) linha(`Retirar: ${pedido.nomeRetirada}`);
  linha(`Pagamento: ${pedido.formaPagamento}`);
  if (pedido.observacaoGeral) linha(`Obs. gerais: ${pedido.observacaoGeral}`);
  destino.appendChild(el('hr'));
  linha(`Subtotal: ${formatarMoeda(pedido.subtotalItens)}`);
  linha(`Taxa: ${formatarMoeda(pedido.taxaEntrega)}`);
  destino.appendChild(el('p', { className: 'comprovante-total', text: `TOTAL: ${formatarMoeda(pedido.total)}` }));
}

function sessaoExpirada() {
  localStorageEncerrarSessao();
  alert('Sua sessão expirou. Faça login novamente.');
  window.location.href = 'index.html';
}

function salvarPedidoNoHistoricoLocal(pedido) {
  let historico = [];
  try {
    historico = JSON.parse(localStorage.getItem('cityPizzas_historicoPedidos') || '[]');
  } catch (_) { }
  historico.unshift(pedido);
  localStorage.setItem('cityPizzas_historicoPedidos', JSON.stringify(historico.slice(0, 50)));
}

/* ---------- Utilidades ---------- */

function numeroAtualPedido() {
  const atual = Number(localStorage.getItem('cityPizzas_ultimoPedido') || '0') + 1;
  return String(atual).padStart(3, '0');
}

function atualizarNumeroPedido() {
  document.getElementById('numeroPedido').textContent = `pedido ${numeroAtualPedido()}`;
}

function confirmarNumeroPedido() {
  const atual = Number(localStorage.getItem('cityPizzas_ultimoPedido') || '0') + 1;
  localStorage.setItem('cityPizzas_ultimoPedido', String(atual));
}

function formatarMoeda(valor) {
  return Number(valor).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

function mostrarErro(texto) {
  const el = document.getElementById('mensagemErro');
  el.textContent = texto;
  el.style.display = 'block';
  el.scrollIntoView({ block: 'nearest' });
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
