package PI.Pizzaria.dto;

import java.math.BigDecimal;

public class CorrecaoPedidoDTO {
    private String motivo;
    private BigDecimal novoValorTotal;
    private String novoStatus;

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public BigDecimal getNovoValorTotal() { return novoValorTotal; }
    public void setNovoValorTotal(BigDecimal novoValorTotal) { this.novoValorTotal = novoValorTotal; }
    
    public String getNovoStatus() { return novoStatus; }
    public void setNovoStatus(String novoStatus) { this.novoStatus = novoStatus; }
}