package PI.Pizzaria.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import PI.Pizzaria.model.Pizza;
import PI.Pizzaria.service.PizzaService;

@RestController
public class PizzaController {

    private final PizzaService pizzaService;

    public PizzaController(PizzaService pizzaService) {
        this.pizzaService = pizzaService;
    }

    @GetMapping("/pizzas")
    public List<Pizza> listarPizzas(){
        return pizzaService.listarPizzas();
    }

    @GetMapping("/pizzas/{id}")
    public Pizza buscarPoId(@PathVariable String id){
        return pizzaService.buscarPorId(id);
    }

}
