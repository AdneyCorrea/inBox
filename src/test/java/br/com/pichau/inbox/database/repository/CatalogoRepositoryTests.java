package br.com.pichau.inbox.database.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;

/** Confere a leitura dos produtos, caixas e medidas dos arquivos JSON. */
class CatalogoRepositoryTests {
    @TempDir
    Path diretorio;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void carregaProdutosDoBackupSemConverterMilimetros() {
        var repository = new ProdutoRepository(jsonMapper, diretorio.toString());
        assertEquals(17, repository.buscarTodos().size());
        var produto = repository.buscarPorId("pichau-hf-pc").orElseThrow();
        assertEquals("Pichau Voyager One 3 Fans Black", produto.getNome());
        assertEquals("45076", produto.getSku());
        assertEquals(610, produto.getComprimento());
        assertEquals(360, produto.getLargura());
        assertEquals(610, produto.getAltura());
        assertTrue(repository.buscarPorId("inexistente").isEmpty());
    }

    @Test
    void carregaCaixasDoBackupComDimensoesInternas() {
        var repository = new CaixaRepository(jsonMapper, diretorio.toString());
        assertEquals(15, repository.buscarTodos().size());
        var caixa = repository.buscarPorId("415c2318-baef-4b76-ba47-a315ff11e388").orElseThrow();
        assertEquals("CX 101", caixa.getNome());
        assertEquals(370, caixa.getComprimento());
        assertEquals(595, caixa.getLargura());
        assertEquals(270, caixa.getAltura());
        assertTrue(repository.buscarPorId("inexistente").isEmpty());
    }
}
