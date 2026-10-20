package PI.Pizzaria.service;

import java.util.List;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import PI.Pizzaria.exception.ColaboradorNotFoundException;
import PI.Pizzaria.model.Colaborador;
import PI.Pizzaria.repository.ColaboradorRepository;

@Service
public class ColaboradorService {

    private final ColaboradorRepository colaboradorRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public ColaboradorService(ColaboradorRepository colaboradorRepository) {
        this.colaboradorRepository = colaboradorRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    // Cadastrar colaborador
    public Colaborador cadastrar(Colaborador colaborador) {

        if (colaboradorRepository.existsByMatricula(colaborador.getMatricula())) {
            throw new IllegalArgumentException("Matrícula já cadastrada.");
        }

        if (colaboradorRepository.existsByEmail(colaborador.getEmail())) {
            throw new IllegalArgumentException("E-mail já cadastrado.");
        }

        colaborador.setSenha(passwordEncoder.encode(colaborador.getSenha()));

        return colaboradorRepository.save(colaborador);
    }

    // Listar todos os colaboradores
    public List<Colaborador> listarTodos() {
        return colaboradorRepository.findAll();
    }

    // Buscar colaborador pelo ID
    public Colaborador buscarPorId(String id) {
        return colaboradorRepository.findById(id)
                .orElseThrow(() ->
                    new ColaboradorNotFoundException("Colaborador não encontrado."));
    }

    // Atualizar colaborador    
public Colaborador atualizar(String id, Colaborador dados) {

    Colaborador colaborador = buscarPorId(id);

    if (dados.getMatricula() != null
            && !dados.getMatricula().equals(colaborador.getMatricula())
            && colaboradorRepository.existsByMatricula(dados.getMatricula())) {

        throw new IllegalArgumentException("Matrícula já cadastrada.");
    }

    if (dados.getEmail() != null
            && !dados.getEmail().equalsIgnoreCase(colaborador.getEmail())
            && colaboradorRepository.existsByEmail(dados.getEmail())) {

        throw new IllegalArgumentException("E-mail já cadastrado.");
    }

    colaborador.setNome(dados.getNome());
    colaborador.setMatricula(dados.getMatricula());
    colaborador.setEmail(dados.getEmail());
    colaborador.setPerfil(dados.getPerfil());

    // Só altera a senha se uma nova senha foi informada
    if (dados.getSenha() != null && !dados.getSenha().isBlank()) {

        colaborador.setSenha(
                passwordEncoder.encode(dados.getSenha())
        );
    }

    return colaboradorRepository.save(colaborador);
}

    // Excluir colaborador
    public void excluir(String id) {
        Colaborador colaborador = buscarPorId(id);
        colaboradorRepository.delete(colaborador);
    }

}
