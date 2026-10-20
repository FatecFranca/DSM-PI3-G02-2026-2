package PI.Pizzaria.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import PI.Pizzaria.repository.ColaboradorRepository;
import PI.Pizzaria.model.Colaborador;

import java.security.SecureRandom;
import java.util.Optional;

@Service
public class AuthServicy {
    private final ColaboradorRepository colaboradorRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthServicy(ColaboradorRepository colaboradorRepository) {
        this.colaboradorRepository = colaboradorRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public Optional<Colaborador> autenticar(String matricula, String senha) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        return colaboradorRepository.findByMatricula(matricula)
                .filter(colaborador -> encoder.matches(senha, colaborador.getSenha()));
    }

    public boolean isAdmnistrador(Colaborador colaborador) {
        return colaborador.getPerfil() == Colaborador.Perfil.administrador;
    }

    public String gerarCodigoRecuperacao(String email) {
        Colaborador colaborador = colaboradorRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("E-mail não encontrado."));
        SecureRandom random = new SecureRandom();
        String codigo = String.format("%06d", random.nextInt(1000000));
        colaborador.setCodigoRecuperacao(codigo);
        colaboradorRepository.save(colaborador);
        return codigo;
    }

    public void redefinirSenha(String email, String codigo, String novaSenha) {
        Colaborador colaborador = colaboradorRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("E-mail não encontrado."));
        if (colaborador.getCodigoRecuperacao() == null || !colaborador.getCodigoRecuperacao().equals(codigo)) {
            throw new IllegalArgumentException("Código de recuperação inválido.");
        }
        colaborador.setSenha(passwordEncoder.encode(novaSenha));
        colaborador.setCodigoRecuperacao(null);
        colaboradorRepository.save(colaborador);
    }
}
