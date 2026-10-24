package br.com.inbox;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:concurrency;DB_CLOSE_DELAY=-1")
class StoreConcurrencyTest {
 @Autowired Store store;
 @Test void staleBrowserCannotOverwriteAnotherBrowser(){var first=store.read();var stale=store.read();first.dimensionProducts.clear();var p=new Model.DimensionProduct();p.name="Peça salva por outro acesso";first.dimensionProducts.add(p);store.save(first);long revision=stale.generation;assertThrows(ResponseStatusException.class,()->store.save(stale));assertEquals(revision,stale.generation);assertEquals(p.name,store.read().dimensionProducts.get(0).name);}
}
