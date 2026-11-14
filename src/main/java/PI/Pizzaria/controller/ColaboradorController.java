package PI.Pizzaria.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import PI.Pizzaria.dto.ColaboradorRequestDTO;
import PI.Pizzaria.dto.ColaboradorResponseDTO;
import PI.Pizzaria.model.Colaborador;
import PI.Pizzaria.service.ColaboradorService;
import PI.Pizzaria.service.AuditoriaService;
import org.springframework.web.bind.annotation.RequestAttribute;

@RestController 
@RequestMapping("/colaboradores")

public class ColaboradorController {

    private final ColaboradorService colaboradorService;
    private final AuditoriaService auditoriaService;

    public ColaboradorController(ColaboradorService colaboradorService, AuditoriaService auditoriaService) {
        this.colaboradorService = colaboradorService;
        this.auditoriaService = auditoriaService;
    }

    @PostMapping
    public ResponseEntity<ColaboradorResponseDTO> cadastrar(
            @RequestBody ColaboradorRequestDTO dados,
            @RequestAttribute("usuarioAutenticado") Colaborador administrador) {

        Colaborador colaborador = dados.toColaborador();

        Colaborador novoColaborador = colaboradorService.cadastrar(colaborador);
        auditoriaService.registrar(administrador, "CRIACAO_COLABORADOR", "COLABORADOR", novoColaborador.getId(), novoColaborador.getNome());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ColaboradorResponseDTO(novoColaborador));
    }

    @GetMapping
    public ResponseEntity<List<ColaboradorResponseDTO>> listarTodos() {

        List<ColaboradorResponseDTO> colaboradores = colaboradorService.listarTodos()
                .stream()
                .map(ColaboradorResponseDTO::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(colaboradores);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ColaboradorResponseDTO> buscarPorId(
            @PathVariable String id) {
        Colaborador colaborador = colaboradorService.buscarPorId(id);

        return ResponseEntity.ok(
                new ColaboradorResponseDTO(colaborador));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ColaboradorResponseDTO> atualizar(
            @PathVariable String id,
            @RequestBody ColaboradorRequestDTO dados,
            @RequestAttribute("usuarioAutenticado") Colaborador administrador) {

        Colaborador colaborador = dados.toColaborador();

        Colaborador atualizado = colaboradorService.atualizar(id, colaborador);
        auditoriaService.registrar(administrador, "ALTERACAO_COLABORADOR", "COLABORADOR", id, atualizado.getNome());

        return ResponseEntity.ok(
                new ColaboradorResponseDTO(atualizado));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(
            @PathVariable String id,
            @RequestAttribute("usuarioAutenticado") Colaborador administrador) {

        String nome = colaboradorService.buscarPorId(id).getNome();
        colaboradorService.excluir(id);
        auditoriaService.registrar(administrador, "EXCLUSAO_COLABORADOR", "COLABORADOR", id, nome);

        return ResponseEntity.noContent().build();
    }

}
