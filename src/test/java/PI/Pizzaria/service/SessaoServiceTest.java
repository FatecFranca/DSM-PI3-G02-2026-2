package PI.Pizzaria.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import PI.Pizzaria.model.Colaborador;
import PI.Pizzaria.repository.ColaboradorRepository;

class SessaoServiceTest {
    @Test
    void tokenTemQueExistirNoServidorEPermiteLogout() {
        ColaboradorRepository repository = mock(ColaboradorRepository.class);
        Colaborador usuario = new Colaborador();
        usuario.setId("usuario-1");
        usuario.setMatricula("1001");
        usuario.setPerfil(Colaborador.Perfil.funcionario);
        when(repository.findById("usuario-1")).thenReturn(Optional.of(usuario));
        SessaoService service = new SessaoService(repository);

        String token = service.criar(usuario);
        assertNotNull(service.autenticar("Bearer " + token));
        assertNull(service.autenticar("Bearer token-invalido"));
        service.encerrar("Bearer " + token);
        assertNull(service.autenticar("Bearer " + token));
    }
}
