package PI.Pizzaria.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import PI.Pizzaria.model.Colaborador;
import PI.Pizzaria.model.Produto;
import PI.Pizzaria.repository.ProdutoRepository;

@Service
public class ProdutoService {

    private final ProdutoRepository produtoRepository;

    public ProdutoService(ProdutoRepository produtoRepository) {
        this.produtoRepository = produtoRepository;
    }

    public Produto cadastrar(Produto produto, Colaborador usuarioAutenticado) {

        validarProduto(produto);

        produto.setId(null);

        produto.setCriadoPorId(usuarioAutenticado.getId());
        produto.setCriadoEm(Instant.now());

        produto.setAlteradoPorId(null);
        produto.setAlteradoEm(null);

        return produtoRepository.save(produto);
    }

    public List<Produto> listarTodos() {
        return produtoRepository.findAll();
    }

    public Produto buscarPorId(String id) {
        return produtoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado."));
    }

    public List<Produto> buscarPorNome(String nome) {
        return produtoRepository.findByNomeContainingIgnoreCase(nome);
    }

    public List<Produto> buscarPorCategoria(String categoria) {
        return produtoRepository.findByCategoriaIgnoreCase(categoria);
    }

    public Produto atualizar(
            String id,
            Produto dados,
            Colaborador usuarioAutenticado) {

        Produto produto = buscarPorId(id);

        validarProduto(dados);

        produto.setNome(dados.getNome());
        produto.setCategoria(dados.getCategoria());
        produto.setPreco(dados.getPreco());
        produto.setEstoque(dados.getEstoque());
        produto.setApresentacaoVolume(dados.getApresentacaoVolume());
        produto.setSabor(dados.getSabor());

        produto.setAlteradoPorId(usuarioAutenticado.getId());
        produto.setAlteradoEm(Instant.now());

        return produtoRepository.save(produto);
    }

    private void validarProduto(Produto produto) {

        if (produto.getNome() == null || produto.getNome().isBlank()) {
            throw new IllegalArgumentException("O nome do produto é obrigatório.");
        }

        if (produto.getCategoria() == null || produto.getCategoria().isBlank()) {
            throw new IllegalArgumentException("A categoria do produto é obrigatória.");
        }

        if (produto.getPreco() == null || produto.getPreco().signum() < 0) {
            throw new IllegalArgumentException("O preço do produto deve ser válido.");
        }

        if (produto.getEstoque() < 0) {
            throw new IllegalArgumentException("O estoque não pode ser negativo.");
        }
    }
}