/**
 * login.js — login de funcionário (matrícula + senha) e recuperação de senha.
 * Integra com POST /auth/login, /auth/recuperar-senha e /auth/redefinir-senha.
 */

document.addEventListener('DOMContentLoaded', () => {
  if (usuarioEstaLogado()) {
    window.location.href = 'pedidos.html';
    return;
  }

  const formLogin = document.getElementById('formLogin');
  const formRecuperar = document.getElementById('formRecuperar');
  const campoUsuario = document.getElementById('usuario');
  const campoSenha = document.getElementById('senha');
  const erroUsuario = document.getElementById('erroUsuario');
  const erroSenha = document.getElementById('erroSenha');
  const mensagemErro = document.getElementById('mensagemErro');
  const mensagemInfo = document.getElementById('mensagemInfo');
  const botaoEntrar = document.getElementById('botaoEntrar');

  /* ---------- Login ---------- */

  formLogin.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    esconderMensagens();

    const matricula = campoUsuario.value.trim();
    const senha = campoSenha.value;
    let valido = true;

    if (!matricula) {
      erroUsuario.style.display = 'block';
      valido = false;
    }
    if (!senha) {
      erroSenha.style.display = 'block';
      valido = false;
    }
    if (!valido) return;

    alternarCarregando(botaoEntrar, true, 'Entrando...', 'Entrar');

    try {
      // POST /auth/login -> { matricula, perfil }
      const resultado = await login(matricula, senha);

      if (!resultado || !resultado.perfil) {
        // O LoginResponse do back-end precisa de getters sem parâmetro para o JSON sair completo.
        throw new Error('Resposta de login incompleta. Avise o back-end (LoginResponse sem getters).');
      }

      localStorageSalvarSessao(resultado);
      window.location.href = 'pedidos.html';
    } catch (erro) {
      mostrarErro(erro.message || 'Matrícula ou senha incorreta.');
    } finally {
      alternarCarregando(botaoEntrar, false, 'Entrando...', 'Entrar');
    }
  });

  /* ---------- Recuperação de senha ---------- */

  const campoEmail = document.getElementById('emailRecuperacao');
  const campoCodigo = document.getElementById('codigoRecuperacao');
  const campoNovaSenha = document.getElementById('novaSenha');
  const etapaCodigo = document.getElementById('etapaCodigo');
  const botaoRecuperar = document.getElementById('botaoRecuperar');
  let codigoJaSolicitado = false;

  document.getElementById('linkEsqueciSenha').addEventListener('click', (evento) => {
    evento.preventDefault();
    esconderMensagens();
    formLogin.style.display = 'none';
    formRecuperar.style.display = 'block';
    campoEmail.focus();
  });

  document.getElementById('linkVoltarLogin').addEventListener('click', (evento) => {
    evento.preventDefault();
    esconderMensagens();
    formRecuperar.style.display = 'none';
    formLogin.style.display = 'block';
    etapaCodigo.style.display = 'none';
    codigoJaSolicitado = false;
    botaoRecuperar.textContent = 'Enviar código';
  });

  formRecuperar.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    esconderMensagens();

    const email = campoEmail.value.trim();
    if (!email.includes('@')) {
      mostrarErro('Informe o e-mail cadastrado do colaborador.');
      return;
    }

    try {
      botaoRecuperar.disabled = true;

      if (!codigoJaSolicitado) {
        // Etapa 1 — POST /auth/recuperar-senha { email }
        // Hoje o back-end devolve o próprio código no texto da resposta (não envia e-mail).
        const resposta = await solicitarRecuperacaoSenha(email);
        mostrarInfo(typeof resposta === 'string' ? resposta : 'Código gerado.');
        etapaCodigo.style.display = 'block';
        botaoRecuperar.textContent = 'Redefinir senha';
        codigoJaSolicitado = true;
        campoCodigo.focus();
      } else {
        // Etapa 2 — POST /auth/redefinir-senha { email, codigo, novaSenha }
        const codigo = campoCodigo.value.trim();
        const novaSenha = campoNovaSenha.value;
        if (!codigo || novaSenha.length < 6) {
          mostrarErro('Informe o código e uma nova senha com pelo menos 6 caracteres.');
          return;
        }
        await redefinirSenha(email, codigo, novaSenha);
        document.getElementById('linkVoltarLogin').click();
        mostrarInfo('Senha redefinida com sucesso. Entre com a nova senha.');
      }
    } catch (erro) {
      mostrarErro(erro.message || 'Não foi possível concluir a recuperação de senha.');
    } finally {
      botaoRecuperar.disabled = false;
    }
  });

  /* ---------- Modo de teste (TEMPORÁRIO) ---------- */

  document.getElementById('linkModoTeste').addEventListener('click', (evento) => {
    evento.preventDefault();
    localStorageSalvarSessao({
      matricula: 'teste',
      nome: 'Atendente Teste',
      perfil: 'funcionario',
      modoTeste: true,
    });
    window.location.href = 'pedidos.html';
  });

  /* ---------- Utilidades ---------- */

  function mostrarErro(texto) {
    mensagemInfo.style.display = 'none';
    mensagemErro.textContent = texto;
    mensagemErro.style.display = 'block';
  }

  function mostrarInfo(texto) {
    mensagemErro.style.display = 'none';
    mensagemInfo.textContent = texto;
    mensagemInfo.style.display = 'block';
  }

  function esconderMensagens() {
    erroUsuario.style.display = 'none';
    erroSenha.style.display = 'none';
    mensagemErro.style.display = 'none';
    mensagemInfo.style.display = 'none';
  }

  function alternarCarregando(botao, carregando, textoCarregando, textoNormal) {
    botao.disabled = carregando;
    botao.textContent = carregando ? textoCarregando : textoNormal;
  }
});
