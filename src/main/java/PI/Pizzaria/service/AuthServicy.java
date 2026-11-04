package PI.Pizzaria.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import PI.Pizzaria.model.Colaborador;
import PI.Pizzaria.repository.ColaboradorRepository;

@Service
public class AuthServicy {
    private final ColaboradorRepository colaboradorRepository;
    private final EmailRecuperacaoService emailRecuperacaoService;
    private final SessaoService sessaoService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();

    public AuthServicy(ColaboradorRepository colaboradorRepository,
                       EmailRecuperacaoService emailRecuperacaoService,
                       SessaoService sessaoService) {
        this.colaboradorRepository = colaboradorRepository;
        this.emailRecuperacaoService = emailRecuperacaoService;
        this.sessaoService = sessaoService;
    }

    public Optional<Colaborador> autenticar(String matricula, String senha) {
        if (matricula == null || senha == null) return Optional.empty();
        return colaboradorRepository.findByMatricula(matricula.trim())
                .filter(colaborador -> colaborador.getSenha() != null
                        && passwordEncoder.matches(senha, colaborador.getSenha()));
    }

    public String criarSessao(Colaborador colaborador) {
        return sessaoService.criar(colaborador);
    }

    public void solicitarRecuperacao(String email) {
        if (email == null || email.isBlank())
            throw new IllegalArgumentException("Informe o e-mail do colaborador.");
        emailRecuperacaoService.verificarConfiguracao();
        // Não diferencia e-mail existente de inexistente na resposta pública.
        Optional<Colaborador> registro = colaboradorRepository.findByEmail(email.trim().toLowerCase());
        if (registro.isEmpty()) return;

        Colaborador colaborador = registro.get();
        String codigo = String.format("%06d", random.nextInt(1_000_000));
        colaborador.setCodigoRecuperacao(passwordEncoder.encode(codigo));
        colaborador.setValidadeCodigoRecuperacao(Instant.now().plus(15, ChronoUnit.MINUTES));
        colaborador.setTentativasRecuperacao(0);
        colaboradorRepository.save(colaborador);
        try {
            emailRecuperacaoService.enviarCodigo(colaborador.getEmail(), codigo);
        } catch (RuntimeException e) {
            colaborador.setCodigoRecuperacao(null);
            colaborador.setValidadeCodigoRecuperacao(null);
            colaboradorRepository.save(colaborador);
            throw e;
        }
    }

    public void redefinirSenha(String email, String codigo, String novaSenha) {
        validarNovaSenha(novaSenha);
        if (email == null || codigo == null)
            throw new IllegalArgumentException("Código de recuperação inválido ou expirado.");
        Colaborador colaborador = colaboradorRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Código de recuperação inválido ou expirado."));
        if (colaborador.getCodigoRecuperacao() == null
                || colaborador.getValidadeCodigoRecuperacao() == null
                || Instant.now().isAfter(colaborador.getValidadeCodigoRecuperacao())
                || colaborador.getTentativasRecuperacao() >= 5) {
            throw new IllegalArgumentException("Código de recuperação inválido ou expirado.");
        }
        if (!passwordEncoder.matches(codigo, colaborador.getCodigoRecuperacao())) {
            colaborador.setTentativasRecuperacao(colaborador.getTentativasRecuperacao() + 1);
            colaboradorRepository.save(colaborador);
            throw new IllegalArgumentException("Código de recuperação inválido ou expirado.");
        }
        atualizarSenha(colaborador, novaSenha);
    }

    public void redefinirSenhaPeloAdministrador(String colaboradorId, String novaSenha) {
        validarNovaSenha(novaSenha);
        Colaborador colaborador = colaboradorRepository.findById(colaboradorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Colaborador não encontrado."));
        atualizarSenha(colaborador, novaSenha);
    }

    private void atualizarSenha(Colaborador colaborador, String novaSenha) {
        colaborador.setSenha(passwordEncoder.encode(novaSenha));
        colaborador.setCodigoRecuperacao(null);
        colaborador.setValidadeCodigoRecuperacao(null);
        colaborador.setTentativasRecuperacao(0);
        colaboradorRepository.save(colaborador);
        sessaoService.encerrarSessoesDoColaborador(colaborador.getId());
    }

    public static void validarNovaSenha(String senha) {
        if (senha == null || senha.length() < 8)
            throw new IllegalArgumentException("A senha deve ter pelo menos 8 caracteres.");
    }
}
