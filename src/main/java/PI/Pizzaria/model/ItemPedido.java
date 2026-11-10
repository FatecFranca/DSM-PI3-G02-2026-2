package PI.Pizzaria.model;

import java.math.BigDecimal;

/** Item registrado com um retrato dos sabores e valores no momento da criação. */
public class ItemPedido {
    public enum TipoPreparo { ASSADA, PRE_ASSADA }

    private String produtoId;
    private String sabor;
    private String segundoProdutoId;
    private String segundoSabor;
    private boolean meioAMeio;
    private TipoPreparo tipoPreparo;
    private int quantidade;
    private BigDecimal precoUnitario;
    private BigDecimal subtotal;

    public String getProdutoId() { return produtoId; }
    public void setProdutoId(String produtoId) { this.produtoId = produtoId; }
    public String getSabor() { return sabor; }
    public void setSabor(String sabor) { this.sabor = sabor; }
    public String getSegundoProdutoId() { return segundoProdutoId; }
    public void setSegundoProdutoId(String segundoProdutoId) { this.segundoProdutoId = segundoProdutoId; }
    public String getSegundoSabor() { return segundoSabor; }
    public void setSegundoSabor(String segundoSabor) { this.segundoSabor = segundoSabor; }
    public boolean isMeioAMeio() { return meioAMeio; }
    public void setMeioAMeio(boolean meioAMeio) { this.meioAMeio = meioAMeio; }
    public TipoPreparo getTipoPreparo() { return tipoPreparo; }
    public void setTipoPreparo(TipoPreparo tipoPreparo) { this.tipoPreparo = tipoPreparo; }
    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }
    public BigDecimal getPrecoUnitario() { return precoUnitario; }
    public void setPrecoUnitario(BigDecimal precoUnitario) { this.precoUnitario = precoUnitario; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
}
