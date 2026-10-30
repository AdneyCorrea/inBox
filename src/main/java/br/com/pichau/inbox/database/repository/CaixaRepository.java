package br.com.pichau.inbox.database.repository;

import br.com.pichau.inbox.database.model.Caixa;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.util.ArrayList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/** Lê o catálogo de caixas e grava as alterações no arquivo local. */
@Repository
public class CaixaRepository {

    private List<Caixa> caixas;
    private final JsonMapper jsonMapper;
    private final Path destino;

    // As caixas têm seu próprio arquivo, separado do catálogo de produtos.
    public CaixaRepository(JsonMapper jsonMapper) {
        this(jsonMapper, "catalogo-local");
    }

    @Autowired
    public CaixaRepository(JsonMapper jsonMapper, @Value("${inbox.dados-diretorio:catalogo-local}") String diretorio) {
        this.jsonMapper = jsonMapper;
        this.destino = Path.of(diretorio, "caixas.json").toAbsolutePath();
        // O catálogo inicial é usado apenas quando ainda não existe um arquivo local.
        var arquivo = new ClassPathResource("catalogo-inicial/caixas.json");
        try (var entrada = Files.exists(destino) ? Files.newInputStream(destino) : arquivo.getInputStream()) {
            var tipo = jsonMapper.getTypeFactory().constructCollectionType(List.class, Caixa.class);
            caixas = List.copyOf(jsonMapper.<List<Caixa>>readValue(entrada, tipo));
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Não foi possível carregar o catálogo de caixas: " + destino, exception);
        }
    }

    public synchronized List<Caixa> buscarTodos() {
        return caixas;
    }

    public synchronized Optional<Caixa> buscarPorId(String id) {
        return caixas.stream().filter(caixa -> caixa.getId().equals(id)).findFirst();
    }

    public synchronized Caixa salvar(Caixa registro) {
        var atualizados = new ArrayList<>(caixas);
        atualizados.removeIf(atual -> atual.getId().equals(registro.getId()));
        atualizados.add(registro);
        gravar(atualizados);
        return registro;
    }

    public synchronized void remover(String id) {
        var atualizados = new ArrayList<>(caixas);
        atualizados.removeIf(atual -> atual.getId().equals(id));
        gravar(atualizados);
    }

    // Grava primeiro em arquivo temporário para evitar um JSON incompleto.
    private void gravar(List<Caixa> registros) {
        Path temporario = null;
        try {
            Files.createDirectories(destino.getParent());
            temporario = Files.createTempFile(destino.getParent(), "caixas-", ".tmp");
            Files.writeString(temporario, jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(registros));
            try {
                Files.move(temporario, destino, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporario, destino, StandardCopyOption.REPLACE_EXISTING);
            }
            caixas = List.copyOf(registros);
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Não foi possível salvar caixas.", exception);
        } finally {
            if (temporario != null) {
                try { Files.deleteIfExists(temporario); } catch (IOException ignored) { }
            }
        }
    }
}
