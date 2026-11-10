package PI.Pizzaria.dto;

import PI.Pizzaria.model.Pedido;

/** Dados básicos do cliente combinados ao pedido ainda aberto. */
public record PedidoAbertoResponse(Pedido pedido, String clienteNome, String clienteTelefone) {}
