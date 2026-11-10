
package PI.Pizzaria.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import PI.Pizzaria.model.Cliente;
import PI.Pizzaria.service.ClienteService;

@RestController
@RequestMapping("/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @PostMapping
    public ResponseEntity<Cliente> cadastrar(
            @RequestBody Cliente cliente) {

        Cliente novoCliente = clienteService.cadastrar(cliente);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(novoCliente);
    }

    @GetMapping("/{termo}")
    public List<Cliente> buscar(@PathVariable String termo) {
        return clienteService.buscar(termo);
    }
}
