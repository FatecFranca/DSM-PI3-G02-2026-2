package PI.Pizzaria.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
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

@Service
public class PedidoService {
    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final PizzaRepository pizzaRepository;
    private final String precoBaseConfigurado;

    public PedidoService(PedidoRepository pedidoRepository,
                         ClienteRepository clienteRepository,
                         PizzaRepository pizzaRepository,
                         @Value("${app.pizza.preco-base:}") String precoBaseConfigurado) {
        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
        this.pizzaRepository = pizzaRepository;
        this.precoBaseConfigurado = precoBaseConfigurado;
    }

    public Pedido criar(PedidoRequest dados, Colaborador atendente) {
        if (atendente == null || atendente.getId() == null || atendente.getId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Faça login para criar pedidos.");
        }
        if (dados == null || dados.getClienteId() == null || dados.getClienteId().isBlank()) {
            throw new IllegalArgumentException("Selecione um cliente para o pedido.");
        }
        if (!clienteRepository.existsById(dados.getClienteId().trim())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado.");
        }
        if (dados.getItens() == null || dados.getItens().isEmpty()) {
            throw new IllegalArgumentException("Adicione ao menos uma pizza ao pedido.");
        }
        if (dados.getItens().size() > 50) {
            throw new IllegalArgumentException("O pedido aceita até 50 itens nesta etapa.");
        }

        BigDecimal precoBase = obterPrecoBase();
        List<ItemPedido> itens = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO.setScale(2);
        for (ItemPedidoRequest item : dados.getItens()) {
            ItemPedido salvo = montarItem(item, precoBase);
            itens.add(salvo);
            subtotal = subtotal.add(salvo.getSubtotal());
        }

        Pedido pedido = new Pedido();
        pedido.setClienteId(dados.getClienteId().trim());
        // Nunca aceita o ID ou o perfil do atendente enviado pelo navegador.
        pedido.setColaboradorId(atendente.getId());
        pedido.setColaboradorMatricula(atendente.getMatricula());
        pedido.setCriadoEm(Instant.now());
        pedido.setItens(itens);
        pedido.setSubtotalPizzas(subtotal);
        pedido.setSituacao("EM_ABERTO");
        return pedidoRepository.save(pedido);
    }

    public Pedido buscarPorId(String id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Pedido não encontrado."));
    }

    private ItemPedido montarItem(ItemPedidoRequest dados, BigDecimal precoBase) {
        if (dados == null || dados.getProdutoId() == null || dados.getProdutoId().isBlank()) {
            throw new IllegalArgumentException("Selecione uma pizza válida.");
        }
        int quantidade = dados.getQuantidade() == null ? 1 : dados.getQuantidade();
        if (quantidade < 1 || quantidade > 100) {
            throw new IllegalArgumentException("A quantidade deve estar entre 1 e 100.");
        }

        Pizza primeira = buscarPizza(dados.getProdutoId().trim());
        String segundoId = dados.getSegundoProdutoId() == null ? null : dados.getSegundoProdutoId().trim();
        if (dados.isMeioAMeio()) {
            if (segundoId == null || segundoId.isBlank() || segundoId.equals(dados.getProdutoId().trim())) {
                throw new IllegalArgumentException("Pizza meio a meio exige dois sabores diferentes.");
            }
        } else if (segundoId != null && !segundoId.isBlank()) {
            throw new IllegalArgumentException("Pizza inteira não pode ter segundo sabor.");
        }

        Pizza segunda = dados.isMeioAMeio() ? buscarPizza(segundoId) : null;
        ItemPedido item = new ItemPedido();
        item.setProdutoId(primeira.id);
        item.setSabor(descricaoPizza(primeira));
        item.setMeioAMeio(dados.isMeioAMeio());
        if (segunda != null) {
            item.setSegundoProdutoId(segunda.id);
            item.setSegundoSabor(descricaoPizza(segunda));
        }
        item.setTipoPreparo(dados.getTipoPreparo() == null ? ItemPedido.TipoPreparo.ASSADA : dados.getTipoPreparo());
        item.setQuantidade(quantidade);
        // Mesmo preço-base para inteira e meio a meio, independentemente dos sabores.
        item.setPrecoUnitario(precoBase);
        item.setSubtotal(precoBase.multiply(BigDecimal.valueOf(quantidade)));
        return item;
    }

    private Pizza buscarPizza(String id) {
        Pizza pizza = pizzaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Pizza não encontrada no catálogo."));
        if (pizza.sabor == null || pizza.sabor.isBlank()) {
            throw new IllegalArgumentException("A pizza cadastrada precisa possuir um sabor.");
        }
        return pizza;
    }

    private String descricaoPizza(Pizza pizza) {
        return pizza.sabor.trim();
    }

    private BigDecimal obterPrecoBase() {
        if (precoBaseConfigurado == null || precoBaseConfigurado.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Preço-base das pizzas ainda não configurado pela pizzaria.");
        }
        try {
            BigDecimal valor = new BigDecimal(precoBaseConfigurado.trim()).setScale(2, RoundingMode.UNNECESSARY);
            if (valor.signum() <= 0) throw new NumberFormatException("Preço-base não positivo");
            return valor;
        } catch (NumberFormatException | ArithmeticException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Preço-base das pizzas inválido. Configure um valor positivo com até duas casas decimais.");
        }
    }
}
