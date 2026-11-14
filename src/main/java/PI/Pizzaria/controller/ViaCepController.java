
package PI.Pizzaria.controller;

import PI.Pizzaria.model.Endereco;
import PI.Pizzaria.service.ViaCepService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cep")
public class ViaCepController {

    private final ViaCepService viaCepService;

    public ViaCepController(ViaCepService viaCepService) {
        this.viaCepService = viaCepService;
    }

@GetMapping("/{cep}")
public ResponseEntity<?> consultarCep(@PathVariable String cep) {
    try {
        Endereco endereco = viaCepService.consultarCep(cep);
        return ResponseEntity.ok(endereco);

    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());

    } catch (RuntimeException e) {
        return ResponseEntity.status(502).body(e.getMessage());
    }
}
}
