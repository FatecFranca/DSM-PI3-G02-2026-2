package PI.Pizzaria.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import PI.Pizzaria.model.ProdutoAuditoria;

public interface ProdutoAuditoriaRepository extends MongoRepository<ProdutoAuditoria, String> {}
