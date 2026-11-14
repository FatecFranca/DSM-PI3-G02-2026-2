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
import PI.Pizzaria.dto.CorrecaoPedidoDTO;
import PI.Pizzaria.model.Colaborador;
import PI.Pizzaria.model.Pedido;
import PI.Pizzaria.service.PedidoService;
import PI.Pizzaria.service.AuditoriaService;
import org.springframework.web.bind.annotation.DeleteMapping;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {
    private final PedidoService pedidoService;
    private final AuditoriaService auditoriaService;

    public PedidoController(PedidoService pedidoService, AuditoriaService auditoriaService) {
        this.pedidoService = pedidoService;
        this.auditoriaService = auditoriaService;
    }

    @PostMapping
    public ResponseEntity<Pedido> criar(@RequestBody PedidoRequest dados,
                                        @RequestAttribute("usuarioAutenticado") Colaborador usuario) {
        Pedido criado = pedidoService.criar(dados, usuario);
        auditoriaService.registrarPedido(usuario, "CRIACAO_PEDIDO", criado);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Pedido> atualizar(@PathVariable String id, @RequestBody PedidoRequest dados,
                                           @RequestAttribute("usuarioAutenticado") Colaborador usuario) {
        Pedido atualizado = pedidoService.atualizar(id, dados, usuario);
        auditoriaService.registrarPedido(usuario, "ALTERACAO_PEDIDO_ABERTO", atualizado);
        return ResponseEntity.ok(atualizado);
    }

    @PutMapping("/admin/{id}")
    public ResponseEntity<Pedido> atualizarComoAdministrador(
        @PathVariable String id,
        @RequestBody PedidoRequest dados,
        @RequestAttribute("usuarioAutenticado") Colaborador administrador) {

    Pedido atualizado = pedidoService.atualizarComoAdministrador(id, dados, administrador);
    auditoriaService.registrarPedido(administrador, "ALTERACAO_PEDIDO", atualizado);
    return ResponseEntity.ok(atualizado);
}

    @DeleteMapping("/admin/{id}")
    public ResponseEntity<Void> excluirComoAdministrador(
        @PathVariable String id,
        @RequestAttribute("usuarioAutenticado") Colaborador administrador) {

    Pedido pedido = pedidoService.buscarPorId(id);
    pedidoService.excluirComoAdministrador(id, administrador);
    auditoriaService.registrarPedido(administrador, "EXCLUSAO_PEDIDO", pedido);
    return ResponseEntity.noContent().build();
    }

    @PutMapping("/admin/{id}/corrigir-valor")
    public ResponseEntity<Pedido> corrigirValorComoAdministrador(
            @PathVariable String id, @RequestBody CorrecaoPedidoDTO dados,
            @RequestAttribute("usuarioAutenticado") Colaborador administrador) {
        Pedido anterior = pedidoService.buscarPorId(id);
        BigDecimal valorAnterior = anterior.getTotal();
        Pedido atualizado = pedidoService.corrigirValor(id, dados, administrador);
        String motivo = dados.getMotivo().trim();
        auditoriaService.registrarPedido(administrador, "CORRECAO_VALOR_PEDIDO", atualizado,
                "De " + valorAnterior + " para " + atualizado.getTotal() + ". Motivo: " + motivo);
        return ResponseEntity.ok(atualizado);
    }

    @PostMapping("/{id}/finalizar")
    public ResponseEntity<Pedido> finalizar(@PathVariable String id,
                                           @RequestAttribute("usuarioAutenticado") Colaborador usuario) {
        Pedido finalizado = pedidoService.finalizar(id, usuario);
        auditoriaService.registrarPedido(usuario, "FINALIZACAO_PEDIDO", finalizado);
        return ResponseEntity.ok(finalizado);
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
