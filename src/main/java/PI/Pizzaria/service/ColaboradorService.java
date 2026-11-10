package PI.Pizzaria.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import PI.Pizzaria.exception.ColaboradorNotFoundException;
import PI.Pizzaria.model.Colaborador;
import PI.Pizzaria.repository.ColaboradorRepository;

@Service
public class ColaboradorService {
    private final ColaboradorRepository colaboradorRepository;
    private final SessaoService sessaoService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public ColaboradorService(ColaboradorRepository colaboradorRepository, SessaoService sessaoService) {
        this.colaboradorRepository = colaboradorRepository;
        this.sessaoService = sessaoService;
    }

    public Colaborador cadastrar(Colaborador colaborador) {
        validarDados(colaborador, true);
        if (colaboradorRepository.existsByMatricula(colaborador.getMatricula()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Matrícula já cadastrada.");
        if (colaboradorRepository.existsByEmail(colaborador.getEmail()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado.");

        colaborador.setId(null);
        colaborador.setSenha(passwordEncoder.encode(colaborador.getSenha()));
        colaborador.setCodigoRecuperacao(null);
        colaborador.setValidadeCodigoRecuperacao(null);
        colaborador.setTentativasRecuperacao(0);
        return colaboradorRepository.save(colaborador);
    }

    public List<Colaborador> listarTodos() { return colaboradorRepository.findAll(); }

    public Colaborador buscarPorId(String id) {
        return colaboradorRepository.findById(id)
                .orElseThrow(() -> new ColaboradorNotFoundException("Colaborador não encontrado."));
    }

    public Colaborador atualizar(String id, Colaborador dados) {
        Colaborador colaborador = buscarPorId(id);
        validarDados(dados, false);
        if (!dados.getMatricula().equals(colaborador.getMatricula())
                && colaboradorRepository.existsByMatricula(dados.getMatricula()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Matrícula já cadastrada.");
        if (!dados.getEmail().equals(colaborador.getEmail())
                && colaboradorRepository.existsByEmail(dados.getEmail()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado.");
        impedirRemocaoUltimoAdministrador(colaborador, dados.getPerfil());

        colaborador.setNome(dados.getNome().trim());
        colaborador.setMatricula(dados.getMatricula().trim());
        colaborador.setEmail(dados.getEmail().trim().toLowerCase());
        colaborador.setPerfil(dados.getPerfil());
        if (dados.getSenha() != null && !dados.getSenha().isBlank()) {
            AuthServicy.validarNovaSenha(dados.getSenha());
            colaborador.setSenha(passwordEncoder.encode(dados.getSenha()));
        }
        Colaborador atualizado = colaboradorRepository.save(colaborador);
        sessaoService.encerrarSessoesDoColaborador(id);
        return atualizado;
    }

    public void excluir(String id) {
        Colaborador colaborador = buscarPorId(id);
        impedirRemocaoUltimoAdministrador(colaborador, null);
        colaboradorRepository.delete(colaborador);
        sessaoService.encerrarSessoesDoColaborador(id);
    }

    private void validarDados(Colaborador colaborador, boolean exigirSenha) {
        if (colaborador.getNome() == null || colaborador.getNome().isBlank()
                || colaborador.getMatricula() == null || colaborador.getMatricula().isBlank()
                || colaborador.getEmail() == null
                || !colaborador.getEmail().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
                || colaborador.getPerfil() == null) {
            throw new IllegalArgumentException("Informe nome, matrícula, e-mail válido e perfil.");
        }
        colaborador.setNome(colaborador.getNome().trim());
        colaborador.setMatricula(colaborador.getMatricula().trim());
        colaborador.setEmail(colaborador.getEmail().trim().toLowerCase());
        if (exigirSenha) AuthServicy.validarNovaSenha(colaborador.getSenha());
    }

    private void impedirRemocaoUltimoAdministrador(Colaborador atual, Colaborador.Perfil novoPerfil) {
        if (atual.getPerfil() == Colaborador.Perfil.administrador
                && novoPerfil != Colaborador.Perfil.administrador
                && colaboradorRepository.countByPerfil(Colaborador.Perfil.administrador) <= 1) {
            throw new IllegalArgumentException("Não é possível remover o último administrador.");
        }
    }
}
