package PI.Pizzaria.controller;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import PI.Pizzaria.model.Auditoria;
import PI.Pizzaria.service.AuditoriaService;

@RestController
@RequestMapping("/auditoria")
public class AuditoriaController {
    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    public List<RegistroAuditoria> listar(
            @RequestParam(defaultValue = "") String termo,
            @RequestParam(defaultValue = "") String de,
            @RequestParam(defaultValue = "") String ate) {
        LocalDate inicio = lerData(de);
        LocalDate fim = lerData(ate);
        if (inicio != null && fim != null && inicio.isAfter(fim)) {
            throw new IllegalArgumentException("A data inicial não pode ser posterior à final.");
        }
        String filtro = termo.trim().toLowerCase(Locale.ROOT);
        return auditoriaService.listarMaisRecentes().stream()
                .filter(a -> a.getDataHora() != null)
                .filter(a -> inicio == null || !a.getDataHora().isBefore(inicio.atStartOfDay().toInstant(ZoneOffset.UTC)))
                .filter(a -> fim == null || a.getDataHora().isBefore(fim.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)))
                .filter(a -> filtro.isBlank()
                        || contem(a.getUsuarioMatricula(), filtro)
                        || contem(a.getAcao(), filtro)
                        || contem(a.getTipoRegistro(), filtro)
                        || contem(a.getRegistroId(), filtro))
                .map(a -> new RegistroAuditoria(a.getDataHora(),
                        a.getUsuarioMatricula(), a.getAcao(),
                        a.getTipoRegistro() + " #" + a.getRegistroId()))
                .toList();
    }

    private LocalDate lerData(String valor) {
        if (valor == null || valor.isBlank()) return null;
        try {
            return LocalDate.parse(valor);
        } catch (DateTimeParseException erro) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use datas no formato AAAA-MM-DD.");
        }
    }

    private boolean contem(String texto, String filtro) {
        return texto != null && texto.toLowerCase(Locale.ROOT).contains(filtro);
    }

    public record RegistroAuditoria(java.time.Instant dataHora, String usuario,
                                   String acao, String registro) {}
}
