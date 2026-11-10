package PI.Pizzaria.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import PI.Pizzaria.model.Cliente;
import PI.Pizzaria.model.Endereco;
import PI.Pizzaria.repository.ClienteRepository;

class ClienteServiceTest {
    private Cliente novoCliente() {
        Cliente cliente = new Cliente();
        cliente.setNome("Maria");
        cliente.setTelefone("(11) 99999-8888");
        cliente.setCpf("529.982.247-25");
        Endereco endereco = new Endereco();
        endereco.setRua("Rua Principal");
        endereco.setNumero("123");
        endereco.setBairro("Centro");
        cliente.setEndereco(endereco);
        return cliente;
    }

    @Test
    void cadastrarNormalizaTelefoneCpfEVerificaDuplicidade() {
        ClienteRepository repository = mock(ClienteRepository.class);
        when(repository.save(any(Cliente.class))).thenAnswer(c -> c.getArgument(0));
        ClienteService service = new ClienteService(repository);
        Cliente cliente = service.cadastrar(novoCliente());
        assertEquals("11999998888", cliente.getTelefone());
        assertEquals("52998224725", cliente.getCpf());
        verify(repository).existsByCpf("52998224725");
    }

    @Test
    void pesquisaPorNomeNaoDeveBuscarTelefoneVazio() {
        ClienteRepository repository = mock(ClienteRepository.class);
        ClienteService service = new ClienteService(repository);
        service.buscar("Maria");
        verify(repository).findByNomeContainingIgnoreCase("Maria");
        verify(repository, never()).findByNomeContainingIgnoreCaseOrTelefoneContaining(anyString(), anyString());
    }

    @Test
    void naoPermiteCpfInvalido() {
        ClienteService service = new ClienteService(mock(ClienteRepository.class));
        Cliente cliente = novoCliente();
        cliente.setCpf("111.111.111-11");
        assertThrows(IllegalArgumentException.class, () -> service.cadastrar(cliente));
    }
}
