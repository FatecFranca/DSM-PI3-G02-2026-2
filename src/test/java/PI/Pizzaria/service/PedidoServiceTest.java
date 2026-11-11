package PI.Pizzaria.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import PI.Pizzaria.dto.ItemPedidoRequest;
import PI.Pizzaria.dto.PedidoRequest;
import PI.Pizzaria.model.Colaborador;
import PI.Pizzaria.model.Endereco;
import PI.Pizzaria.model.ItemPedido;
import PI.Pizzaria.model.Pedido;
import PI.Pizzaria.model.Produto;
import PI.Pizzaria.repository.ClienteRepository;
import PI.Pizzaria.repository.PedidoRepository;
import PI.Pizzaria.repository.ProdutoRepository;

class PedidoServiceTest {

    private PedidoRepository pedidoRepository;
    private ClienteRepository clienteRepository;
    private ProdutoRepository produtoRepository;
    private PedidoService service;
    private Colaborador atendente;

    @BeforeEach
    void iniciar() {

        pedidoRepository = mock(PedidoRepository.class);
        clienteRepository = mock(ClienteRepository.class);
        produtoRepository = mock(ProdutoRepository.class);

        service = new PedidoService(
                pedidoRepository,
                clienteRepository,
                produtoRepository,
                "59.00",
                "",
                "7.00");

        PI.Pizzaria.model.Cliente cliente = new PI.Pizzaria.model.Cliente();
        cliente.setId("cliente-1");
        cliente.setNome("Maria");
        cliente.setTelefone("16999999999");
        when(clienteRepository.findById("cliente-1"))
                .thenReturn(Optional.of(cliente));

        when(pedidoRepository.save(any(Pedido.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        Produto calabresa =
                produto("pizza-1", "Pizza Calabresa", "Calabresa", 54.0);

        Produto mussarela =
                produto("pizza-2", "Pizza Mussarela", "Mussarela", 72.0);

        when(produtoRepository.findById("pizza-1"))
                .thenReturn(Optional.of(calabresa));

        when(produtoRepository.findById("pizza-2"))
                .thenReturn(Optional.of(mussarela));

        atendente = new Colaborador();
        atendente.setId("colaborador-1");
        atendente.setMatricula("1001");
    }

    @Test
    void criaPedidoComClienteFuncionarioEModalidadePagamentoEndereco() {

        PedidoRequest request = requisicao(item("pizza-1"));
        request.setModalidade("ENTREGA");
        request.setFormaPagamento("PIX");

        Endereco endereco = new Endereco();
        endereco.setCep("14400000");
        endereco.setRua("Rua Exemplo");
        endereco.setNumero("100");
        endereco.setBairro("Centro");
        endereco.setCidade("Franca");
        request.setEnderecoEntrega(endereco);

        Pedido pedido = service.criar(request, atendente);

        assertEquals("cliente-1", pedido.getClienteId());
        assertEquals("colaborador-1", pedido.getColaboradorId());
        assertEquals("1001", pedido.getColaboradorMatricula());
        assertNotNull(pedido.getCriadoEm());
        assertEquals("EM_ABERTO", pedido.getSituacao());
        assertEquals(ItemPedido.TipoPreparo.ASSADA,
                pedido.getItens().get(0).getTipoPreparo());
        assertEquals(new BigDecimal("59.00"), pedido.getSubtotalItens());

        assertEquals("ENTREGA", pedido.getModalidade());
        assertEquals("PIX", pedido.getFormaPagamento());
        assertNotNull(pedido.getEnderecoEntrega());
        assertEquals("14400000", pedido.getEnderecoEntrega().getCep());
        assertEquals(new BigDecimal("7.00"), pedido.getTaxaEntrega());
        assertEquals(new BigDecimal("66.00"), pedido.getTotal());

        verify(pedidoRepository).save(any(Pedido.class));
    }

    @Test
    void pizzaMeioAMeioTemMesmoPrecoBaseEAceitaPreAssada() {

        ItemPedidoRequest item = item("pizza-1");

        item.setMeioAMeio(true);
        item.setSegundoProdutoId("pizza-2");

        item.setTipoPreparo(
                ItemPedido.TipoPreparo.PRE_ASSADA);

        item.setQuantidade(2);

        Pedido pedido =
                service.criar(
                        requisicao(item),
                        atendente);

        ItemPedido salvo =
                pedido.getItens().get(0);

        assertTrue(salvo.isMeioAMeio());

        assertEquals(
                "Calabresa",
                salvo.getSabor());

        assertEquals(
                "Mussarela",
                salvo.getSegundoSabor());

        assertEquals(
                ItemPedido.TipoPreparo.PRE_ASSADA,
                salvo.getTipoPreparo());

        assertEquals(
                new BigDecimal("59.00"),
                salvo.getPrecoUnitario());

        assertEquals(
                new BigDecimal("118.00"),
                salvo.getSubtotal());
    }

    @Test
    void configuracaoSeparadaDePreAssadaNaoFazMediaDosSabores() {
        PedidoService comPrecoPreAssada = new PedidoService(
                pedidoRepository, clienteRepository, produtoRepository,
                "59.00", "49.00", "7.00");
        ItemPedidoRequest metade = item("pizza-1");
        metade.setMeioAMeio(true);
        metade.setSegundoProdutoId("pizza-2");
        metade.setTipoPreparo(ItemPedido.TipoPreparo.PRE_ASSADA);
        Pedido preAssadaMeio = comPrecoPreAssada.criar(requisicao(metade), atendente);
        assertEquals(new BigDecimal("49.00"), preAssadaMeio.getItens().get(0).getPrecoUnitario());
        assertEquals(new BigDecimal("49.00"), preAssadaMeio.getSubtotalItens());

        ItemPedidoRequest inteira = item("pizza-1");
        inteira.setTipoPreparo(ItemPedido.TipoPreparo.PRE_ASSADA);
        Pedido preAssadaInteira = comPrecoPreAssada.criar(requisicao(inteira), atendente);
        assertEquals(new BigDecimal("49.00"), preAssadaInteira.getSubtotalItens());

        metade.setTipoPreparo(ItemPedido.TipoPreparo.ASSADA);
        Pedido assadaMeio = comPrecoPreAssada.criar(requisicao(metade), atendente);
        assertEquals(new BigDecimal("59.00"), assadaMeio.getSubtotalItens());
        Pedido assadaInteira = comPrecoPreAssada.criar(requisicao(item("pizza-1")), atendente);
        assertEquals(new BigDecimal("59.00"), assadaInteira.getSubtotalItens());
        assertEquals(new BigDecimal("59.00"), comPrecoPreAssada.consultarPrecosPizza().get("precoBase"));
        assertEquals(new BigDecimal("49.00"), comPrecoPreAssada.consultarPrecosPizza().get("precoPreAssada"));
    }

    @Test
    void valorDosAcrescimosTambemEntraNoSubtotalComQuantidade() {
        ItemPedidoRequest metade = item("pizza-1");
        metade.setQuantidade(2);
        metade.setAcrescimos(List.of("Catupiry", "Bacon"));
        Pedido pedido = service.criar(requisicao(metade), atendente);
        assertEquals(new BigDecimal("69.00"), pedido.getItens().get(0).getPrecoUnitario());
        assertEquals(new BigDecimal("138.00"), pedido.getSubtotalItens());
    }

    @Test
    void pesquisaPedidosAbertosPeloNomeDoCliente() {
        Pedido pedido = new Pedido();
        pedido.setId("pedido-1");
        pedido.setClienteId("cliente-1");
        pedido.setSituacao("EM_ABERTO");
        pedido.setColaboradorMatricula("1001");
        PI.Pizzaria.model.Cliente cliente = new PI.Pizzaria.model.Cliente();
        cliente.setId("cliente-1");
        cliente.setNome("Maria Teste");
        cliente.setTelefone("16999999999");
        when(pedidoRepository.findBySituacaoOrderByCriadoEmDesc("EM_ABERTO"))
                .thenReturn(List.of(pedido));
        when(clienteRepository.findAllById(anyList()))
                .thenReturn(List.of(cliente));

        assertEquals(1, service.pesquisarPedidosAbertos("maria").size());
        assertEquals(1, service.pesquisarPedidosAbertos("1001").size());
        assertEquals(0, service.pesquisarPedidosAbertos("outra pessoa").size());
    }

    @Test
    void rejeitaMeioAMeioSemSegundoSabor() {

        ItemPedidoRequest item = item("pizza-1");

        item.setMeioAMeio(true);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.criar(
                        requisicao(item),
                        atendente));

        verify(
                pedidoRepository,
                never())
                .save(any());
    }

    @Test
    void rejeitaPizzaInteiraComSegundoSabor() {

        ItemPedidoRequest item = item("pizza-1");

        item.setSegundoProdutoId("pizza-2");

        assertThrows(
                IllegalArgumentException.class,
                () -> service.criar(
                        requisicao(item),
                        atendente));
    }

    @Test
    void rejeitaQuantidadeNegativaEProdutoInexistente() {

        ItemPedidoRequest item = item("pizza-1");
        item.setQuantidade(-1);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.criar(
                        requisicao(item),
                        atendente));

        assertThrows(
                ResponseStatusException.class,
                () -> service.criar(
                        requisicao(
                                item("nao-existe")),
                        atendente));
    }

    @Test
    void rejeitaClienteInexistenteEAtendenteNaoAutenticado() {

        when(clienteRepository.findById("cliente-1"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResponseStatusException.class,
                () -> service.criar(
                        requisicao(item("pizza-1")),
                        atendente));

        assertThrows(
                ResponseStatusException.class,
                () -> service.criar(
                        requisicao(item("pizza-1")),
                        null));
    }

    @Test
    void bloqueiaPedidoSemPrecoBaseConfigurado() {

        PedidoService semPreco =
                new PedidoService(
                        pedidoRepository,
                        clienteRepository,
                        produtoRepository,
                        "",
                        "",
                        "7.00");

        assertThrows(
                ResponseStatusException.class,
                () -> semPreco.criar(
                        requisicao(item("pizza-1")),
                        atendente));
    }

    @Test
    void retiradaNaoCobraTaxaENaoGuardaEndereco() {
        PedidoRequest requisicao = requisicao(item("pizza-1"));
        Endereco rua = new Endereco();
        rua.setRua("Não gravar na retirada");
        requisicao.setEnderecoEntrega(rua);
        Pedido pedido = service.criar(requisicao, atendente);
        assertEquals("RETIRADA", pedido.getModalidade());
        assertNull(pedido.getEnderecoEntrega());
        assertEquals(new BigDecimal("0.00"), pedido.getTaxaEntrega());
        assertEquals(new BigDecimal("59.00"), pedido.getTotal());
    }

    @Test
    void entregaSemEnderecoFalhaSemSalvar() {
        PedidoRequest requisicao = requisicao(item("pizza-1"));
        requisicao.setModalidade("ENTREGA");
        assertThrows(IllegalArgumentException.class, () -> service.criar(requisicao, atendente));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void entregaSemTaxaConfiguradaNaoFingeSerGratuita() {
        PedidoService semTaxa = new PedidoService(pedidoRepository, clienteRepository,
                produtoRepository, "59.00", "", "");
        PedidoRequest requisicao = requisicao(item("pizza-1"));
        requisicao.setModalidade("ENTREGA");
        Endereco e = new Endereco();
        e.setRua("Rua"); e.setNumero("2"); e.setBairro("Bairro"); e.setCidade("Franca");
        requisicao.setEnderecoEntrega(e);
        assertThrows(ResponseStatusException.class, () -> semTaxa.criar(requisicao, atendente));
    }

    @Test
    void atualizaPedidoExistenteSemDuplicarEFinalizaSomenteUmaVez() {
        Pedido pedido = service.criar(requisicao(item("pizza-1")), atendente);
        pedido.setId("pedido-1");
        when(pedidoRepository.findById("pedido-1")).thenReturn(Optional.of(pedido));
        PedidoRequest novo = requisicao(item("pizza-2"));
        novo.setFormaPagamento("PIX");
        Pedido atualizado = service.atualizar("pedido-1", novo, atendente);
        assertEquals("pedido-1", atualizado.getId());
        assertEquals("PIX", atualizado.getFormaPagamento());
        assertEquals("EM_ABERTO", atualizado.getSituacao());
        Pedido finalizado = service.finalizar("pedido-1", atendente);
        assertEquals("FINALIZADO", finalizado.getSituacao());
        assertNotNull(finalizado.getFinalizadoEm());
        assertEquals("1001", finalizado.getFinalizadoPorMatricula());
        assertThrows(ResponseStatusException.class, () -> service.atualizar("pedido-1", novo, atendente));
        assertThrows(ResponseStatusException.class, () -> service.finalizar("pedido-1", atendente));
    }

    @Test
    void usaTelefoneEditadoSomenteNoPedido() {
        PedidoRequest requisicao = requisicao(item("pizza-1"));
        requisicao.setTelefoneContato("(16) 98888-1234");
        Pedido pedido = service.criar(requisicao, atendente);
        assertEquals("16988881234", pedido.getTelefoneContato());
        assertEquals("16999999999", clienteRepository.findById("cliente-1").orElseThrow().getTelefone());
    }

    private PedidoRequest requisicao(
            ItemPedidoRequest item) {

        PedidoRequest request =
                new PedidoRequest();

        request.setClienteId("cliente-1");
        request.setItens(List.of(item));
        request.setModalidade("RETIRADA");

        return request;
    }

    private ItemPedidoRequest item(String id) {

        ItemPedidoRequest item =
                new ItemPedidoRequest();

        item.setProdutoId(id);

        return item;
    }

    private Produto produto(
            String id,
            String nome,
            String sabor,
            double preco) {

        Produto produto = new Produto();

        produto.setId(id);
        produto.setNome(nome);
        produto.setCategoria("PIZZA");
        produto.setSabor(sabor);

        produto.setPreco(
                BigDecimal.valueOf(preco));

        produto.setEstoque(10);

        return produto;
    }
}