package PI.Pizzaria.controller;

import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import PI.Pizzaria.dto.LoginRequest;
import PI.Pizzaria.dto.LoginResponse;
import PI.Pizzaria.dto.RecuperarSenhaRequest;
import PI.Pizzaria.dto.RedefinirSenhaRequest;
import PI.Pizzaria.dto.RedefinicaoAdminRequest;
import PI.Pizzaria.model.Colaborador;
import PI.Pizzaria.service.AuthServicy;
import PI.Pizzaria.service.SessaoService;
import PI.Pizzaria.service.AuditoriaService;
import org.springframework.web.bind.annotation.RequestAttribute;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthServicy authServicy;
    private final SessaoService sessaoService;
    private final AuditoriaService auditoriaService;

    public AuthController(AuthServicy authServicy, SessaoService sessaoService,
                          AuditoriaService auditoriaService) {
        this.authServicy = authServicy;
        this.sessaoService = sessaoService;
        this.auditoriaService = auditoriaService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest dados) {
        Optional<Colaborador> colaborador = authServicy.autenticar(dados.getMatricula(), dados.getSenha());
        if (colaborador.isEmpty())
            return ResponseEntity.status(401).body("Matrícula ou senha incorreta.");
        Colaborador usuario = colaborador.get();
        return ResponseEntity.ok(new LoginResponse(usuario.getMatricula(), usuario.getPerfil().name(),
                usuario.getNome(), authServicy.criarSessao(usuario)));
    }

    @PostMapping("/recuperar-senha")
    public ResponseEntity<String> recuperarSenha(@RequestBody RecuperarSenhaRequest request) {
        authServicy.solicitarRecuperacao(request.getEmail());
        return ResponseEntity.ok("Se o e-mail estiver cadastrado, o código será enviado.");
    }

    @PostMapping("/redefinir-senha")
    public ResponseEntity<String> redefinirSenha(@RequestBody RedefinirSenhaRequest request) {
        authServicy.redefinirSenha(request.getEmail(), request.getCodigo(), request.getNovaSenha());
        return ResponseEntity.ok("Senha redefinida com sucesso.");
    }

    @PostMapping("/admin/redefinir-senha")
    public ResponseEntity<String> redefinirSenhaPeloAdministrador(
            @RequestBody RedefinicaoAdminRequest request,
            @RequestAttribute("usuarioAutenticado") Colaborador administrador) {
        // A permissão de administrador é checada no interceptor do back-end.
        authServicy.redefinirSenhaPeloAdministrador(request.getColaboradorId(), request.getNovaSenha());
        auditoriaService.registrar(administrador, "REDEFINICAO_SENHA", "COLABORADOR", request.getColaboradorId());
        return ResponseEntity.ok("Senha redefinida pelo administrador.");
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String autorizacao) {
        sessaoService.encerrar(autorizacao);
        return ResponseEntity.noContent().build();
    }
}
