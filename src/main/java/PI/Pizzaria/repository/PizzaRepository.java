package PI.Pizzaria.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import PI.Pizzaria.model.Pizza;

public interface PizzaRepository extends MongoRepository <Pizza, String>{


    

}
