/**
 * cadastros.js — abas de cadastro e formulário de cliente.
 * Integra com POST /clientes (rota proposta; o ClienteController ainda não existe no back-end).
 *
 * Campos conforme o model Cliente.java:
 *   obrigatórios: nome, telefone, cpf | opcionais: email, endereco (um único TEXTO), dataNascimento
 */

document.addEventListener('DOMContentLoaded', () => {
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
      endereco: montarEndereco() || undefined,
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

  /** O back-end guarda endereço como UM texto: "Rua, nº — Bairro, CEP". */
  function montarEndereco() {
    const rua = campos.rua.value.trim();
    const numero = campos.numero.value.trim();
    const bairro = campos.bairro.value.trim();
    const cep = campos.cep.value.trim();

    const ruaENumero = [rua, numero].filter(Boolean).join(', ');
    const partes = [ruaENumero, bairro].filter(Boolean).join(' — ');
    return [partes, cep && `CEP ${cep}`].filter(Boolean).join(', ');
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
    if (campos.email.value.trim() && !campos.email.value.includes('@')) {
      document.getElementById('erroEmail').style.display = 'block';
      valido = false;
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

    if (!campoRua.value) campoRua.value = dados.rua || '';
    if (!campoBairro.value) campoBairro.value = dados.bairro || '';
  });
}
