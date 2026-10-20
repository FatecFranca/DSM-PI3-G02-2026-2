package PI.Pizzaria.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import PI.Pizzaria.model.Colaborador;
import java.util.Optional;

public interface ColaboradorRepository extends MongoRepository<Colaborador, String> {
    
    Optional<Colaborador> findByMatricula(String matricula);
    Optional<Colaborador> findByEmail(String email);

    boolean existsByMatricula(String matricula);
    boolean existsByEmail(String email);

}