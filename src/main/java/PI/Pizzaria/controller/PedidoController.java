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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import PI.Pizzaria.dto.PedidoRequest;
import PI.Pizzaria.model.Colaborador;
import PI.Pizzaria.model.Pedido;
import PI.Pizzaria.service.PedidoService;
import org.springframework.web.bind.annotation.DeleteMapping;

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

    @PutMapping("/{id}")
    public ResponseEntity<Pedido> atualizar(@PathVariable String id, @RequestBody PedidoRequest dados,
                                           @RequestAttribute("usuarioAutenticado") Colaborador usuario) {
        return ResponseEntity.ok(pedidoService.atualizar(id, dados, usuario));
    }

    @PutMapping("/admin/{id}")
    public ResponseEntity<Pedido> atualizarComoAdministrador(
        @PathVariable String id,
        @RequestBody PedidoRequest dados,
        @RequestAttribute("usuarioAutenticado") Colaborador administrador) {

    return ResponseEntity.ok(
            pedidoService.atualizarComoAdministrador(id, dados, administrador));
}

    @DeleteMapping("/admin/{id}")
    public ResponseEntity<Void> excluirComoAdministrador(
        @PathVariable String id,
        @RequestAttribute("usuarioAutenticado") Colaborador administrador) {

    pedidoService.excluirComoAdministrador(id, administrador);
    return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/finalizar")
    public ResponseEntity<Pedido> finalizar(@PathVariable String id,
                                           @RequestAttribute("usuarioAutenticado") Colaborador usuario) {
        return ResponseEntity.ok(pedidoService.finalizar(id, usuario));
    }

    @GetMapping("/taxa-entrega")
    public ResponseEntity<Map<String, BigDecimal>> consultarTaxaEntrega() {
        Map<String, BigDecimal> resposta = new java.util.HashMap<>();
        resposta.put("taxaEntrega", pedidoService.consultarTaxaEntrega());
        return ResponseEntity.ok(resposta);
    }

    @GetMapping("/abertos")
    public ResponseEntity<List<PedidoAbertoResponse>> buscarAbertos(
            @RequestParam(defaultValue = "") String termo) {
        return ResponseEntity.ok(pedidoService.pesquisarPedidosAbertos(termo));
    }

    @GetMapping("/finalizados")
    public ResponseEntity<List<PedidoAbertoResponse>> buscarFinalizados(
            @RequestParam(defaultValue = "") String termo) {
        return ResponseEntity.ok(pedidoService.pesquisarPedidosFinalizados(termo));
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
