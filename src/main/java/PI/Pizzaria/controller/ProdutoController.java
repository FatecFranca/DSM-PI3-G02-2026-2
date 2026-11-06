package PI.Pizzaria.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import PI.Pizzaria.model.Colaborador;
import PI.Pizzaria.model.Produto;
import PI.Pizzaria.service.ProdutoService;

@RestController
@RequestMapping("/produtos")
public class ProdutoController {

    private final ProdutoService produtoService;

    public ProdutoController(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    @PostMapping
    public ResponseEntity<Produto> cadastrar(
            @RequestBody Produto produto,
            @RequestAttribute("usuarioAutenticado") Colaborador usuarioAutenticado) {

        Produto novoProduto = produtoService.cadastrar(produto, usuarioAutenticado);

        return ResponseEntity.status(HttpStatus.CREATED).body(novoProduto);
    }

    @GetMapping
    public ResponseEntity<List<Produto>> listarTodos() {
        return ResponseEntity.ok(produtoService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Produto> buscarPorId(@PathVariable String id) {
        return ResponseEntity.ok(produtoService.buscarPorId(id));
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<Produto>> buscarPorNome(
            @RequestParam String nome) {

        return ResponseEntity.ok(produtoService.buscarPorNome(nome));
    }

    @GetMapping("/categoria/{categoria}")
    public ResponseEntity<List<Produto>> buscarPorCategoria(
            @PathVariable String categoria) {

        return ResponseEntity.ok(
                produtoService.buscarPorCategoria(categoria));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Produto> atualizar(
            @PathVariable String id,
            @RequestBody Produto produto,
            @RequestAttribute("usuarioAutenticado") Colaborador usuarioAutenticado) {

        Produto atualizado =
                produtoService.atualizar(id, produto, usuarioAutenticado);

        return ResponseEntity.ok(atualizado);
    }
}