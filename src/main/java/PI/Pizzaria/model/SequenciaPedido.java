package PI.Pizzaria.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** Contador persistente no MongoDB para numerar os pedidos sem depender do navegador. */
@Document(collection = "sequencias")
public class SequenciaPedido {
    @Id
    private String id;
    private long ultimoNumero;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public long getUltimoNumero() { return ultimoNumero; }
    public void setUltimoNumero(long ultimoNumero) { this.ultimoNumero = ultimoNumero; }
}
