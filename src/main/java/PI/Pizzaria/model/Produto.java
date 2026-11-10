package PI.Pizzaria.model;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

@Document(collection = "produtos")
public class Produto {

    @Id
    private String id;

    private String nome;

    private String categoria;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal preco;

    private int estoque;

    private String apresentacaoVolume;

    private String sabor;

    private String criadoPorId;

    private Instant criadoEm;

    private String alteradoPorId;

    private Instant alteradoEm;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public void setPreco(BigDecimal preco) {
        this.preco = preco;
    }

    public int getEstoque() {
        return estoque;
    }

    public void setEstoque(int estoque) {
        this.estoque = estoque;
    }

    public String getApresentacaoVolume() {
        return apresentacaoVolume;
    }

    public void setApresentacaoVolume(String apresentacaoVolume) {
        this.apresentacaoVolume = apresentacaoVolume;
    }

    public String getSabor() {
        return sabor;
    }

    public void setSabor(String sabor) {
        this.sabor = sabor;
    }

    public String getCriadoPorId() {
        return criadoPorId;
    }

    public void setCriadoPorId(String criadoPorId) {
        this.criadoPorId = criadoPorId;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(Instant criadoEm) {
        this.criadoEm = criadoEm;
    }

    public String getAlteradoPorId() {
        return alteradoPorId;
    }

    public void setAlteradoPorId(String alteradoPorId) {
        this.alteradoPorId = alteradoPorId;
    }

    public Instant getAlteradoEm() {
        return alteradoEm;
    }

    public void setAlteradoEm(Instant alteradoEm) {
        this.alteradoEm = alteradoEm;
    }
}