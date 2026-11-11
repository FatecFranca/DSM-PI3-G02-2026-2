package PI.Pizzaria.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import PI.Pizzaria.model.Produto;

public interface ProdutoRepository extends MongoRepository<Produto, String> {

    List<Produto> findByNomeContainingIgnoreCase(String nome);

    List<Produto> findByCategoriaIgnoreCase(String categoria);

    List<Produto> findByNomeContainingIgnoreCaseOrCategoriaIgnoreCaseOrApresentacaoVolumeContainingIgnoreCase(
        String nome, String categoria, String apresentacaoVolume);
}