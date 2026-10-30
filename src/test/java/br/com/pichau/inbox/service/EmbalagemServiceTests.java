package br.com.pichau.inbox.service;

import br.com.pichau.inbox.database.model.Caixa;
import br.com.pichau.inbox.database.model.Produto;
import br.com.pichau.inbox.database.repository.CaixaRepository;
import br.com.pichau.inbox.database.repository.ProdutoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;

import java.util.List;
import java.util.Optional;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Testa encaixe, rotação, empilhamento e reserva para bolha. */
class EmbalagemServiceTests {
    @TempDir
    Path diretorio;
    private final ProdutoRepository produtos = mock(ProdutoRepository.class);
    private final CaixaRepository caixas = mock(CaixaRepository.class);
    private final EmbalagemService service = new EmbalagemService(produtos, caixas);

    private void produto(int c, int l, int a) {
        var produto = new Produto();
        produto.setComprimento(c); produto.setLargura(l); produto.setAltura(a);
        when(produtos.buscarPorId("produto")).thenReturn(Optional.of(produto));
    }

    private Caixa caixa(String id, int c, int l, int a) {
        var caixa = new Caixa();
        caixa.setId(id); caixa.setNome(id);
        caixa.setComprimento(c); caixa.setLargura(l); caixa.setAltura(a);
        return caixa;
    }

    @Test
    void escolheMenorCaixaCompativelMesmoQuandoPrecisaRotacionar() {
        produto(20, 10, 5);
        when(caixas.buscarTodos()).thenReturn(List.of(caixa("grande", 40, 40, 40), caixa("menor", 10, 5, 25)));
        var resultado = service.calcular("produto", 1);
        assertEquals("menor", resultado.caixa().getId());
        assertEquals(10, resultado.posicoes().get(0).comprimento());
        assertEquals(5, resultado.posicoes().get(0).largura());
        assertEquals(20, resultado.posicoes().get(0).altura());
        assertEquals(20, resultado.percentualLivre(), 0.0001);
    }

    @Test
    void aceitaExatamenteQuinzePorCentoLivreMasRejeitaReservaInsuficiente() {
        produto(17, 10, 10);
        when(caixas.buscarTodos()).thenReturn(List.of(caixa("sem-reserva", 18, 10, 10), caixa("limite", 20, 10, 10)));
        var resultado = service.calcular("produto", 1);
        assertEquals("limite", resultado.caixa().getId());
        assertEquals(15, resultado.percentualLivre(), 0.0001);
    }

    @Test
    void volumeSuficienteNaoSubstituiEncaixeFisico() {
        produto(50, 2, 2);
        when(caixas.buscarTodos()).thenReturn(List.of(caixa("curta", 20, 20, 20)));
        assertNull(service.calcular("produto", 1).caixa());
    }

    @Test
    void empilhaUnidadesSemSobreposicaoEComReserva() {
        produto(10, 10, 10);
        when(caixas.buscarTodos()).thenReturn(List.of(caixa("pilha", 10, 10, 24)));
        var resultado = service.calcular("produto", 2);
        assertEquals(2, resultado.posicoes().size());
        assertEquals(0, resultado.posicoes().get(0).z());
        assertEquals(10, resultado.posicoes().get(1).z());
        assertTrue(resultado.percentualLivre() >= 15);
        validarGeometria(resultado);
    }

    @Test
    void combinaProdutosDiferentesERespeitaLimitesDeQuantidade() {
        produto(10, 10, 10);
        var outro = new Produto();
        outro.setComprimento(12); outro.setLargura(8); outro.setAltura(6);
        when(produtos.buscarPorId("outro")).thenReturn(Optional.of(outro));
        when(caixas.buscarTodos()).thenReturn(List.of(caixa("conjunto", 35, 25, 20)));
        var resultado = service.calcular(List.of(new EmbalagemService.ItemPedido("produto", 3),
                new EmbalagemService.ItemPedido("outro", 2)));
        assertNotNull(resultado.caixa());
        assertEquals(5, resultado.posicoes().size());
        assertEquals(2, resultado.posicoes().stream().filter(p -> p.produtoId().equals("outro")).count());
        validarGeometria(resultado);
        assertThrows(IllegalArgumentException.class, () -> service.calcular(List.of()));
        assertThrows(IllegalArgumentException.class, () -> service.calcular("produto", 201));
    }

    private void validarGeometria(EmbalagemService.Resultado resultado) {
        var caixa = resultado.caixa();
        var posicoes = resultado.posicoes();
        for (int i = 0; i < posicoes.size(); i++) {
            var a = posicoes.get(i);
            assertTrue(a.x() >= 0 && a.y() >= 0 && a.z() >= 0);
            assertTrue(a.x() + a.comprimento() <= caixa.getComprimento());
            assertTrue(a.y() + a.largura() <= caixa.getLargura());
            assertTrue(a.z() + a.altura() <= caixa.getAltura());
            if (a.z() > 0) assertTrue(posicoes.stream().anyMatch(base ->
                    base.z() + base.altura() == a.z() && base.x() <= a.x() && base.y() <= a.y()
                    && base.x() + base.comprimento() >= a.x() + a.comprimento()
                    && base.y() + base.largura() >= a.y() + a.largura()));
            for (int j = i + 1; j < posicoes.size(); j++) {
                var b = posicoes.get(j);
                assertTrue(a.x() + a.comprimento() <= b.x() || b.x() + b.comprimento() <= a.x()
                        || a.y() + a.largura() <= b.y() || b.y() + b.largura() <= a.y()
                        || a.z() + a.altura() <= b.z() || b.z() + b.altura() <= a.z());
            }
        }
    }

    @Test
    void acomodaComposicaoHighflyerComDadosReaisDoBackup() {
        var mapper = tools.jackson.databind.json.JsonMapper.builder().build();
        var real = new EmbalagemService(new ProdutoRepository(mapper, diretorio.toString()), new CaixaRepository(mapper, diretorio.toString()));
        var itens = List.of("pc", "cpu", "mb", "gpu", "ram", "ssd", "psu", "cooler", "power-cable", "hdmi")
                .stream().map(sufixo -> new EmbalagemService.ItemPedido("pichau-hf-" + sufixo, 1)).toList();
        var resultado = real.calcular(itens);
        assertNotNull(resultado.caixa());
        assertEquals(10, resultado.posicoes().size());
        assertTrue(resultado.percentualLivre() >= 15);
        validarGeometria(resultado);
    }

    @Test
    void rejeitaQuantidadeInvalidaEProdutoInexistente() {
        assertThrows(IllegalArgumentException.class, () -> service.calcular("produto", 0));
        assertThrows(IllegalArgumentException.class, () -> service.calcular(null, 1));
        when(produtos.buscarPorId("inexistente")).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () -> service.calcular("inexistente", 1));
    }
}
