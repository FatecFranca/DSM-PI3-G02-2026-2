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
import PI.Pizzaria.model.ItemPedido;
import PI.Pizzaria.model.Pedido;
import PI.Pizzaria.model.Pizza;
import PI.Pizzaria.repository.ClienteRepository;
import PI.Pizzaria.repository.PedidoRepository;
import PI.Pizzaria.repository.PizzaRepository;

class PedidoServiceTest {
    private PedidoRepository pedidoRepository;
    private ClienteRepository clienteRepository;
    private PizzaRepository pizzaRepository;
    private PedidoService service;
    private Colaborador atendente;

    @BeforeEach
    void iniciar() {
        pedidoRepository = mock(PedidoRepository.class);
        clienteRepository = mock(ClienteRepository.class);
        pizzaRepository = mock(PizzaRepository.class);
        service = new PedidoService(pedidoRepository, clienteRepository, pizzaRepository, "59.00");
        when(clienteRepository.existsById("cliente-1")).thenReturn(true);
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        Pizza calabresa = pizza("pizza-1", "Calabresa", 54.0);
        Pizza mussarela = pizza("pizza-2", "Mussarela", 72.0);
        when(pizzaRepository.findById("pizza-1")).thenReturn(Optional.of(calabresa));
        when(pizzaRepository.findById("pizza-2")).thenReturn(Optional.of(mussarela));

        atendente = new Colaborador();
        atendente.setId("colaborador-1");
        atendente.setMatricula("1001");
    }

    @Test
    void criaPedidoComClienteEFuncionarioAutenticadoEAssadaComoPadrao() {
        Pedido pedido = service.criar(requisicao(item("pizza-1")), atendente);

        assertEquals("cliente-1", pedido.getClienteId());
        assertEquals("colaborador-1", pedido.getColaboradorId());
        assertEquals("1001", pedido.getColaboradorMatricula());
        assertNotNull(pedido.getCriadoEm());
        assertEquals("EM_ABERTO", pedido.getSituacao());
        assertEquals(ItemPedido.TipoPreparo.ASSADA, pedido.getItens().get(0).getTipoPreparo());
        assertEquals(new BigDecimal("59.00"), pedido.getSubtotalPizzas());
        verify(pedidoRepository).save(any(Pedido.class));
    }

    @Test
    void pizzaMeioAMeioTemMesmoPrecoBaseEAceitaPreAssada() {
        ItemPedidoRequest item = item("pizza-1");
        item.setMeioAMeio(true);
        item.setSegundoProdutoId("pizza-2");
        item.setTipoPreparo(ItemPedido.TipoPreparo.PRE_ASSADA);
        item.setQuantidade(2);

        Pedido pedido = service.criar(requisicao(item), atendente);
        ItemPedido salvo = pedido.getItens().get(0);
        assertTrue(salvo.isMeioAMeio());
        assertEquals("Calabresa", salvo.getSabor());
        assertEquals("Mussarela", salvo.getSegundoSabor());
        assertEquals(ItemPedido.TipoPreparo.PRE_ASSADA, salvo.getTipoPreparo());
        assertEquals(new BigDecimal("59.00"), salvo.getPrecoUnitario());
        assertEquals(new BigDecimal("118.00"), salvo.getSubtotal());
    }

    @Test
    void rejeitaMeioAMeioSemSegundoSabor() {
        ItemPedidoRequest item = item("pizza-1");
        item.setMeioAMeio(true);
        assertThrows(IllegalArgumentException.class, () -> service.criar(requisicao(item), atendente));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void rejeitaPizzaInteiraComSegundoSabor() {
        ItemPedidoRequest item = item("pizza-1");
        item.setSegundoProdutoId("pizza-2");
        assertThrows(IllegalArgumentException.class, () -> service.criar(requisicao(item), atendente));
    }

    @Test
    void rejeitaQuantidadeNegativaEPizzaInexistente() {
        ItemPedidoRequest item = item("pizza-1");
        item.setQuantidade(-1);
        assertThrows(IllegalArgumentException.class, () -> service.criar(requisicao(item), atendente));
        assertThrows(ResponseStatusException.class,
                () -> service.criar(requisicao(item("nao-existe")), atendente));
    }

    @Test
    void rejeitaClienteInexistenteEAtendenteNaoAutenticado() {
        when(clienteRepository.existsById("cliente-1")).thenReturn(false);
        assertThrows(ResponseStatusException.class,
                () -> service.criar(requisicao(item("pizza-1")), atendente));
        assertThrows(ResponseStatusException.class,
                () -> service.criar(requisicao(item("pizza-1")), null));
    }

    @Test
    void bloqueiaPedidoSemPrecoBaseConfigurado() {
        PedidoService semPreco = new PedidoService(pedidoRepository, clienteRepository, pizzaRepository, "");
        assertThrows(ResponseStatusException.class,
                () -> semPreco.criar(requisicao(item("pizza-1")), atendente));
    }

    private PedidoRequest requisicao(ItemPedidoRequest item) {
        PedidoRequest request = new PedidoRequest();
        request.setClienteId("cliente-1");
        request.setItens(List.of(item));
        return request;
    }

    private ItemPedidoRequest item(String id) {
        ItemPedidoRequest item = new ItemPedidoRequest();
        item.setProdutoId(id);
        return item;
    }

    private Pizza pizza(String id, String sabor, double preco) {
        Pizza pizza = new Pizza();
        pizza.id = id;
        pizza.sabor = sabor;
        pizza.nome = sabor;
        pizza.preco = preco;
        return pizza;
    }
}
