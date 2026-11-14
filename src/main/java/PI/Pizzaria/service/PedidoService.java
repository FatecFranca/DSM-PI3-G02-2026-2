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
import PI.Pizzaria.model.Endereco;

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
    private final String taxaEntregaConfigurada;
    private static final BigDecimal PRECO_POR_ACRESCIMO =
        new BigDecimal("5.00");

    public PedidoService(
            PedidoRepository pedidoRepository,
            ClienteRepository clienteRepository,
            ProdutoRepository produtoRepository,
            @Value("${app.pizza.preco-base:}") String precoBaseConfigurado,
            @Value("${app.pizza.preco-pre-assada:}") String precoPreAssadaConfigurado,
            @Value("${app.pedido.taxa-entrega:}") String taxaEntregaConfigurada) {

        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
        this.produtoRepository = produtoRepository;
        this.precoBaseConfigurado = precoBaseConfigurado;
        this.precoPreAssadaConfigurado = precoPreAssadaConfigurado;
        this.taxaEntregaConfigurada = taxaEntregaConfigurada;
    }

    /** O preço, a taxa e o total são sempre calculados pelo servidor. */
    public Pedido criar(PedidoRequest dados, Colaborador atendente) {
        validarAtendente(atendente);
        Pedido pedido = new Pedido();
        pedido.setColaboradorId(atendente.getId());
        pedido.setColaboradorMatricula(atendente.getMatricula());
        pedido.setCriadoEm(Instant.now());
        pedido.setSituacao("EM_ABERTO");
        aplicarDados(pedido, dados);
        return pedidoRepository.save(pedido);
    }

    /** Reabrir e editar um pedido usa o mesmo ID: nunca duplica a venda. */
    public Pedido atualizar(String id, PedidoRequest dados, Colaborador atendente) {
        validarAtendente(atendente);
        Pedido pedido = buscarPorId(id);
        exigirAberto(pedido);
        aplicarDados(pedido, dados);
        return pedidoRepository.save(pedido);
    }

    public Pedido atualizarComoAdministrador(
        String id, PedidoRequest dados, Colaborador administrador) {

    validarAdministrador(administrador);

    Pedido pedido = buscarPorId(id);
    aplicarDados(pedido, dados);

    return pedidoRepository.save(pedido);
    }

    /** Finalização explícita — imprimir por si só não confirma impressão física. */
    public Pedido finalizar(String id, Colaborador atendente) {
        validarAtendente(atendente);
        Pedido pedido = buscarPorId(id);
        
        exigirAberto(pedido);
        if (pedido.getFormaPagamento() == null
        || !List.of("DINHEIRO", "PIX", "CREDITO", "DEBITO")
                .contains(pedido.getFormaPagamento())) {
        throw new IllegalArgumentException("Selecione uma forma de pagamento válida antes de finalizar.");
        }

        if (pedido.getItens() == null || pedido.getItens().isEmpty()) {
            throw new IllegalArgumentException("Pedido sem itens não pode ser finalizado.");
        }
        // Atualiza valores de documentos antigos ainda abertos, sem aceitar totais do navegador.
        if (pedido.getTotal() == null) {
            throw new IllegalArgumentException("Revise e salve o pedido antes de finalizar para calcular o total.");
        }
        pedido.setSituacao("FINALIZADO");
        pedido.setFinalizadoEm(Instant.now());
        pedido.setFinalizadoPorMatricula(atendente.getMatricula());
        return pedidoRepository.save(pedido);
    }

    private void exigirAberto(Pedido pedido) {
        

        if (!"EM_ABERTO".equals(pedido.getSituacao())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Este pedido já foi finalizado e não pode mais ser alterado.");
        }
    }

    private void validarAtendente(Colaborador atendente) {
        if (atendente == null || atendente.getId() == null || atendente.getId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Faça login para trabalhar com pedidos.");
        }
    }

    private void validarAdministrador(Colaborador administrador) {
    validarAtendente(administrador);

    if (administrador.getPerfil() != Colaborador.Perfil.administrador) {
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Acesso permitido apenas ao administrador.");
    }
    }

    private void aplicarDados(Pedido pedido, PedidoRequest dados) {
        if (dados == null || dados.getClienteId() == null || dados.getClienteId().isBlank()) {
            throw new IllegalArgumentException("Selecione um cliente para o pedido.");
        }
        String clienteId = dados.getClienteId().trim();
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Cliente não encontrado."));
        if (dados.getItens() == null || dados.getItens().isEmpty()) {
            throw new IllegalArgumentException("Adicione ao menos um item ao pedido.");
        }
        if (dados.getItens().size() > 50) {
            throw new IllegalArgumentException("O pedido aceita até 50 itens.");
        }
        List<ItemPedido> itens = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO.setScale(2);
        for (ItemPedidoRequest item : dados.getItens()) {
            ItemPedido salvo = montarItem(item);
            itens.add(salvo);
            subtotal = subtotal.add(salvo.getSubtotal());
        }

        String modalidade = dados.getModalidade() == null || dados.getModalidade().isBlank()
                ? "ENTREGA" : dados.getModalidade().trim().toUpperCase(Locale.ROOT);
        if (!modalidade.equals("ENTREGA") && !modalidade.equals("RETIRADA")) {
            throw new IllegalArgumentException("Escolha Entrega ou Retirada.");
        }
        String pagamento = dados.getFormaPagamento() == null || dados.getFormaPagamento().isBlank()
                ? "DINHEIRO" : dados.getFormaPagamento().trim().toUpperCase(Locale.ROOT);
        if (!List.of("DINHEIRO", "PIX", "CREDITO", "DEBITO").contains(pagamento)) {
            throw new IllegalArgumentException("Forma de pagamento inválida.");
        }
        String telefone = dados.getTelefoneContato();
        if (telefone == null || telefone.isBlank()) telefone = cliente.getTelefone();
        telefone = telefone == null ? "" : telefone.replaceAll("\\D", "");
        if (!telefone.matches("\\d{10,11}")) {
            throw new IllegalArgumentException("Informe telefone de contato com DDD (10 ou 11 dígitos).");
        }
        Endereco endereco = null;
        BigDecimal taxa = BigDecimal.ZERO.setScale(2);
        if (modalidade.equals("ENTREGA")) {
            endereco = dados.getEnderecoEntrega();
            validarEnderecoEntrega(endereco);
            taxa = obterTaxaEntrega();
        }

        pedido.setClienteId(clienteId);
        pedido.setItens(itens);
        pedido.setSubtotalItens(subtotal);
        pedido.setModalidade(modalidade);
        pedido.setEnderecoEntrega(endereco);
        pedido.setTaxaEntrega(taxa);
        pedido.setFormaPagamento(pagamento);
        pedido.setTelefoneContato(telefone);
        pedido.setNomeRetirada(modalidade.equals("RETIRADA") ? textoLimitado(dados.getNomeRetirada(), 100) : null);
        pedido.setObservacaoGeral(textoLimitado(dados.getObservacaoGeral(), 500));
        pedido.setTotal(subtotal.add(taxa).setScale(2));
    }

    private String textoLimitado(String valor, int limite) {
        if (valor == null || valor.isBlank()) return null;
        String texto = valor.trim();
        if (texto.length() > limite) throw new IllegalArgumentException("Texto ultrapassa " + limite + " caracteres.");
        return texto;
    }

    private void validarEnderecoEntrega(Endereco endereco) {
        if (endereco == null || endereco.getRua() == null || endereco.getRua().isBlank()
                || endereco.getNumero() == null || endereco.getNumero().isBlank()
                || endereco.getBairro() == null || endereco.getBairro().isBlank()
                || endereco.getCidade() == null || endereco.getCidade().isBlank()) {
            throw new IllegalArgumentException("Informe rua, número, bairro e cidade para entrega.");
        }
        String cep = endereco.getCep();
        if (cep != null && !cep.isBlank()) {
            String digitos = cep.replaceAll("\\D", "");
            if (!digitos.matches("\\d{8}")) throw new IllegalArgumentException("CEP precisa ter 8 dígitos.");
            endereco.setCep(digitos);
        }
    }

    /** Valor oficial da taxa; vazio significa não configurada, não frete grátis. */
    public BigDecimal consultarTaxaEntrega() {
        if (taxaEntregaConfigurada == null || taxaEntregaConfigurada.isBlank()) return null;
        return obterTaxaEntrega();
    }

    private BigDecimal obterTaxaEntrega() {
        if (taxaEntregaConfigurada == null || taxaEntregaConfigurada.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Configure TAXA_ENTREGA no servidor para aceitar pedidos de entrega.");
        }
        try {
            BigDecimal valor = new BigDecimal(taxaEntregaConfigurada.trim())
                    .setScale(2, RoundingMode.UNNECESSARY);
            if (valor.signum() < 0 || valor.compareTo(new BigDecimal("9999.99")) > 0)
                throw new NumberFormatException("Taxa fora dos limites");
            return valor;
        } catch (NumberFormatException | ArithmeticException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "TAXA_ENTREGA inválida. Use valor de 0.00 a 9999.99.");
        }
    }

    public Pedido buscarPorId(String id) {

        return pedidoRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Pedido não encontrado."));
    }

    public void excluirComoAdministrador(String id, Colaborador administrador) {
    validarAdministrador(administrador);

    Pedido pedido = buscarPorId(id);

    pedidoRepository.deleteById(pedido.getId());
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

    /** Pedidos abertos pesquisáveis por id, nome, telefone ou matrícula. */
    public List<PedidoAbertoResponse> pesquisarPedidosAbertos(String termo) {
        return pesquisarPorSituacao("EM_ABERTO", termo);
    }

    /** Histórico real dos pedidos finalizados, inclusive sem impressão. */
    public List<PedidoAbertoResponse> pesquisarPedidosFinalizados(String termo) {
        return pesquisarPorSituacao("FINALIZADO", termo);
    }

    private List<PedidoAbertoResponse> pesquisarPorSituacao(String situacao, String termo) {
        List<Pedido> abertos = pedidoRepository.findBySituacaoOrderByCriadoEmDesc(situacao);
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