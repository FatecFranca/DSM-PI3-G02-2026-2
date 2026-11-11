package PI.Pizzaria.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import java.util.stream.Collectors;

import PI.Pizzaria.dto.PedidoAbertoResponse;
import PI.Pizzaria.model.Cliente;

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
    private final String precoPreAssadaConfigurado;
    private static final BigDecimal PRECO_POR_ACRESCIMO =
        new BigDecimal("5.00");

    public PedidoService(
            PedidoRepository pedidoRepository,
            ClienteRepository clienteRepository,
            ProdutoRepository produtoRepository,
            @Value("${app.pizza.preco-base:}") String precoBaseConfigurado,
            @Value("${app.pizza.preco-pre-assada:}") String precoPreAssadaConfigurado) {

        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
        this.produtoRepository = produtoRepository;
        this.precoBaseConfigurado = precoBaseConfigurado;
        this.precoPreAssadaConfigurado = precoPreAssadaConfigurado;
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

        pedido.setSubtotalItens(subtotal);

        // Persistência da Sprint 4. As regras de modalidade, CEP, taxa,
        // pagamento e total são implementadas pelos responsáveis dessas tarefas.
        pedido.setModalidade(dados.getModalidade());
        pedido.setEnderecoEntrega(dados.getEnderecoEntrega());
        pedido.setFormaPagamento(dados.getFormaPagamento());

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
            // O preço depende do preparo (assada ou pré-assada),
            // nunca da quantidade de sabores (inteira ou meio a meio).
            precoUnitario = item.getTipoPreparo() == ItemPedido.TipoPreparo.PRE_ASSADA
                    ? obterPrecoPreAssada()
                    : obterPrecoBasePizza();
        } else {

            if (primeiro.getPreco() == null) {
                throw new IllegalArgumentException(
                        "O produto precisa possuir um preço.");
            }

            precoUnitario = primeiro.getPreco();
        }

        item.setPrecoUnitario(precoUnitario);

        // Persistimos observações e acréscimos; cada ingrediente extra custa R$ 5,00.
        item.setObservacao(dados.getObservacao());

        List<String> acrescimos =
        normalizarIngredientes(dados.getAcrescimos(), "acréscimos");

        List<String> removidos =
                normalizarIngredientes(
                        dados.getIngredientesRemovidos(),
                        "ingredientes removidos");

        item.setAcrescimos(acrescimos);
        item.setIngredientesRemovidos(removidos);

        // Cada ingrediente adicional custa R$ 5,00.
        BigDecimal valorAdicional =
                PRECO_POR_ACRESCIMO.multiply(
                        BigDecimal.valueOf(acrescimos.size()));

        // Preço unitário com os adicionais.
        BigDecimal precoFinalUnitario =
                precoUnitario.add(valorAdicional);

        item.setPrecoUnitario(precoFinalUnitario);

        // Subtotal considera também a quantidade.

        item.setSubtotal(
                precoFinalUnitario.multiply(
                        BigDecimal.valueOf(quantidade)));

        return item;
    }

    private List<String> normalizarIngredientes(
        List<String> ingredientes,
        String descricao) {

    if (ingredientes == null) {
        return new ArrayList<>();
    }

    List<String> resultado = new ArrayList<>();

    for (String ingrediente : ingredientes) {

        if (ingrediente == null || ingrediente.isBlank()) {
            throw new IllegalArgumentException(
                    "Existem ingredientes inválidos em " + descricao + ".");
        }

        resultado.add(ingrediente.trim());
    }

        return resultado;
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
        return lerPrecoConfigurado(precoBaseConfigurado, "Preço-base das pizzas");
    }

    private BigDecimal obterPrecoPreAssada() {
        // Padrão compatível: sem preço específico, mantém o preço-base.
        if (precoPreAssadaConfigurado == null || precoPreAssadaConfigurado.isBlank()) {
            return obterPrecoBasePizza();
        }
        return lerPrecoConfigurado(precoPreAssadaConfigurado, "Preço das pizzas pré-assadas");
    }

    /** Preços oficiais da API, definidos pelo preparo e não pelo número de sabores. */
    public Map<String, BigDecimal> consultarPrecosPizza() {
        return Map.of(
                "precoBase", obterPrecoBasePizza(),
                "precoPreAssada", obterPrecoPreAssada());
    }

    private BigDecimal lerPrecoConfigurado(String configuracao, String descricao) {
        if (configuracao == null || configuracao.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    descricao + " ainda não configurado pela pizzaria.");
        }
        try {
            BigDecimal preco = new BigDecimal(configuracao.trim())
                    .setScale(2, RoundingMode.UNNECESSARY);
            if (preco.signum() <= 0) throw new NumberFormatException("Preço não positivo");
            return preco;
        } catch (NumberFormatException | ArithmeticException erro) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    descricao + " inválido. Configure um valor positivo com até duas casas decimais.");
        }
    }

    /** Pedidos abertos, com dados do cliente, pesquisáveis por id, nome, telefone ou matrícula. */
    public List<PedidoAbertoResponse> pesquisarPedidosAbertos(String termo) {
        List<Pedido> abertos = pedidoRepository.findBySituacaoOrderByCriadoEmDesc("EM_ABERTO");
        Map<String, Cliente> clientesPorId = new HashMap<>();
        List<String> ids = abertos.stream().map(Pedido::getClienteId)
                .filter(id -> id != null && !id.isBlank()).distinct().toList();
        clienteRepository.findAllById(ids).forEach(c -> clientesPorId.put(c.getId(), c));

        String consulta = termo == null ? "" : termo.strip().toLowerCase(Locale.ROOT);
        return abertos.stream().map(pedido -> {
            Cliente cliente = clientesPorId.get(pedido.getClienteId());
            return new PedidoAbertoResponse(pedido,
                    cliente != null ? cliente.getNome() : "Cliente não encontrado",
                    cliente != null ? cliente.getTelefone() : "");
        }).filter(resumo -> consulta.isEmpty()
                || contem(resumo.pedido().getId(), consulta)
                || contem(resumo.pedido().getClienteId(), consulta)
                || contem(resumo.pedido().getColaboradorMatricula(), consulta)
                || contem(resumo.clienteNome(), consulta)
                || contem(resumo.clienteTelefone(), consulta))
                .collect(Collectors.toList());
    }

    private boolean contem(String valor, String termo) {
        return valor != null && valor.toLowerCase(Locale.ROOT).contains(termo);
    }

    public List<Pedido> listarTodos() {
        return pedidoRepository.findAll();
    }
}