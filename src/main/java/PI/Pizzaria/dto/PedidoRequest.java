package PI.Pizzaria.dto;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonAnySetter;

/** Somente os campos da primeira parte da Sprint 3; não aceita preço nem atendente do navegador. */
public class PedidoRequest {
    private String clienteId;
    private List<ItemPedidoRequest> itens;

    public String getClienteId() { return clienteId; }
    public void setClienteId(String clienteId) { this.clienteId = clienteId; }

    public List<ItemPedidoRequest> getItens() { return itens; }
    public void setItens(List<ItemPedidoRequest> itens) { this.itens = itens; }

    @JsonAnySetter
    public void campoNaoSuportado(String nome, Object valor) {
        throw new IllegalArgumentException("Campo de pedido ainda não suportado nesta etapa: " + nome);
    }
}
