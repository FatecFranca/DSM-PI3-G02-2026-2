package PI.Pizzaria.service;

import org.springframework.stereotype.Service;
import PI.Pizzaria.model.Cliente;
import PI.Pizzaria.repository.ClienteRepository;
import java.util.List;

@Service 
public class ClienteService {
    
    private final ClienteRepository clienteRepository;

    public ClienteService (ClienteRepository clienteRepository){
        this.clienteRepository = clienteRepository;
    }

    public Cliente cadastrar(Cliente cliente) {

        if (cliente.getNome() == null || cliente.getNome().isBlank()) {
            throw new IllegalArgumentException("Nome do cliente é obrigatório.");
        }

        if (cliente.getTelefone() == null || cliente.getTelefone().isBlank()) {
            throw new IllegalArgumentException("Telefone do cliente é obrigatório.");
        }

        if (cliente.getCpf() == null || cliente.getCpf().isBlank()) {
            throw new IllegalArgumentException("CPF do cliente é obrigatório.");
        }

        if (clienteRepository.existsByCpf(cliente.getCpf())) {
            throw new IllegalArgumentException("CPF já cadastrado.");
        }

        return clienteRepository.save(cliente);
    }   


    public List<Cliente> listarTodos() {
        return clienteRepository.findAll();
    }

    public List<Cliente> buscarPorNome(String nome) {
        return clienteRepository.findByNomeContainingIgnoreCase(nome);
    }

    public List<Cliente> buscarPorTelefone(String telefone) {
        return clienteRepository.findByTelefone(telefone);
    }

    public Cliente buscarPorId(String id){
        return clienteRepository.findById(id)
            .orElseThrow(() ->
                new IllegalArgumentException("Cliente não encontrado"));
    }

    public Cliente atualizar(String id, Cliente dados) {

        Cliente cliente = buscarPorId(id);

        if (dados.getNome() == null || dados.getNome().isBlank()) {
            throw new IllegalArgumentException("Nome do cliente é obrigatório.");
        }

        if (dados.getTelefone() == null || dados.getTelefone().isBlank()) {
            throw new IllegalArgumentException("Telefone do cliente é obrigatório.");
        }

        if (dados.getCpf() == null || dados.getCpf().isBlank()) {
            throw new IllegalArgumentException("CPF do cliente é obrigatório.");
        }

        if (!dados.getCpf().equals(cliente.getCpf())
                && clienteRepository.existsByCpf(dados.getCpf())) {

            throw new IllegalArgumentException("CPF já cadastrado.");
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
