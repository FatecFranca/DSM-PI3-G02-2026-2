document.addEventListener('DOMContentLoaded', () => {
  if (usuarioEstaLogado()) {
    window.location.href = 'cadastros.html';
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
      const resultado = await login(matricula, senha);

      if (!resultado || !resultado.perfil) {
        throw new Error('O servidor retornou uma resposta de login incompleta.');
      }

      localStorageSalvarSessao(resultado);
      window.location.href = 'cadastros.html';
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
        await solicitarRecuperacaoSenha(email);
        mostrarInfo('Se o e-mail estiver cadastrado, você receberá um código.');
        etapaCodigo.style.display = 'block';
        botaoRecuperar.textContent = 'Redefinir senha';
        codigoJaSolicitado = true;
        campoCodigo.focus();
      } else {
        const codigo = campoCodigo.value.trim();
        const novaSenha = campoNovaSenha.value;
        if (!/^\d{6}$/.test(codigo) || novaSenha.length < 8) {
          mostrarErro('Informe o código e uma nova senha com pelo menos 8 caracteres.');
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
