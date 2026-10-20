package PI.Pizzaria.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import PI.Pizzaria.model.Cliente;

public interface ClienteRepository extends MongoRepository<Cliente, String> {

    List<Cliente> findByTelefone(String telefone);

    List<Cliente> findByNomeContainingIgnoreCase(String nome);

    boolean existsByCpf(String cpf);
}