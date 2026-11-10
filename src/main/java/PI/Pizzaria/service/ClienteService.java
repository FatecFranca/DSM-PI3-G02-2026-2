
package PI.Pizzaria.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import PI.Pizzaria.model.Cliente;
import PI.Pizzaria.model.Endereco;
import PI.Pizzaria.repository.ClienteRepository;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    public Cliente cadastrar(Cliente cliente) {

        if (cliente.getNome() == null
                || cliente.getNome().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O nome do cliente é obrigatório.");
        }

        if (cliente.getTelefone() == null
                || cliente.getTelefone().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O telefone do cliente é obrigatório.");
        }

        Endereco endereco = cliente.getEndereco();

        if (endereco == null
                || endereco.getRua() == null
                || endereco.getRua().isBlank()
                || endereco.getNumero() == null
                || endereco.getNumero().isBlank()
                || endereco.getBairro() == null
                || endereco.getBairro().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Rua, número e bairro são obrigatórios.");
        }

        String cpf = cliente.getCpf();

        if (cpf != null && !cpf.isBlank()
                && clienteRepository.existsByCpf(cpf)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "CPF já cadastrado.");
        }

        // Garante que o MongoDB gere um novo ID.
        cliente.setId(null);

        return clienteRepository.save(cliente);
    }

    public List<Cliente> buscar(String termo) {

        if (termo == null || termo.isBlank()) {
            return clienteRepository.findAll();
        }

        String pesquisa = termo.trim();

        return clienteRepository
                .findByNomeContainingIgnoreCaseOrTelefoneContaining(
                        pesquisa, pesquisa);
    }

    public List<Cliente> listarTodos() {
        return clienteRepository.findAll();
    }

    public List<Cliente> buscarPorNome(String nome) {
        return clienteRepository
                .findByNomeContainingIgnoreCase(nome);
    }

    public List<Cliente> buscarPorTelefone(String telefone) {
        return clienteRepository
                .findByTelefoneContaining(telefone);
    }

    public Cliente buscarPorId(String id) {
        return clienteRepository.findById(id)
                .orElseThrow(() ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Cliente não encontrado."));
    }

    public Cliente atualizar(String id, Cliente dados) {

        Cliente cliente = buscarPorId(id);

        if (dados.getNome() == null
                || dados.getNome().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O nome do cliente é obrigatório.");
        }

        if (dados.getTelefone() == null
                || dados.getTelefone().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O telefone do cliente é obrigatório.");
        }

        String novoCpf = dados.getCpf();

        if (novoCpf != null && !novoCpf.isBlank()
                && !novoCpf.equals(cliente.getCpf())
                && clienteRepository.existsByCpf(novoCpf)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "CPF já cadastrado.");
        }

        cliente.setNome(dados.getNome());
        cliente.setTelefone(dados.getTelefone());
        cliente.setEmail(dados.getEmail());
        cliente.setEndereco(dados.getEndereco());
        cliente.setCpf(dados.getCpf());
        cliente.setDataNascimento(dados.getDataNascimento());

        return clienteRepository.save(cliente);
    }
}
