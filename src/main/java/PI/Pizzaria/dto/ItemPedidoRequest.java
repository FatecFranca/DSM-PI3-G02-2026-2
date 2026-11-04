package PI.Pizzaria.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import PI.Pizzaria.model.ItemPedido.TipoPreparo;

/** Dados da pizza escolhida. Valor, sabor e funcionário são definidos pelo servidor. */
public class ItemPedidoRequest {
    private String produtoId;
    private String segundoProdutoId;
    private boolean meioAMeio;
    private TipoPreparo tipoPreparo;
    private Integer quantidade;

    public String getProdutoId() { return produtoId; }
    public void setProdutoId(String produtoId) { this.produtoId = produtoId; }

    public String getSegundoProdutoId() { return segundoProdutoId; }
    public void setSegundoProdutoId(String segundoProdutoId) { this.segundoProdutoId = segundoProdutoId; }

    public boolean isMeioAMeio() { return meioAMeio; }
    public void setMeioAMeio(boolean meioAMeio) { this.meioAMeio = meioAMeio; }

    public TipoPreparo getTipoPreparo() { return tipoPreparo; }
    public void setTipoPreparo(TipoPreparo tipoPreparo) { this.tipoPreparo = tipoPreparo; }

    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }

    @JsonAnySetter
    public void campoNaoSuportado(String nome, Object valor) {
        throw new IllegalArgumentException("Campo do item ainda não suportado nesta etapa: " + nome);
    }
}
