package br.com.pichau.inbox;

import br.com.pichau.inbox.database.repository.CaixaRepository;
import br.com.pichau.inbox.database.repository.ProdutoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Testa cadastro, edição e remoção pelas APIs, usando arquivos temporários. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CadastroApiTests {
    @TempDir
    static Path diretorio;

    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry registry) {
        registry.add("inbox.dados-diretorio", () -> diretorio.toString());
    }

    @Value("${local.server.port}")
    private int port;
    private final JsonMapper mapper = JsonMapper.builder().build();

    private HttpResponse<String> request(String endpoint, String method, String body) throws Exception {
        return HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + endpoint))
                .header("Content-Type", "application/json")
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body))
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void cadastraEditaERemoveProdutosComPersistencia() throws Exception {
        var body = "{\"nome\":\"Produto novo\",\"sku\":\"TESTE\",\"categoria\":\"CPU\",\"comprimento\":100,\"largura\":80,\"altura\":50}";
        var criado = request("/api/produtos", "POST", body);
        assertEquals(201, criado.statusCode());
        var id = mapper.readTree(criado.body()).get("id").asString();
        var atualizado = request("/api/produtos/" + id, "PUT", body.replace("Produto novo", "Produto editado").replace("100", "120").replace("CPU", "GPU"));
        assertEquals(200, atualizado.statusCode());
        var recarregado = new ProdutoRepository(mapper, diretorio.toString()).buscarPorId(id).orElseThrow();
        assertEquals("Produto editado", recarregado.getNome());
        assertEquals(120, recarregado.getComprimento());
        assertEquals("GPU", recarregado.getCategoria());
        assertEquals(204, request("/api/produtos/" + id, "DELETE", null).statusCode());
        assertTrue(new ProdutoRepository(mapper, diretorio.toString()).buscarPorId(id).isEmpty());

        // Também permite editar e remover um registro do catálogo inicial.
        var original = "pichau-hf-power-cable";
        assertEquals(200, request("/api/produtos/" + original, "PUT", body).statusCode());
        assertEquals(204, request("/api/produtos/" + original, "DELETE", null).statusCode());
        assertTrue(new ProdutoRepository(mapper, diretorio.toString()).buscarPorId(original).isEmpty());
    }

    @Test
    void cadastraEditaERemoveCaixasComPersistencia() throws Exception {
        var body = "{\"nome\":\"Caixa nova\",\"comprimento\":500,\"largura\":400,\"altura\":300}";
        var criado = request("/api/caixas", "POST", body);
        assertEquals(201, criado.statusCode());
        var id = mapper.readTree(criado.body()).get("id").asString();
        assertEquals(200, request("/api/caixas/" + id, "PUT", body.replace("Caixa nova", "Caixa editada")).statusCode());
        assertEquals("Caixa editada", new CaixaRepository(mapper, diretorio.toString()).buscarPorId(id).orElseThrow().getNome());
        assertEquals(204, request("/api/caixas/" + id, "DELETE", null).statusCode());
        assertTrue(new CaixaRepository(mapper, diretorio.toString()).buscarPorId(id).isEmpty());
        var original = "415c2318-baef-4b76-ba47-a315ff11e388";
        assertEquals(200, request("/api/caixas/" + original, "PUT", body).statusCode());
        assertEquals(204, request("/api/caixas/" + original, "DELETE", null).statusCode());
        assertTrue(new CaixaRepository(mapper, diretorio.toString()).buscarPorId(original).isEmpty());
    }

    @Test
    void rejeitaDadosInvalidosERegistroInexistente() throws Exception {
        assertEquals(400, request("/api/caixas", "POST", "{\"nome\":\"Inválida\",\"comprimento\":0}").statusCode());
        assertEquals(400, request("/api/produtos", "POST", "{}").statusCode());
        assertEquals(404, request("/api/caixas/inexistente", "DELETE", null).statusCode());
        assertEquals(404, request("/api/produtos/inexistente", "PUT", "{}").statusCode());
    }
}
