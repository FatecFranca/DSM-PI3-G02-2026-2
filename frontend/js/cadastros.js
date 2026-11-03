/**
 * cadastros.js — busca/cadastro de clientes e administração de colaboradores.
 * Sprint 2 (PB01 a PB06). Mantém a estrutura de abas do protótipo.
 */

document.addEventListener('DOMContentLoaded', () => {
  if (!usuarioEstaLogado()) {
    window.location.href = 'index.html';
    return;
  }
  configurarSaida();
  configurarBuscaClientes();
  configurarCadastroColaborador();
  configurarAbas();
  configurarFormularioCliente();
  configurarConsultaCep();
});

/* ---------- Abas ---------- */

function configurarAbas() {
  const botoesAba = document.querySelectorAll('.cadastro__aba');
  const conteudos = {
    cliente: document.getElementById('abaCliente'),
    colaborador: document.getElementById('abaColaborador'),
    produto: document.getElementById('abaProduto'),
  };

  botoesAba.forEach((botao) => {
    botao.addEventListener('click', () => {
      botoesAba.forEach((b) => b.classList.remove('ativa'));
      botao.classList.add('ativa');
      Object.values(conteudos).forEach((el) => (el.style.display = 'none'));
      conteudos[botao.dataset.aba].style.display = 'block';
    });
  });
}

/* ---------- Formulário de cliente ---------- */

function configurarFormularioCliente() {
  const formulario = document.getElementById('formCliente');
  const mensagemErro = document.getElementById('mensagemErro');
  const mensagemSucesso = document.getElementById('mensagemSucesso');
  const botaoCadastrar = document.getElementById('botaoCadastrar');

  const campos = {
    nome: document.getElementById('nome'),
    telefone: document.getElementById('telefone'),
    cpf: document.getElementById('cpf'),
    email: document.getElementById('email'),
    dataNascimento: document.getElementById('dataNascimento'),
    cep: document.getElementById('cep'),
    numero: document.getElementById('numero'),
    bairro: document.getElementById('bairro'),
    rua: document.getElementById('rua'),
  };

  // Veio da tela de pedidos (busca sem resultado)? Então volta pra lá com o cliente selecionado.
  const veioDoPedido = new URLSearchParams(window.location.search).get('origem') === 'pedidos';

  formulario.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    esconderMensagens();

    if (!validarFormulario()) return;

    alternarCarregando(true);

    const dadosCliente = {
      nome: campos.nome.value.trim(),
      telefone: somenteDigitos(campos.telefone.value), // guardado só com dígitos (busca por telefone é exata)
      cpf: somenteDigitos(campos.cpf.value),
      email: campos.email.value.trim() || undefined,
      dataNascimento: campos.dataNascimento.value || undefined, // yyyy-MM-dd (LocalDate)
      endereco: montarEndereco(),
    };

    try {
      // POST /clientes (rota proposta) -> cliente criado
      const criado = await cadastrarCliente(dadosCliente);

      if (veioDoPedido && criado && criado.id) {
        sessionStorage.setItem('cityPizzas_clienteSelecionado', JSON.stringify(criado));
        window.location.href = 'pedidos.html';
        return;
      }

      mensagemSucesso.textContent = 'Cliente cadastrado com sucesso.';
      mensagemSucesso.style.display = 'block';
      formulario.reset();
    } catch (erro) {
      mensagemErro.textContent = erro.message || 'Não foi possível cadastrar o cliente.';
      mensagemErro.style.display = 'block';
    } finally {
      alternarCarregando(false);
    }
  });

  /** O model Endereco.java persiste campos separados no MongoDB. */
  function montarEndereco() {
    return {
      cep: somenteDigitos(campos.cep.value),
      rua: campos.rua.value.trim(),
      numero: campos.numero.value.trim(),
      bairro: campos.bairro.value.trim(),
      cidade: document.getElementById('cidade').value.trim(),
    };
  }

  function validarFormulario() {
    let valido = true;

    if (!campos.nome.value.trim()) {
      document.getElementById('erroNome').style.display = 'block';
      valido = false;
    }
    const digitosTelefone = somenteDigitos(campos.telefone.value);
    if (digitosTelefone.length < 10 || digitosTelefone.length > 11) {
      document.getElementById('erroTelefone').style.display = 'block';
      valido = false;
    }
    if (!validarCpf(campos.cpf.value)) {
      document.getElementById('erroCpf').style.display = 'block';
      valido = false;
    }
    if (campos.email.value.trim() && !campos.email.validity.valid) {
      document.getElementById('erroEmail').style.display = 'block';
      valido = false;
    }

    for (const id of ['rua', 'numero', 'bairro']) {
      if (!campos[id].value.trim()) {
        document.getElementById('erroEndereco').style.display = 'block';
        valido = false;
      }
    }

    return valido;
  }

  function esconderMensagens() {
    mensagemErro.style.display = 'none';
    mensagemSucesso.style.display = 'none';
    document.querySelectorAll('.campo__erro').forEach((el) => (el.style.display = 'none'));
  }

  function alternarCarregando(carregando) {
    botaoCadastrar.disabled = carregando;
    botaoCadastrar.textContent = carregando ? 'Cadastrando...' : 'Cadastrar cliente';
  }
}

/* ---------- Preenchimento automático por CEP ---------- */

function configurarConsultaCep() {
  const campoCep = document.getElementById('cep');

  campoCep.addEventListener('blur', async () => {
    const dados = await consultarCep(campoCep.value);
    if (!dados) return;
    const campoRua = document.getElementById('rua');
    const campoBairro = document.getElementById('bairro');
    const campoCidade = document.getElementById('cidade');

    if (!campoRua.value) campoRua.value = dados.rua || '';
    if (!campoBairro.value) campoBairro.value = dados.bairro || '';
    if (!campoCidade.value) campoCidade.value = dados.cidade || '';
  });
}

/* ---------- Busca de clientes (PB05) ---------- */
function configurarBuscaClientes() {
  const formulario = document.getElementById('formBuscaCliente');
  const resultados = document.getElementById('listaClientesEncontrados');
  formulario.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    const termo = document.getElementById('buscaCliente').value.trim();
    if (termo.length < 2) {
      resultados.textContent = 'Digite pelo menos dois caracteres.';
      return;
    }
    resultados.textContent = 'Pesquisando...';
    try {
      const clientes = await buscarClientes(termo);
      resultados.replaceChildren();
      if (!clientes.length) {
        resultados.textContent = 'Nenhum cliente encontrado. Use o cadastro abaixo.';
        return;
      }
      clientes.forEach((cliente) => {
        const item = document.createElement('div');
        item.className = 'cadastro__resultado';
        const texto = document.createElement('span');
        texto.textContent = `${cliente.nome} — ${formatarTelefone(cliente.telefone)}`;
        const botao = document.createElement('button');
        botao.type = 'button';
        botao.className = 'botao botao--contorno';
        botao.textContent = 'Selecionar';
        botao.addEventListener('click', () => {
          sessionStorage.setItem('cityPizzas_clienteSelecionado', JSON.stringify(cliente));
          window.location.href = 'pedidos.html';
        });
        item.append(texto, botao);
        resultados.appendChild(item);
      });
    } catch (erro) {
      resultados.textContent = erro.message || 'Não foi possível buscar clientes.';
    }
  });
}

/* ---------- Colaboradores (PB03, PB04; administrador) ---------- */
function configurarCadastroColaborador() {
  const aba = document.querySelector('[data-aba="colaborador"]');
  if (!usuarioEhAdministrador()) {
    aba.style.display = 'none';
    return;
  }

  document.getElementById('bloqueioColaborador').style.display = 'none';
  document.getElementById('conteudoColaboradorAdmin').style.display = 'block';
  const mensagem = document.getElementById('mensagemColaborador');
  const form = document.getElementById('formColaborador');
  const formSenha = document.getElementById('formSenhaAdmin');
  const seletor = document.getElementById('colaboradorSenhaId');

  async function atualizarLista() {
    const colaboradores = await listarColaboradores();
    seletor.replaceChildren();
    colaboradores.forEach((colaborador) => {
      const opcao = document.createElement('option');
      opcao.value = colaborador.id;
      opcao.textContent = `${colaborador.nome} (${colaborador.matricula})`;
      seletor.appendChild(opcao);
    });
  }

  form.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    mensagem.textContent = '';
    const dados = {
      nome: document.getElementById('colNome').value.trim(),
      matricula: document.getElementById('colMatricula').value.trim(),
      email: document.getElementById('colEmail').value.trim(),
      senha: document.getElementById('colSenha').value,
      perfil: document.getElementById('colPerfil').value,
    };
    try {
      await cadastrarColaborador(dados);
      mensagem.textContent = 'Colaborador cadastrado com sucesso.';
      form.reset();
      await atualizarLista();
    } catch (erro) { mensagem.textContent = erro.message; }
  });

  formSenha.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    mensagem.textContent = '';
    const id = seletor.value;
    const senha = document.getElementById('senhaAdminNova').value;
    if (!id || senha.length < 8) {
      mensagem.textContent = 'Selecione um colaborador e informe uma senha de pelo menos 8 caracteres.';
      return;
    }
    try {
      await redefinirSenhaPeloAdministrador(id, senha);
      mensagem.textContent = 'Senha alterada com sucesso.';
      formSenha.reset();
    } catch (erro) { mensagem.textContent = erro.message; }
  });

  atualizarLista().catch((erro) => { mensagem.textContent = erro.message; });
}

function configurarSaida() {
  document.getElementById('linkSairCadastro').addEventListener('click', async (evento) => {
    evento.preventDefault();
    try { await encerrarSessaoNoServidor(); } catch (_) { /* rede indisponível */ }
    localStorageEncerrarSessao();
    window.location.href = 'index.html';
  });
}
