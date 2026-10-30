package br.com.pichau.inbox.service;

import br.com.pichau.inbox.database.model.Produto;
import br.com.pichau.inbox.database.repository.ProdutoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.NoSuchElementException;

/** Valida os produtos antes de consultar, salvar ou remover pelo repository. */
@Service
public class ProdutoService {

    private final ProdutoRepository repository;

    public ProdutoService(ProdutoRepository repository) {
        this.repository = repository;
    }

    public List<Produto> buscarTodos() {
        return repository.buscarTodos();
    }

    public Produto cadastrar(Produto produto) {
        validar(produto);
        produto.setId(UUID.randomUUID().toString());
        return repository.salvar(produto);
    }

    public Produto atualizar(String id, Produto produto) {
        exigirExistente(id);
        validar(produto);
        produto.setId(id);
        return repository.salvar(produto);
    }

    public void remover(String id) {
        exigirExistente(id);
        repository.remover(id);
    }

    private void exigirExistente(String id) {
        if (repository.buscarPorId(id).isEmpty()) throw new NoSuchElementException("Cadastro não encontrado.");
    }

    private void validar(Produto produto) {
        if (produto == null || produto.getNome() == null || produto.getNome().isBlank()
                || produto.getNome().length() > 120 || produto.getComprimento() < 1
                || produto.getLargura() < 1 || produto.getAltura() < 1
                || produto.getComprimento() > 100000 || produto.getLargura() > 100000
                || produto.getAltura() > 100000) {
            throw new IllegalArgumentException("Informe nome e medidas entre 0,1 e 10.000 cm.");
        }
        produto.setNome(produto.getNome().trim());
        if (produto.getCategoria() == null || !List.of("CABLE", "CASE", "PREPARED_PC", "CPU", "MOTHERBOARD", "MEMORY", "GPU", "SSD", "PSU", "COOLER", "FAN", "UNCATEGORIZED").contains(produto.getCategoria()))
            throw new IllegalArgumentException("Selecione uma categoria válida.");
        if (produto.getSku() != null && produto.getSku().length() > 80)
            throw new IllegalArgumentException("O SKU deve ter no máximo 80 caracteres.");
    }
}
