// Carrega e indexa as categorias do catálogo e normaliza categorias de produtos.
package br.com.inbox;

import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import static br.com.inbox.Validation.require;

/** Catalogo compartilhado com a interface. Os indices sao montados uma unica vez. */
public final class CatalogTopics {
    public record Topic(String id, String label, String category) {
    }
    public record Department(String id, String label, List<Topic> topics) {
    }
    public static final List<Department> DEPARTMENTS;
    private static final Map<String, Topic> TOPICS_BY_ID;
    private static final Map<String, String> FIRST_TOPIC_BY_CATEGORY;
    static {
        try (var input = CatalogTopics.class.getResourceAsStream("/static/catalog-categories.json")) {
            DEPARTMENTS = List.of(new ObjectMapper().readValue(input, Department[].class));
            Map<String, Topic> topics = new LinkedHashMap<>();
            Map<String, String> defaults = new HashMap<>();
            for (Department department : DEPARTMENTS) {
                for (Topic topic : department.topics()) {
                    if (topics.putIfAbsent(topic.id(), topic) != null) {
                        throw new IllegalStateException("Categoria duplicada: " + topic.id());
                    }
                    defaults.putIfAbsent(topic.category(), topic.id());
                }
            }
            TOPICS_BY_ID = Map.copyOf(topics);
            FIRST_TOPIC_BY_CATEGORY = Map.copyOf(defaults);
        }
        catch (Exception error) {
            throw new ExceptionInInitializerError(error);
        }
    }
    private CatalogTopics() {
    }
    /** Completa categorias de backups antigos sem mudar os identificadores dos produtos. */
    public static void normalize(Model.DimensionProduct product) {
        require(product.topic != null, "Categoria ausente.");
        if (product.topic.isBlank()) {
            product.topic = switch (product.category) {
                case "OTHER" -> "other/other";
                case "CABLE" -> "peripherals/cables";
                default -> FIRST_TOPIC_BY_CATEGORY.getOrDefault(product.category, "other/other");
            };
        }
        Topic topic = TOPICS_BY_ID.get(product.topic);
        if (topic == null) throw new IllegalArgumentException("Categoria de catalogo invalida.");
        product.category = topic.category();
    }
}
