package br.com.pichau.inbox;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Value;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

/** Confere as APIs de consulta e o cálculo da caixa com o catálogo real. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CatalogoApiTests {
    @TempDir
    static Path diretorio;

    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry registry) {
        registry.add("inbox.dados-diretorio", () -> diretorio.toString());
    }

    @Value("${local.server.port}")
    private int port;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private HttpResponse<String> get(String caminho) throws Exception {
        var client = HttpClient.newHttpClient();
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + caminho)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void calculaCaixaPeloFluxoCompleto() throws Exception {
        var client = HttpClient.newHttpClient();
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/embalagens/calcular"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"itens\":[{\"produtoId\":\"pichau-hf-power-cable\",\"quantidade\":1}]}"))
                .build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        var resultado = jsonMapper.readTree(response.body());
        assertEquals("CX 26", resultado.get("caixa").get("nome").asString());
        assertTrue(resultado.get("percentualLivre").asDouble() >= 15);
    }

    @Test
    void listaProdutosDoJsonPelaApi() throws Exception {
        var response = get("/api/produtos");
        assertEquals(200, response.statusCode());
        var produtos = jsonMapper.readTree(response.body());
        assertEquals(17, produtos.size());
        boolean encontrou = false;
        for (var produto : produtos) {
            if (produto.get("id").asString().equals("pichau-hf-pc")) {
                encontrou = true;
                assertEquals("Pichau Voyager One 3 Fans Black", produto.get("nome").asString());
                assertEquals("45076", produto.get("sku").asString());
                assertEquals("CASE", produto.get("categoria").asString());
                assertEquals(610, produto.get("comprimento").asInt());
                assertEquals(360, produto.get("largura").asInt());
                assertEquals(610, produto.get("altura").asInt());
            }
        }
        assertTrue(encontrou);
    }

    @Test
    void listaCaixasDoJsonPelaApi() throws Exception {
        var response = get("/api/caixas");
        assertEquals(200, response.statusCode());
        var caixas = jsonMapper.readTree(response.body());
        assertEquals(15, caixas.size());
        boolean encontrou = false;
        for (var caixa : caixas) {
            if (caixa.get("nome").asString().equals("CX MANCER M")) {
                encontrou = true;
                assertEquals(420, caixa.get("comprimento").asInt());
                assertEquals(560, caixa.get("largura").asInt());
                assertEquals(720, caixa.get("altura").asInt());
            }
        }
        assertTrue(encontrou);
    }
}
