package PI.Pizzaria.controller;

import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
import org.springframework.web.bind.annotation.RequestParam;
import PI.Pizzaria.dto.PedidoAbertoResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import PI.Pizzaria.dto.PedidoRequest;
import PI.Pizzaria.model.Colaborador;
import PI.Pizzaria.model.Pedido;
import PI.Pizzaria.service.PedidoService;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {
    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PostMapping
    public ResponseEntity<Pedido> criar(@RequestBody PedidoRequest dados,
                                        @RequestAttribute("usuarioAutenticado") Colaborador usuario) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pedidoService.criar(dados, usuario));
    }

    @GetMapping("/abertos")
    public ResponseEntity<List<PedidoAbertoResponse>> buscarAbertos(
            @RequestParam(defaultValue = "") String termo) {
        return ResponseEntity.ok(pedidoService.pesquisarPedidosAbertos(termo));
    }

    @GetMapping("/precos")
    public ResponseEntity<Map<String, BigDecimal>> consultarPrecos() {
        return ResponseEntity.ok(pedidoService.consultarPrecosPizza());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pedido> buscarPorId(@PathVariable String id) {
        return ResponseEntity.ok(pedidoService.buscarPorId(id));
    }

    @GetMapping
    public ResponseEntity<List<Pedido>> listarTodos() {
        return ResponseEntity.ok(pedidoService.listarTodos());
    }
}
