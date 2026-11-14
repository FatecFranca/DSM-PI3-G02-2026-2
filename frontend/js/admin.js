const LIMITE_RESULTADOS = 200;

document.addEventListener('DOMContentLoaded', async () => {
  if (!usuarioEstaLogado()) {
    window.location.href = 'index.html';
    return;
  }
  if (!usuarioEhAdministrador()) {
    window.location.replace('pedidos.html');
    return;
  }

  configurarCabecalho();

  let colaboradores;
  try {
    colaboradores = await listarColaboradores();
  } catch (erro) {
    if (erro.status === 403) {
      window.location.replace('pedidos.html');
      return;
    }
    if (erro.status === 401) {
      localStorageEncerrarSessao();
      window.location.replace('index.html');
      return;
    }
    const aviso = criar('p', `Não foi possível abrir o painel: ${erro.message}`, 'mensagem mensagem--erro');
    aviso.style.cssText = 'display:block; max-width:640px; margin:32px auto;';
    document.body.appendChild(aviso);
    return;
  }

  document.getElementById('painelAdmin').hidden = false;
  configurarAbas();
  configurarColaboradores(colaboradores);
  configurarPedidos();
  configurarAuditoria();
});

/* ---------- Utilidades ---------- */

function criar(tag, texto, classe) {
  const elemento = document.createElement(tag);
  if (texto != null) elemento.textContent = String(texto);
  if (classe) elemento.className = classe;
  return elemento;
}

function moeda(valor) {
  return Number(valor || 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

function dataHora(valor) {
  if (!valor) return '—';
  const data = new Date(valor);
  return Number.isNaN(data.getTime()) ? '—' : data.toLocaleString('pt-BR');
}

function diaLocal(valor) {
  const data = new Date(valor);
  if (Number.isNaN(data.getTime())) return '';
  const mes = String(data.getMonth() + 1).padStart(2, '0');
  const dia = String(data.getDate()).padStart(2, '0');
  return `${data.getFullYear()}-${mes}-${dia}`;
}

function mostrarMensagem(destino, texto, tipo) {
  destino.textContent = texto || '';
  destino.className = `mensagem${texto && tipo ? ` mensagem--${tipo}` : ''}`;
  destino.style.display = texto ? 'block' : 'none';
}

/* ---------- Abas ---------- */

function configurarAbas() {
  const botoes = document.querySelectorAll('.cadastro__aba');
  const abas = {
    colaboradores: document.getElementById('abaColaboradores'),
    pedidos: document.getElementById('abaPedidos'),
    auditoria: document.getElementById('abaAuditoria'),
  };
  const carregada = { pedidos: false, auditoria: false };

  botoes.forEach((botao) => {
    botao.addEventListener('click', () => {
      botoes.forEach((b) => b.classList.remove('ativa'));
      botao.classList.add('ativa');
      Object.values(abas).forEach((aba) => (aba.style.display = 'none'));
      abas[botao.dataset.aba].style.display = 'block';
      if (botao.dataset.aba in carregada && !carregada[botao.dataset.aba]) {
        carregada[botao.dataset.aba] = true;
        document.getElementById(botao.dataset.aba === 'pedidos' ? 'formFiltroPedidos' : 'formFiltroAuditoria').requestSubmit();
      }
    });
  });
}

/* ---------- Colaboradores ---------- */

function configurarColaboradores(inicial) {
  const mensagem = document.getElementById('mensagemColaborador');
  const tabela = document.getElementById('listaColaboradores');
  const seletor = document.getElementById('colaboradorSenhaId');
  const minhaMatricula = localStorageObterUsuario()?.matricula;

  function desenhar(colaboradores) {
    tabela.replaceChildren();
    seletor.replaceChildren();
    colaboradores.forEach((colaborador) => {
      const linha = criar('tr');
      const acao = criar('td');
      if (colaborador.matricula === minhaMatricula) {
        acao.textContent = 'Você';
      } else {
        const excluir = criar('button', 'Excluir', 'botao botao--vermelho botao--pequeno');
        excluir.type = 'button';
        excluir.addEventListener('click', () => excluirUmColaborador(colaborador));
        acao.appendChild(excluir);
      }
      linha.append(
        criar('td', colaborador.nome),
        criar('td', colaborador.matricula),
        criar('td', colaborador.email),
        criar('td', colaborador.perfil === 'administrador' ? 'Administrador' : 'Funcionário'),
        acao
      );
      tabela.appendChild(linha);

      const opcao = criar('option', `${colaborador.nome} (${colaborador.matricula})`);
      opcao.value = colaborador.id;
      seletor.appendChild(opcao);
    });
  }

  async function atualizar() {
    desenhar(await listarColaboradores());
  }

  async function excluirUmColaborador(colaborador) {
    if (!confirm(`Excluir o colaborador ${colaborador.nome} (${colaborador.matricula})? Esta ação não pode ser desfeita.`)) return;
    try {
      await excluirColaborador(colaborador.id);
      mostrarMensagem(mensagem, 'Colaborador excluído.', 'sucesso');
      await atualizar();
    } catch (erro) {
      mostrarMensagem(mensagem, erro.message, 'erro');
    }
  }

  document.getElementById('formColaborador').addEventListener('submit', async (evento) => {
    evento.preventDefault();
    mostrarMensagem(mensagem, '');
    const dados = {
      nome: document.getElementById('colNome').value.trim(),
      matricula: document.getElementById('colMatricula').value.trim(),
      email: document.getElementById('colEmail').value.trim(),
      senha: document.getElementById('colSenha').value,
      perfil: document.getElementById('colPerfil').value,
    };
    try {
      await cadastrarColaborador(dados);
      evento.target.reset();
      mostrarMensagem(mensagem, 'Colaborador cadastrado com sucesso.', 'sucesso');
      await atualizar();
    } catch (erro) {
      mostrarMensagem(mensagem, erro.message, 'erro');
    }
  });

  document.getElementById('formSenhaAdmin').addEventListener('submit', async (evento) => {
    evento.preventDefault();
    mostrarMensagem(mensagem, '');
    const id = seletor.value;
    const senha = document.getElementById('senhaAdminNova').value;
    if (!id || senha.length < 8) {
      mostrarMensagem(mensagem, 'Selecione um colaborador e informe uma senha de pelo menos 8 caracteres.', 'erro');
      return;
    }
    try {
      await redefinirSenhaPeloAdministrador(id, senha);
      evento.target.reset();
      mostrarMensagem(mensagem, 'Senha alterada com sucesso.', 'sucesso');
    } catch (erro) {
      mostrarMensagem(mensagem, erro.message, 'erro');
    }
  });

  desenhar(inicial);
}

/* ---------- Pedidos ---------- */

function configurarPedidos() {
  const formulario = document.getElementById('formFiltroPedidos');
  const mensagem = document.getElementById('mensagemPedidos');
  const resumo = document.getElementById('resumoPedidos');
  const lista = document.getElementById('listaPedidosAdmin');

  formulario.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    lista.replaceChildren();
    resumo.textContent = '';
    mostrarMensagem(mensagem, 'Consultando pedidos...', 'info');

    const termo = document.getElementById('filtroPedidoTermo').value;
    const situacao = document.getElementById('filtroPedidoSituacao').value;
    try {
      const [abertos, finalizados] = await Promise.all([
        situacao === 'FINALIZADOS' ? [] : pesquisarPedidosAbertos(termo),
        situacao === 'ABERTOS' ? [] : pesquisarPedidosFinalizados(termo),
      ]);
      const todos = [
        ...abertos.map((r) => ({ ...r, aberto: true })),
        ...finalizados.map((r) => ({ ...r, aberto: false })),
      ];
      desenharPedidos(aplicarFiltrosPedidos(todos), lista, resumo, mensagem, () => formulario.requestSubmit());
    } catch (erro) {
      mostrarMensagem(mensagem, `Não foi possível consultar pedidos: ${erro.message}`, 'erro');
    }
  });

  document.getElementById('limparFiltroPedidos').addEventListener('click', () => {
    formulario.reset();
    formulario.requestSubmit();
  });
}

function aplicarFiltrosPedidos(registros) {
  const modalidade = document.getElementById('filtroPedidoModalidade').value;
  const pagamento = document.getElementById('filtroPedidoPagamento').value;
  const de = document.getElementById('filtroPedidoDe').value;
  const ate = document.getElementById('filtroPedidoAte').value;

  return registros
    .filter(({ pedido }) => {
      if (modalidade && pedido.modalidade !== modalidade) return false;
      if (pagamento && pedido.formaPagamento !== pagamento) return false;
      const dia = diaLocal(pedido.finalizadoEm || pedido.criadoEm);
      if (de && (!dia || dia < de)) return false;
      if (ate && (!dia || dia > ate)) return false;
      return true;
    })
    .sort((a, b) => new Date(b.pedido.finalizadoEm || b.pedido.criadoEm) - new Date(a.pedido.finalizadoEm || a.pedido.criadoEm));
}

function desenharPedidos(registros, lista, resumo, mensagem, recarregar) {
  mostrarMensagem(mensagem, '');
  if (!registros.length) {
    mostrarMensagem(mensagem, 'Nenhum pedido encontrado para esses filtros.', 'info');
    return;
  }

  const finalizados = registros.filter((r) => !r.aberto);
  const somaFinalizados = finalizados.reduce((soma, r) => soma + Number(r.pedido.total || 0), 0);
  resumo.textContent = `${registros.length} pedido(s). Finalizados: ${finalizados.length}, total ${moeda(somaFinalizados)}.`
    + (registros.length > LIMITE_RESULTADOS ? ` Exibindo os ${LIMITE_RESULTADOS} mais recentes; refine a pesquisa.` : '');

  registros.slice(0, LIMITE_RESULTADOS).forEach((registro) => {
    const { pedido, clienteNome, clienteTelefone } = registro;
    const cartao = criar('article', null, 'cadastro__cartao');
    const itens = criar('ul', null, 'pedido-aberto__itens');
    (pedido.itens || []).forEach((item) => {
      const nome = item.meioAMeio ? `${item.nomeProduto} + ${item.segundoSabor}` : item.nomeProduto;
      itens.appendChild(criar('li', `${item.quantidade || 1} × ${nome || 'Produto'} — ${moeda(item.subtotal)}`));
    });

    const acoes = criar('div', null, 'admin__acoes');
    if (registro.aberto) {
      const abrir = criar('a', 'Corrigir pedido', 'botao botao--preto botao--pequeno');
      abrir.href = `pedidos.html?pedido=${encodeURIComponent(pedido.id)}`;
      acoes.appendChild(abrir);
    }
    const corrigir = criar('button', 'Corrigir valor', 'botao botao--contorno botao--pequeno');
    corrigir.type = 'button';
    const formularioCorrecao = criar('form', null, 'admin__correcao');
    formularioCorrecao.hidden = true;
    const campoValor = document.createElement('input');
    campoValor.type = 'number';
    campoValor.min = '0';
    campoValor.step = '0.01';
    campoValor.required = true;
    campoValor.value = Number(pedido.total || 0).toFixed(2);
    campoValor.setAttribute('aria-label', `Novo valor total de ${rotuloPedido(pedido)}`);
    const campoMotivo = document.createElement('input');
    campoMotivo.type = 'text';
    campoMotivo.required = true;
    campoMotivo.maxLength = 200;
    campoMotivo.placeholder = 'Motivo da correção (obrigatório)';
    campoMotivo.setAttribute('aria-label', 'Motivo da correção');
    const salvarCorrecao = criar('button', 'Salvar correção', 'botao botao--preto botao--pequeno');
    salvarCorrecao.type = 'submit';
    formularioCorrecao.append(campoValor, campoMotivo, salvarCorrecao);
    corrigir.addEventListener('click', () => {
      formularioCorrecao.hidden = !formularioCorrecao.hidden;
      if (!formularioCorrecao.hidden) campoValor.focus();
    });
    formularioCorrecao.addEventListener('submit', async (evento) => {
      evento.preventDefault();
      const novoValor = Number(campoValor.value);
      if (!Number.isFinite(novoValor) || novoValor < 0 || !campoMotivo.value.trim()) {
        mostrarMensagem(mensagem, 'Informe um total válido e o motivo da correção.', 'erro');
        return;
      }
      salvarCorrecao.disabled = true;
      try {
        await corrigirValorPedido(pedido.id, novoValor, campoMotivo.value.trim());
        recarregar();
      } catch (erro) {
        mostrarMensagem(mensagem, `Não foi possível corrigir o valor: ${erro.message}`, 'erro');
      } finally {
        salvarCorrecao.disabled = false;
      }
    });
    acoes.appendChild(corrigir);
    const excluir = criar('button', 'Excluir pedido', 'botao botao--vermelho botao--pequeno');
    excluir.type = 'button';
    excluir.addEventListener('click', async () => {
      if (!confirm(`Excluir ${rotuloPedido(pedido)}? Esta ação não pode ser desfeita.`)) return;
      try {
        await cancelarPedido(pedido.id);
        recarregar();
      } catch (erro) {
        mostrarMensagem(mensagem, `Não foi possível excluir o pedido: ${erro.message}`, 'erro');
      }
    });
    acoes.appendChild(excluir);

    cartao.append(
      criar('strong', rotuloPedido(pedido)),
      criar('p', `${registro.aberto ? 'Em aberto' : 'Finalizado'} · ${dataHora(pedido.finalizadoEm || pedido.criadoEm)}`, 'pedido-aberto__meta'),
      criar('p', `Cliente: ${clienteNome || '—'} · ${formatarTelefone(pedido.telefoneContato || clienteTelefone)}`),
      criar('p', `Atendente (matrícula): ${pedido.colaboradorMatricula || '—'}`),
      criar('p', `Modalidade: ${pedido.modalidade === 'RETIRADA' ? 'Retirada' : 'Entrega'} · Pagamento: ${pedido.formaPagamento || '—'}`),
      itens,
      criar('p', `Total: ${pedido.total == null ? 'pendente' : moeda(pedido.total)}`, 'admin__total'),
      acoes,
      formularioCorrecao
    );
    lista.appendChild(cartao);
  });
}

/* ---------- Auditoria ---------- */

function configurarAuditoria() {
  const formulario = document.getElementById('formFiltroAuditoria');
  const mensagem = document.getElementById('mensagemAuditoria');
  const tabela = document.getElementById('tabelaAuditoria');
  const corpo = document.getElementById('listaAuditoria');

  formulario.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    corpo.replaceChildren();
    tabela.hidden = true;
    mostrarMensagem(mensagem, 'Consultando auditoria...', 'info');
    try {
      const registros = await consultarAuditoria({
        termo: document.getElementById('filtroAuditoriaTermo').value,
        de: document.getElementById('filtroAuditoriaDe').value,
        ate: document.getElementById('filtroAuditoriaAte').value,
      });
      const lista = (Array.isArray(registros) ? registros : [])
        .map(normalizarAuditoria)
        .sort((a, b) => new Date(b.dataHora) - new Date(a.dataHora));
      if (!lista.length) {
        mostrarMensagem(mensagem, 'Nenhum registro de auditoria encontrado.', 'info');
        return;
      }
      mostrarMensagem(mensagem, lista.length > LIMITE_RESULTADOS
        ? `${lista.length} registros. Exibindo os ${LIMITE_RESULTADOS} mais recentes; refine a pesquisa.`
        : `${lista.length} registro(s).`, 'info');
      lista.slice(0, LIMITE_RESULTADOS).forEach((registro) => {
        const linha = criar('tr');
        linha.append(criar('td', dataHora(registro.dataHora)), criar('td', registro.usuario), criar('td', registro.acao), criar('td', registro.detalhes ? `${registro.registro} — ${registro.detalhes}` : registro.registro));
        corpo.appendChild(linha);
      });
      tabela.hidden = false;
    } catch (erro) {
      mostrarMensagem(mensagem, erro.status === 404
        ? 'A consulta de auditoria ainda não está disponível no servidor.'
        : `Não foi possível consultar a auditoria: ${erro.message}`, 'erro');
    }
  });

  document.getElementById('limparFiltroAuditoria').addEventListener('click', () => {
    formulario.reset();
    formulario.requestSubmit();
  });
}

function normalizarAuditoria(registro) {
  return {
    dataHora: registro.dataHora || registro.data || registro.criadoEm,
    usuario: registro.usuarioNome || registro.usuarioResponsavelNome || registro.usuarioResponsavel || registro.usuario || '—',
    acao: registro.acao || '—',
    registro: registro.registroAfetado || registro.registro || '—',
    detalhes: registro.detalhes || '',
  };
}
