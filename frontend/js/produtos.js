/** PB07/PB08: manutenção de produtos com apresentações diferentes para bebidas. */
function configurarCadastroProdutos() {
  const form = document.getElementById('formProduto');
  const mensagem = document.getElementById('mensagemProduto');
  const lista = document.getElementById('listaProdutosCadastro');
  const categoria = document.getElementById('produtoCategoria');
  const grupoVolume = document.getElementById('campoVolumeProduto');
  const volume = document.getElementById('produtoVolume');
  const grupoSabor = document.getElementById('campoSaborProduto');
  const sabor = document.getElementById('produtoSabor');
  const salvar = document.getElementById('botaoSalvarProduto');
  const campoBusca = document.getElementById('buscaProdutoCadastro');
  let produtos = [];

  function aviso(texto, erro = false) {
    mensagem.textContent = texto;
    mensagem.className = `mensagem ${erro ? 'mensagem--erro' : 'mensagem--sucesso'}`;
    mensagem.style.display = 'block';
  }
  function ajustarCategoria() {
    const bebida = categoria.value === 'BEBIDA';
    const pizza = categoria.value === 'PIZZA';
    grupoVolume.style.display = bebida ? 'block' : 'none';
    grupoSabor.style.display = pizza ? 'block' : 'none';
    volume.required = bebida;
  }
  function limpar() {
    form.reset();
    document.getElementById('produtoId').value = '';
    salvar.textContent = 'Cadastrar produto';
    ajustarCategoria();
  }
  function detalhesProduto(p) {
    const detalhes = [p.categoria];
    if (p.apresentacaoVolume) detalhes.push(p.apresentacaoVolume);
    if (p.sabor) detalhes.push(`Sabor: ${p.sabor}`);
    detalhes.push(`Estoque: ${p.estoque}`);
    const preco = Number(p.preco);
    detalhes.push(`Preço cadastrado: ${Number.isFinite(preco) ? preco.toLocaleString('pt-BR', {style:'currency',currency:'BRL'}) : 'não informado'}`);
    return detalhes.join(' · ');
  }
  function botao(texto, acao) {
    const el = document.createElement('button');
    el.type = 'button';
    el.className = 'botao botao--contorno';
    el.textContent = texto;
    el.addEventListener('click', acao);
    return el;
  }
  function editar(p) {
    document.getElementById('produtoId').value = p.id;
    document.getElementById('produtoNome').value = p.nome || '';
    categoria.value = p.categoria || 'OUTRO';
    document.getElementById('produtoPreco').value = p.preco ?? '';
    document.getElementById('produtoEstoque').value = p.estoque ?? 0;
    volume.value = p.apresentacaoVolume || '';
    sabor.value = p.sabor || '';
    salvar.textContent = 'Salvar alterações';
    ajustarCategoria();
    aviso(`Editando ${p.nome}.`);
    form.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
  async function excluir(p) {
    if (!confirm(`Excluir ${p.nome}${p.apresentacaoVolume ? ` (${p.apresentacaoVolume})` : ''} do catálogo?`)) return;
    try {
      await excluirProduto(p.id);
      if (document.getElementById('produtoId').value === p.id) limpar();
      aviso('Produto excluído do catálogo.');
      await carregar();
    } catch (erro) { aviso(erro.message || 'Não foi possível excluir o produto.', true); }
  }
  function mostrar(produtosEncontrados) {
    lista.replaceChildren();
    if (!produtosEncontrados.length) {
      lista.textContent = 'Nenhum produto encontrado.';
      return;
    }
    produtosEncontrados.forEach(p => {
      const linha = document.createElement('div');
      linha.className = 'cadastro__resultado-produto';
      const dados = document.createElement('div');
      const titulo = document.createElement('strong');
      titulo.textContent = p.nome || 'Produto sem nome';
      const detalhe = document.createElement('p');
      detalhe.textContent = detalhesProduto(p);
      dados.append(titulo, detalhe);
      const acoes = document.createElement('div');
      acoes.className = 'cadastro__acoes-produto';
      acoes.append(botao('Editar', () => editar(p)), botao('Excluir', () => excluir(p)));
      linha.append(dados, acoes);
      lista.appendChild(linha);
    });
  }
  async function carregar(termo = campoBusca.value.trim()) {
    lista.textContent = 'Carregando catálogo...';
    try {
      produtos = termo ? await pesquisarProdutos(termo) : await listarProdutos();
      mostrar(Array.isArray(produtos) ? produtos : []);
    } catch (erro) {
      lista.textContent = erro.message || 'Erro ao carregar os produtos.';
    }
  }
  categoria.addEventListener('change', ajustarCategoria);
  document.getElementById('botaoLimparProduto').addEventListener('click', () => {
    limpar(); mensagem.style.display = 'none';
  });
  document.getElementById('formBuscaProdutoCadastro').addEventListener('submit', (event) => {
    event.preventDefault(); carregar();
  });
  campoBusca.addEventListener('input', () => {
    if (!campoBusca.value.trim()) carregar('');
  });
  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    const id = document.getElementById('produtoId').value;
    const nome = document.getElementById('produtoNome').value.trim();
    const precoTexto = document.getElementById('produtoPreco').value;
    const preco = Number(precoTexto);
    const estoqueTexto = document.getElementById('produtoEstoque').value;
    const estoque = Number(estoqueTexto);
    const apresentacaoVolume = volume.value.trim();
    if (!nome || !categoria.value || !precoTexto || !Number.isFinite(preco) || preco < 0 ||
        !/^\d+(?:\.\d{1,2})?$/.test(precoTexto) ||
        !Number.isInteger(estoque) || estoque < 0 || !estoqueTexto ||
        (categoria.value === 'BEBIDA' && !apresentacaoVolume)) {
      aviso('Preencha nome, categoria, preço válido, estoque inteiro e a apresentação/volume quando for bebida.', true);
      return;
    }
    const dados = {
      nome, categoria: categoria.value, preco,
      estoque, apresentacaoVolume: categoria.value === 'BEBIDA' ? apresentacaoVolume : null,
      sabor: categoria.value === 'PIZZA' ? (sabor.value.trim() || null) : null,
    };
    salvar.disabled = true;
    try {
      if (id) await atualizarProduto(id, dados);
      else await cadastrarProduto(dados);
      limpar();
      aviso(id ? 'Produto atualizado com sucesso.' : 'Produto cadastrado com sucesso.');
      await carregar('');
      campoBusca.value = '';
    } catch (erro) {
      aviso(erro.message || 'Não foi possível salvar o produto.', true);
    } finally { salvar.disabled = false; }
  });
  ajustarCategoria();
  carregar();
}
