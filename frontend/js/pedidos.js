let clienteSelecionado = null;
let itensPedido = [];
let proximoUid = 1;
let catalogoProdutos = [];
let enviandoPedido = false;
let precosPizza = { precoBase: null, precoMeioAMeio: null };

const VALOR_ACRESCIMO = 5.0;
const QUANTIDADE_MIN = 1;
const QUANTIDADE_MAX = 100;
const SUGESTOES_OBSERVACAO = ['SEM MILHO', 'SEM CEBOLA', 'COM CEBOLA', 'SEM TOMATE', 'SEM AZEITONA', 'BORDA RECHEADA'];

document.addEventListener('DOMContentLoaded', async () => {
  if (!usuarioEstaLogado()) {
    window.location.href = 'index.html';
    return;
  }

  document.getElementById('nomeUsuario').textContent = `Olá, ${nomeDoUsuarioLogado()}`;
  document.getElementById('atendentePedido').textContent = `Atendente: ${nomeDoUsuarioLogado()}`;
  atualizarNumeroPedido();

  document.getElementById('linkSair').addEventListener('click', async (evento) => {
    evento.preventDefault();
    try { await encerrarSessaoNoServidor(); } catch (_) { /* rede indisponível */ }
    localStorageEncerrarSessao();
    window.location.href = 'index.html';
  });

  configurarBuscaCliente();
  restaurarClienteRecemCadastrado();
  await carregarPrecosPizza();
  await configurarBuscaProduto();
  configurarModalidade();

  document.getElementById('botaoNovoPedido').addEventListener('click', reiniciarPedido);
  document.getElementById('botaoCancelar').addEventListener('click', cancelarPedidoAtual);
  document.getElementById('botaoImprimir').addEventListener('click', finalizarEImprimir);

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
        clienteSelecionado = cliente;
        document.getElementById('nomeCliente').textContent = clienteSelecionado.nome;
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
    clienteSelecionado = JSON.parse(bruto);
    document.getElementById('nomeCliente').textContent = clienteSelecionado.nome;
  } catch (_) {
    clienteSelecionado = null;
  }
}

/* ---------- Catálogo e busca de produtos ---------- */

function ehPizza(produto) {
  return String(produto?.categoria || '').trim().toUpperCase() === 'PIZZA';
}

async function carregarPrecosPizza() {
  try {
    const precos = await consultarPrecosPizza();
    precosPizza.precoBase = Number(precos.precoBase);
    precosPizza.precoMeioAMeio = Number(precos.precoMeioAMeio);
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

/** Usa os preços oficiais configurados no back-end, nunca a média dos sabores. */
function valorUnitarioItem(item) {
  let preco = precoDoProduto(item.produto);
  if (ehPizza(item.produto)) {
    preco = item.meioAMeio ? precosPizza.precoMeioAMeio : precosPizza.precoBase;
    if (!Number.isFinite(preco) || preco === null) return null;
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

/* --- Pizza inteira / meio a meio --- */

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

/* --- Assada / pré-assada --- */

function criarControlePreparo(item) {
  const select = el(
    'select',
    {
      'aria-label': 'Tipo de preparo',
      onChange: () => {
        item.tipoPreparo = select.value;
        const alt = select.parentElement.querySelector('.item-pedido__preparo-alt');
        if (alt) alt.textContent = item.tipoPreparo === 'ASSADA' ? 'sai pronta (assada)' : 'sai para terminar em casa (pré-assada)';
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

/* --- Quantidade --- */

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

/* --- Observação (texto livre com sugestões) --- */

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

/* --- Acréscimos (R$ 5,00 por ingrediente) --- */

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
    // Mantém o foco para adicionar vários ingredientes seguidos.
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

/* ---------- Modalidade (apenas visual até a Sprint 4) ---------- */

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
  return itensPedido.reduce((soma, item) => soma + subtotalItem(item), 0);
}

function atualizarTotal() {
  const total = calcularTotal();
  document.getElementById('valorTotal').textContent = formatarMoeda(total);
  return total;
}

/* ---------- Ações: novo / cancelar / imprimir ---------- */

function reiniciarPedido() {
  clienteSelecionado = null;
  itensPedido = [];
  document.getElementById('nomeCliente').textContent = 'Cliente não selecionado';
  atualizarNumeroPedido();
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

function validarPedido() {
  if (!clienteSelecionado) return 'Selecione um cliente antes de finalizar o pedido.';
  if (itensPedido.length === 0) return 'Adicione pelo menos um item ao pedido.';

  for (const item of itensPedido) {
    const nome = nomeExibicaoProduto(item.produto);
    if (!Number.isInteger(item.quantidade) || item.quantidade < QUANTIDADE_MIN || item.quantidade > QUANTIDADE_MAX) {
      return `Quantidade inválida em "${nome}": use um valor de ${QUANTIDADE_MIN} a ${QUANTIDADE_MAX}.`;
    }
    if (ehPizza(item.produto) && valorUnitarioItem(item) == null) {
      return 'O preço da pizza não está configurado no servidor. Configure PIZZA_PRECO_BASE (e, se necessário, PIZZA_PRECO_MEIO_A_MEIO).';
    }
    if (item.meioAMeio && !item.segundoProduto) {
      document.querySelector(`.item-pedido[data-uid="${item.uid}"] select[aria-label="Segundo sabor"]`)?.classList.add('campo-invalido');
      return `Escolha o 2º sabor da pizza meio a meio "${nome}".`;
    }
  }
  return null;
}

function montarPayloadPedido() {
  return {
    clienteId: clienteSelecionado.id,
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
      return dto;
    }),
  };
}

async function finalizarEImprimir() {
  if (enviandoPedido) return;
  esconderMensagens();

  const problema = validarPedido();
  if (problema) {
    mostrarErro(problema);
    return;
  }

  const botao = document.getElementById('botaoImprimir');
  enviandoPedido = true;
  botao.disabled = true;

  let pedidoSalvo;
  try {
    pedidoSalvo = await criarPedido(montarPayloadPedido());
  } catch (erro) {
    enviandoPedido = false;
    botao.disabled = false;
    if (erro.status === 401) return sessaoExpirada();
    // Impede emitir um pedido que o servidor não registrou.
    mostrarErro(`Pedido não registrado (${erro.message}). A impressão foi cancelada.`);
    return;
  }

  // Guarda o que foi enviado antes de reiniciar a tela.
  const modalidade = document.getElementById('selectModalidade').value;
  const registroLocal = {
    id: pedidoSalvo?.id,
    numero: document.getElementById('numeroPedido').textContent,
    cliente: clienteSelecionado.nome,
    itens: itensPedido.map((item) => ({
      nome: nomeItemCompleto(item),
      quantidade: item.quantidade,
      preparo: ehPizza(item.produto) ? (item.tipoPreparo === 'ASSADA' ? 'Assada' : 'Pré-assada') : '',
      observacao: item.observacao.trim(),
      acrescimos: [...item.acrescimos],
      preco: valorUnitarioItem(item),
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
    total: calcularTotal(),
    atendente: localStorageObterUsuario()?.nome || nomeDoUsuarioLogado(),
    criadoEm: pedidoSalvo?.criadoEm || new Date().toISOString(),
  };

  const subtotalServidor = Number(pedidoSalvo?.subtotalItens);
  confirmarNumeroPedido();
  salvarPedidoNoHistoricoLocal(registroLocal);
  window.print();

  reiniciarPedido();
  enviandoPedido = false;
  botao.disabled = false;

  let texto = `${registroLocal.numero.replace(/^pedido/, 'Pedido')} registrado com sucesso.`;
  if (Number.isFinite(subtotalServidor)) {
    texto += ` Subtotal confirmado pelo servidor: ${formatarMoeda(subtotalServidor)}.`;
    if (Math.abs(subtotalServidor - registroLocal.total) > 0.005) {
      texto += ` Atenção: diverge do valor exibido na impressão (${formatarMoeda(registroLocal.total)}).`;
    }
  }
  mostrarSucesso(texto);
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
