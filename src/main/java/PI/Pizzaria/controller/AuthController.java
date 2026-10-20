package PI.Pizzaria.controller;

import java.util.Optional;
import PI.Pizzaria.dto.LoginResponse;
import PI.Pizzaria.model.Colaborador;
import PI.Pizzaria.dto.LoginRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import PI.Pizzaria.dto.RecuperarSenhaRequest;
import PI.Pizzaria.dto.RedefinirSenhaRequest;

import PI.Pizzaria.service.AuthServicy;

@RestController
@RequestMapping("/auth")

public class AuthController {
    private final AuthServicy authServicy;

    public AuthController(AuthServicy authServicy) {
        this.authServicy = authServicy;
    }

    @PostMapping("/recuperar-senha")
    public ResponseEntity<?> recuperarSenha(
            @RequestBody RecuperarSenhaRequest request) {

        String codigo = authServicy.gerarCodigoRecuperacao(
                request.getEmail());

        return ResponseEntity.ok(
                "Código de recuperação gerado: " + codigo);
    }

    @PostMapping("/redefinir-senha")
    public ResponseEntity<?> redefinirSenha(
            @RequestBody RedefinirSenhaRequest request) {

        authServicy.redefinirSenha(
                request.getEmail(),
                request.getCodigo(),
                request.getNovaSenha());

        return ResponseEntity.ok("Senha redefinida com sucesso.");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {

        Optional<Colaborador> colaborador = authServicy.autenticar(
                loginRequest.getMatricula(),
                loginRequest.getSenha());

        if (colaborador.isPresent()) {
            Colaborador usuario = colaborador.get();
            LoginResponse loginResponse = new LoginResponse(usuario.getMatricula(), usuario.getPerfil().name());
            return ResponseEntity.ok(loginResponse);
        }

        return ResponseEntity.status(401).body("Matricula ou Senha incorreta");
    }
}
