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
        validar(cliente);
        if (cliente.getCpf() != null && clienteRepository.existsByCpf(cliente.getCpf()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CPF já cadastrado.");
        cliente.setId(null);
        return clienteRepository.save(cliente);
    }

    public List<Cliente> buscar(String termo) {
        if (termo == null || termo.isBlank()) return clienteRepository.findAll();
        String pesquisa = termo.trim();
        String digitos = pesquisa.replaceAll("\\D", "");
        if (digitos.isEmpty()) return clienteRepository.findByNomeContainingIgnoreCase(pesquisa);
        return clienteRepository.findByNomeContainingIgnoreCaseOrTelefoneContaining(pesquisa, digitos);
    }

    public List<Cliente> listarTodos() { return clienteRepository.findAll(); }

    public Cliente buscarPorId(String id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Cliente não encontrado."));
    }

    public Cliente atualizar(String id, Cliente dados) {
        Cliente atual = buscarPorId(id);
        validar(dados);
        if (dados.getCpf() != null && !dados.getCpf().equals(atual.getCpf())
                && clienteRepository.existsByCpf(dados.getCpf()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CPF já cadastrado.");
        atual.setNome(dados.getNome());
        atual.setTelefone(dados.getTelefone());
        atual.setCpf(dados.getCpf());
        atual.setEmail(dados.getEmail());
        atual.setDataNascimento(dados.getDataNascimento());
        atual.setEndereco(dados.getEndereco());
        return clienteRepository.save(atual);
    }

    private void validar(Cliente cliente) {
        if (cliente.getNome() == null || cliente.getNome().isBlank())
            throw new IllegalArgumentException("O nome do cliente é obrigatório.");
        cliente.setNome(cliente.getNome().trim());
        if (cliente.getTelefone() == null || !cliente.getTelefone().replaceAll("\\D", "").matches("\\d{10,11}"))
            throw new IllegalArgumentException("Informe um telefone com DDD válido.");
        cliente.setTelefone(cliente.getTelefone().replaceAll("\\D", ""));
        if (cliente.getCpf() != null && !cliente.getCpf().isBlank()) {
            String cpf = cliente.getCpf().replaceAll("\\D", "");
            if (!cpfValido(cpf)) throw new IllegalArgumentException("CPF inválido.");
            cliente.setCpf(cpf);
        } else {
            cliente.setCpf(null);
        }
        if (cliente.getEmail() != null && !cliente.getEmail().isBlank()) {
            if (!cliente.getEmail().trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
                throw new IllegalArgumentException("E-mail do cliente inválido.");
            cliente.setEmail(cliente.getEmail().trim().toLowerCase());
        }
        Endereco endereco = cliente.getEndereco();
        if (endereco == null || endereco.getRua() == null || endereco.getRua().isBlank()
                || endereco.getNumero() == null || endereco.getNumero().isBlank()
                || endereco.getBairro() == null || endereco.getBairro().isBlank())
            throw new IllegalArgumentException("Rua, número e bairro são obrigatórios.");
    }

    private boolean cpfValido(String cpf) {
        if (!cpf.matches("\\d{11}") || cpf.chars().distinct().count() == 1) return false;
        for (int n = 9; n <= 10; n++) {
            int soma = 0;
            for (int i = 0; i < n; i++) soma += Character.digit(cpf.charAt(i), 10) * (n + 1 - i);
            int digito = (soma * 10) % 11;
            if (digito == 10) digito = 0;
            if (digito != Character.digit(cpf.charAt(n), 10)) return false;
        }
        return true;
    }
}
