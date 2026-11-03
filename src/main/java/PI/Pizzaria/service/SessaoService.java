package PI.Pizzaria.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import PI.Pizzaria.model.Colaborador;
import PI.Pizzaria.repository.ColaboradorRepository;

/** Sessões de acesso temporárias; o perfil é sempre conferido no banco. */
@Service
public class SessaoService {
    private static final int DURACAO_HORAS = 8;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Sessao> sessoes = new ConcurrentHashMap<>();
    private final ColaboradorRepository colaboradorRepository;

    private record Sessao(String colaboradorId, Instant expiraEm) {}

    public SessaoService(ColaboradorRepository colaboradorRepository) {
        this.colaboradorRepository = colaboradorRepository;
    }

    public String criar(Colaborador colaborador) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessoes.put(token, new Sessao(colaborador.getId(), Instant.now().plus(DURACAO_HORAS, ChronoUnit.HOURS)));
        return token;
    }

    public Colaborador autenticar(String cabecalho) {
        if (cabecalho == null || !cabecalho.startsWith("Bearer ")) return null;
        String token = cabecalho.substring(7);
        Sessao sessao = sessoes.get(token);
        if (sessao == null) return null;
        if (!sessao.expiraEm().isAfter(Instant.now())) {
            sessoes.remove(token);
            return null;
        }
        // Não confia no perfil recebido do navegador; sempre consulta o banco.
        return colaboradorRepository.findById(sessao.colaboradorId()).orElse(null);
    }

    public void encerrar(String cabecalho) {
        if (cabecalho != null && cabecalho.startsWith("Bearer "))
            sessoes.remove(cabecalho.substring(7));
    }

    public void encerrarSessoesDoColaborador(String id) {
        sessoes.entrySet().removeIf(entrada -> entrada.getValue().colaboradorId().equals(id));
    }
}
