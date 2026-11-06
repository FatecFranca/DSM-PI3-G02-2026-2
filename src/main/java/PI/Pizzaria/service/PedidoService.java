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
import PI.Pizzaria.model.Produto;
import PI.Pizzaria.repository.ClienteRepository;
import PI.Pizzaria.repository.PedidoRepository;
import PI.Pizzaria.repository.ProdutoRepository;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final ProdutoRepository produtoRepository;
    private final String precoBaseConfigurado;

    public PedidoService(
            PedidoRepository pedidoRepository,
            ClienteRepository clienteRepository,
            ProdutoRepository produtoRepository,
            @Value("${app.pizza.preco-base:}") String precoBaseConfigurado) {

        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
        this.produtoRepository = produtoRepository;
        this.precoBaseConfigurado = precoBaseConfigurado;
    }

    public Pedido criar(PedidoRequest dados, Colaborador atendente) {

        if (atendente == null
                || atendente.getId() == null
                || atendente.getId().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Faça login para criar pedidos.");
        }

        if (dados == null
                || dados.getClienteId() == null
                || dados.getClienteId().isBlank()) {

            throw new IllegalArgumentException(
                    "Selecione um cliente para o pedido.");
        }

        if (!clienteRepository.existsById(dados.getClienteId().trim())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Cliente não encontrado.");
        }

        if (dados.getItens() == null || dados.getItens().isEmpty()) {
            throw new IllegalArgumentException(
                    "Adicione ao menos um item ao pedido.");
        }

        if (dados.getItens().size() > 50) {
            throw new IllegalArgumentException(
                    "O pedido aceita até 50 itens nesta etapa.");
        }

        List<ItemPedido> itens = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO.setScale(2);

        for (ItemPedidoRequest item : dados.getItens()) {

            ItemPedido salvo = montarItem(item);

            itens.add(salvo);
            subtotal = subtotal.add(salvo.getSubtotal());
        }

        Pedido pedido = new Pedido();

        pedido.setClienteId(dados.getClienteId().trim());

        // O colaborador vem da sessão autenticada,
        // e nunca do JSON enviado pelo navegador.
        pedido.setColaboradorId(atendente.getId());
        pedido.setColaboradorMatricula(atendente.getMatricula());

        pedido.setCriadoEm(Instant.now());
        pedido.setItens(itens);

        // O nome ainda é subtotalItens no model atual.
        // Mantemos por enquanto para não quebrar outras partes do projeto.
        pedido.setSubtotalItens(subtotal);

        pedido.setSituacao("EM_ABERTO");

        return pedidoRepository.save(pedido);
    }

    public Pedido buscarPorId(String id) {

        return pedidoRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Pedido não encontrado."));
    }

    private ItemPedido montarItem(ItemPedidoRequest dados) {

        if (dados == null
                || dados.getProdutoId() == null
                || dados.getProdutoId().isBlank()) {

            throw new IllegalArgumentException(
                    "Selecione um produto válido.");
        }

        int quantidade =
                dados.getQuantidade() == null
                        ? 1
                        : dados.getQuantidade();

        if (quantidade < 1 || quantidade > 100) {
            throw new IllegalArgumentException(
                    "A quantidade deve estar entre 1 e 100.");
        }

        Produto primeiro =
                buscarProduto(dados.getProdutoId().trim());

        boolean pizza = ehPizza(primeiro);

        /*
         * Meio a meio só faz sentido para produtos
         * da categoria PIZZA.
         */
        if (dados.isMeioAMeio() && !pizza) {
            throw new IllegalArgumentException(
                    "A opção meio a meio é permitida apenas para pizzas.");
        }

        String segundoId =
                dados.getSegundoProdutoId() == null
                        ? null
                        : dados.getSegundoProdutoId().trim();

        Produto segundo = null;

        if (dados.isMeioAMeio()) {

            if (segundoId == null
                    || segundoId.isBlank()
                    || segundoId.equals(primeiro.getId())) {

                throw new IllegalArgumentException(
                        "Pizza meio a meio exige dois sabores diferentes.");
            }

            segundo = buscarProduto(segundoId);

            if (!ehPizza(segundo)) {
                throw new IllegalArgumentException(
                        "O segundo sabor também deve ser uma pizza.");
            }

        } else if (segundoId != null && !segundoId.isBlank()) {

            throw new IllegalArgumentException(
                    "Item inteiro não pode possuir segundo produto.");
        }

        ItemPedido item = new ItemPedido();

        item.setProdutoId(primeiro.getId());
        item.setNomeProduto(primeiro.getNome());
        item.setCategoria(primeiro.getCategoria());

        /*
         * Sabor só será preenchido quando o produto possuir
         * essa informação.
         */
        item.setSabor(primeiro.getSabor());

        item.setMeioAMeio(dados.isMeioAMeio());

        if (segundo != null) {
            item.setSegundoProdutoId(segundo.getId());
            item.setSegundoSabor(segundo.getSabor());
        }

        if (pizza) {

            item.setTipoPreparo(
                    dados.getTipoPreparo() == null
                            ? ItemPedido.TipoPreparo.ASSADA
                            : dados.getTipoPreparo());

        } else {

            item.setTipoPreparo(null);
        }

        item.setQuantidade(quantidade);

        BigDecimal precoUnitario;

        if (pizza) {
            precoUnitario = obterPrecoBasePizza();
        } else {

            if (primeiro.getPreco() == null) {
                throw new IllegalArgumentException(
                        "O produto precisa possuir um preço.");
            }

            precoUnitario = primeiro.getPreco();
        }

        item.setPrecoUnitario(precoUnitario);

        /*
         * Persistimos observações e acréscimos.
         * A regra financeira dos R$ 5,00 será tratada
         * na implementação responsável por essa regra.
         */
        item.setObservacao(dados.getObservacao());
        item.setAcrescimos(dados.getAcrescimos());

        item.setSubtotal(
                precoUnitario.multiply(
                        BigDecimal.valueOf(quantidade)));

        return item;
    }

    private Produto buscarProduto(String id) {

        return produtoRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Produto não encontrado no catálogo."));
    }

    private boolean ehPizza(Produto produto) {

        return produto.getCategoria() != null
                && produto.getCategoria()
                        .equalsIgnoreCase("PIZZA");
    }

    private BigDecimal obterPrecoBasePizza() {

        if (precoBaseConfigurado == null
                || precoBaseConfigurado.isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Preço-base das pizzas ainda não configurado pela pizzaria.");
        }

        try {

            BigDecimal valor =
                    new BigDecimal(precoBaseConfigurado.trim())
                            .setScale(
                                    2,
                                    RoundingMode.UNNECESSARY);

            if (valor.signum() <= 0) {
                throw new NumberFormatException(
                        "Preço-base não positivo");
            }

            return valor;

        } catch (NumberFormatException | ArithmeticException e) {

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Preço-base das pizzas inválido. Configure um valor positivo com até duas casas decimais.");
        }
    }
}