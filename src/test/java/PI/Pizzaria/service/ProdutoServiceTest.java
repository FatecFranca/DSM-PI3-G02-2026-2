package PI.Pizzaria.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import PI.Pizzaria.model.Colaborador;
import PI.Pizzaria.model.Produto;
import PI.Pizzaria.model.ProdutoAuditoria;
import PI.Pizzaria.repository.ProdutoRepository;
import PI.Pizzaria.repository.ProdutoAuditoriaRepository;

class ProdutoServiceTest {
    @Test
    void bebidaExigeApresentacaoVolumeECadastroRegistraAutor() {
        ProdutoRepository produtos = mock(ProdutoRepository.class);
        ProdutoAuditoriaRepository auditoria = mock(ProdutoAuditoriaRepository.class);
        ProdutoService service = new ProdutoService(produtos, auditoria);
        Colaborador usuario = new Colaborador();
        usuario.setId("funcionario-1");
        Produto bebida = new Produto();
        bebida.setNome("Coca-Cola");
        bebida.setCategoria("BEBIDA");
        bebida.setPreco(new BigDecimal("8.50"));
        bebida.setEstoque(10);
        assertThrows(IllegalArgumentException.class,
                () -> service.cadastrar(bebida, usuario));
        bebida.setApresentacaoVolume("Lata 350 ml");
        when(produtos.save(any(Produto.class))).thenAnswer(x -> {
            Produto produto = x.getArgument(0);
            produto.setId("bebida-1");
            return produto;
        });
        Produto salvo = service.cadastrar(bebida, usuario);
        assertEquals("funcionario-1", salvo.getCriadoPorId());
        verify(auditoria).save(argThat(a -> "CRIACAO".equals(a.getAcao())
                && "funcionario-1".equals(a.getColaboradorId())));
    }

    @Test
    void excluirProdutoRegistraAcaoDoUsuario() {
        ProdutoRepository produtos = mock(ProdutoRepository.class);
        ProdutoAuditoriaRepository auditoria = mock(ProdutoAuditoriaRepository.class);
        ProdutoService service = new ProdutoService(produtos, auditoria);
        Produto produto = new Produto();
        produto.setId("produto-1");
        produto.setNome("Teste");
        when(produtos.findById("produto-1")).thenReturn(Optional.of(produto));
        Colaborador usuario = new Colaborador();
        usuario.setId("funcionario-1");
        service.excluir("produto-1", usuario);
        verify(auditoria).save(argThat(a -> "EXCLUSAO".equals(a.getAcao())
                && "produto-1".equals(a.getProdutoId())));
        verify(produtos).delete(produto);
    }
}
