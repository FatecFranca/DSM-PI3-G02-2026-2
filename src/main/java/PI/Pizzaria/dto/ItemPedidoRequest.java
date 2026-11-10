package PI.Pizzaria.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import PI.Pizzaria.model.ItemPedido.TipoPreparo;

/** Dados do item escolhido no pedido. */
public class ItemPedidoRequest {

    private String produtoId;
    private String segundoProdutoId;
    private boolean meioAMeio;
    private TipoPreparo tipoPreparo;
    private Integer quantidade;

    private String observacao;
    private List<String> acrescimos;
    private List<String> ingredientesRemovidos;

    public String getProdutoId() {
        return produtoId;
    }

    public void setProdutoId(String produtoId) {
        this.produtoId = produtoId;
    }

    public String getSegundoProdutoId() {
        return segundoProdutoId;
    }

    public void setSegundoProdutoId(String segundoProdutoId) {
        this.segundoProdutoId = segundoProdutoId;
    }

    public boolean isMeioAMeio() {
        return meioAMeio;
    }

    public void setMeioAMeio(boolean meioAMeio) {
        this.meioAMeio = meioAMeio;
    }

    public TipoPreparo getTipoPreparo() {
        return tipoPreparo;
    }

    public void setTipoPreparo(TipoPreparo tipoPreparo) {
        this.tipoPreparo = tipoPreparo;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public List<String> getAcrescimos() {
        return acrescimos;
    }

    public void setAcrescimos(List<String> acrescimos) {
        this.acrescimos = acrescimos;
    }

    public List<String> getIngredientesRemovidos() {
    return ingredientesRemovidos;
    }

    public void setIngredientesRemovidos(List<String> ingredientesRemovidos) {
        this.ingredientesRemovidos = ingredientesRemovidos;
    }

    @JsonAnySetter
    public void campoNaoSuportado(String nome, Object valor) {
        throw new IllegalArgumentException(
                "Campo do item ainda não suportado nesta etapa: " + nome);
    }
}