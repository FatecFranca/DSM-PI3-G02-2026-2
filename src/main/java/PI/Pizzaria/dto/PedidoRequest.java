package PI.Pizzaria.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import PI.Pizzaria.model.Endereco;

/**
 * Dados recebidos para criação/atualização do pedido.
 * Valores calculados pelo back-end, como taxa e total,
 * não são recebidos diretamente do navegador.
 */
public class PedidoRequest {
    private String clienteId;
    private List<ItemPedidoRequest> itens;
    private String modalidade;
    private Endereco enderecoEntrega;
    private String formaPagamento;
    private String telefoneContato;
    private String nomeRetirada;
    private String observacaoGeral;

    public String getClienteId() { return clienteId; }
    public void setClienteId(String clienteId) { this.clienteId = clienteId; }

    public List<ItemPedidoRequest> getItens() { return itens; }
    public void setItens(List<ItemPedidoRequest> itens) { this.itens = itens; }

    public String getModalidade() { return modalidade; }
    public void setModalidade(String modalidade) { this.modalidade = modalidade; }

    public Endereco getEnderecoEntrega() { return enderecoEntrega; }
    public void setEnderecoEntrega(Endereco enderecoEntrega) { this.enderecoEntrega = enderecoEntrega; }

    public String getFormaPagamento() { return formaPagamento; }
    public void setFormaPagamento(String formaPagamento) { this.formaPagamento = formaPagamento; }

    public String getTelefoneContato() { return telefoneContato; }
    public void setTelefoneContato(String telefoneContato) { this.telefoneContato = telefoneContato; }
    public String getNomeRetirada() { return nomeRetirada; }
    public void setNomeRetirada(String nomeRetirada) { this.nomeRetirada = nomeRetirada; }
    public String getObservacaoGeral() { return observacaoGeral; }
    public void setObservacaoGeral(String observacaoGeral) { this.observacaoGeral = observacaoGeral; }

    @JsonAnySetter
    public void campoNaoSuportado(String nome, Object valor) {
        throw new IllegalArgumentException("Campo de pedido ainda não suportado nesta etapa: " + nome);
    }
}
