package PI.Pizzaria.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import PI.Pizzaria.model.Pedido;

public interface PedidoRepository extends MongoRepository<Pedido, String> {
}
