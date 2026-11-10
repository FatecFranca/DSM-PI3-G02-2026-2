package PI.Pizzaria.model;

import java.math.BigDecimal;
import java.util.List;

/** Item registrado com um retrato dos sabores e valores no momento da criação. */
public class ItemPedido {

    public enum TipoPreparo {
        ASSADA,
        PRE_ASSADA
    }

    private String produtoId;

    private String nomeProduto;

    private String categoria;

    private String sabor;

    private String segundoProdutoId;

    private String segundoSabor;

    private boolean meioAMeio;

    private TipoPreparo tipoPreparo;

    private int quantidade;

    private BigDecimal precoUnitario;

    private BigDecimal subtotal;

    private String observacao;

    private List<String> acrescimos;

    private List<String> ingredientesRemovidos;

    public String getProdutoId() {
        return produtoId;
    }

    public void setProdutoId(String produtoId) {
        this.produtoId = produtoId;
    }

    public String getNomeProduto() {
        return nomeProduto;
    }

    public void setNomeProduto(String nomeProduto) {
        this.nomeProduto = nomeProduto;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getSabor() {
        return sabor;
    }

    public void setSabor(String sabor) {
        this.sabor = sabor;
    }

    public String getSegundoProdutoId() {
        return segundoProdutoId;
    }

    public void setSegundoProdutoId(String segundoProdutoId) {
        this.segundoProdutoId = segundoProdutoId;
    }

    public String getSegundoSabor() {
        return segundoSabor;
    }

    public void setSegundoSabor(String segundoSabor) {
        this.segundoSabor = segundoSabor;
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

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public void setPrecoUnitario(BigDecimal precoUnitario) {
        this.precoUnitario = precoUnitario;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
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

}