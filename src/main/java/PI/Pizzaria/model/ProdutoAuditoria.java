package PI.Pizzaria.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** Rastro básico das operações do catálogo para PB07 / RF12. */
@Document(collection = "auditoria_produtos")
public class ProdutoAuditoria {
    @Id private String id;
    private String produtoId;
    private String nomeProduto;
    private String acao;
    private String colaboradorId;
    private Instant ocorridoEm;

    public ProdutoAuditoria() {}
    public ProdutoAuditoria(Produto produto, String acao, Colaborador colaborador) {
        this.produtoId = produto.getId();
        this.nomeProduto = produto.getNome();
        this.acao = acao;
        this.colaboradorId = colaborador.getId();
        this.ocorridoEm = Instant.now();
    }
    public String getId() { return id; }
    public String getProdutoId() { return produtoId; }
    public String getNomeProduto() { return nomeProduto; }
    public String getAcao() { return acao; }
    public String getColaboradorId() { return colaboradorId; }
    public Instant getOcorridoEm() { return ocorridoEm; }
}
