package PI.Pizzaria.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "pizzas")
public class Pizza {

    @Id
    public String id;
    public String nome;
    public String sabor;
    public int tamanho;
    public Double preco;
    
}
