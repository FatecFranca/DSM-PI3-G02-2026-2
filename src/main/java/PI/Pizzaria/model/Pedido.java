package PI.Pizzaria.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** Pedido com dados persistidos das Sprints 3 e 4. */
@Document(collection = "pedidos")
public class Pedido {
    @Id
    private String id;
    private String clienteId;
    private String colaboradorId;
    private String colaboradorMatricula;
    private Instant criadoEm;
    private List<ItemPedido> itens;
    private BigDecimal subtotalItens;
    private String modalidade;
    private Endereco enderecoEntrega;
    private BigDecimal taxaEntrega;
    private String formaPagamento;
    private BigDecimal total;
    private String situacao;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getClienteId() { return clienteId; }
    public void setClienteId(String clienteId) { this.clienteId = clienteId; }
    public String getColaboradorId() { return colaboradorId; }
    public void setColaboradorId(String colaboradorId) { this.colaboradorId = colaboradorId; }
    public String getColaboradorMatricula() { return colaboradorMatricula; }
    public void setColaboradorMatricula(String colaboradorMatricula) { this.colaboradorMatricula = colaboradorMatricula; }
    public Instant getCriadoEm() { return criadoEm; }
    public void setCriadoEm(Instant criadoEm) { this.criadoEm = criadoEm; }
    public List<ItemPedido> getItens() { return itens; }
    public void setItens(List<ItemPedido> itens) { this.itens = itens; }
    public BigDecimal getSubtotalItens() { return subtotalItens; }
    public void setSubtotalItens(BigDecimal subtotalItens) { this.subtotalItens = subtotalItens; }
    public String getModalidade() { return modalidade; }
    public void setModalidade(String modalidade) { this.modalidade = modalidade; }
    public Endereco getEnderecoEntrega() { return enderecoEntrega; }
    public void setEnderecoEntrega(Endereco enderecoEntrega) { this.enderecoEntrega = enderecoEntrega; }
    public BigDecimal getTaxaEntrega() { return taxaEntrega; }
    public void setTaxaEntrega(BigDecimal taxaEntrega) { this.taxaEntrega = taxaEntrega; }
    public String getFormaPagamento() { return formaPagamento; }
    public void setFormaPagamento(String formaPagamento) { this.formaPagamento = formaPagamento; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public String getSituacao() { return situacao; }
    public void setSituacao(String situacao) { this.situacao = situacao; }
}
