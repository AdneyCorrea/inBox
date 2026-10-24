package br.com.inbox;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static br.com.inbox.Model.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:simpletest;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc
class DimensionsApiTest {
    @Autowired MockMvc mvc;
    @Autowired Store store;
    @BeforeEach void reset(){State clean=new State();clean.generation=store.read().generation;store.save(clean);}
    private long generation(){return store.read().generation;}
    private State measured(int length) {
        State s=new State();s.generation=store.read().generation;DimensionProduct p=new DimensionProduct();p.id="product";p.name="Produto";p.l=length;p.w=100;p.h=100;s.dimensionProducts.add(p);
        DimensionBox b=new DimensionBox();b.id="box";b.name="Caixa";b.l=100;b.w=100;b.h=100;s.dimensionBoxes.add(b);store.save(s);return s;
    }
    @Test void rootUsesSimplePage() throws Exception {mvc.perform(get("/")).andExpect(status().isOk()).andExpect(forwardedUrl("/simple.html"));}
    @Test void catalogDoesNotExposeOldWorkflow() throws Exception {State s=Demo.create();s.generation=store.read().generation;store.save(s);mvc.perform(get("/api/simple/state")).andExpect(status().isOk()).andExpect(jsonPath("$.products.length()").value(0)).andExpect(jsonPath("$.orders").doesNotExist());}
    @Test void canSaveProductWithoutMeasures() throws Exception {DimensionProduct p=new DimensionProduct();p.name="Gabinete";mvc.perform(put("/api/simple/products/"+p.id).header("X-State-Revision",generation()).contentType("application/json").content(store.json(p))).andExpect(status().isOk()).andExpect(jsonPath("$.products[0].l").isEmpty());}
    @Test void exactReserveFindsBox() throws Exception {measured(85);mvc.perform(post("/api/simple/calculate").contentType("application/json").content("[{\"productId\":\"product\",\"quantity\":1}]")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("FOUND")).andExpect(jsonPath("$.occupancy").value(.85));}
    @Test void excessOccupancyRejectsBox() throws Exception {measured(86);mvc.perform(post("/api/simple/calculate").contentType("application/json").content("[{\"productId\":\"product\",\"quantity\":1}]")).andExpect(jsonPath("$.status").value("NOT_FOUND"));}
    @Test void pendingMeasurementsReturnAnExplanation() throws Exception {State s=measured(50);s.dimensionProducts.get(0).l=null;store.save(s);mvc.perform(post("/api/simple/calculate").contentType("application/json").content("[{\"productId\":\"product\",\"quantity\":1}]")).andExpect(jsonPath("$.status").value("PENDING"));}
    @Test void pendingBoxesAreNotUsed() throws Exception {State s=measured(50);s.dimensionBoxes.get(0).l=null;store.save(s);mvc.perform(post("/api/simple/calculate").contentType("application/json").content("[{\"productId\":\"product\",\"quantity\":1}]")).andExpect(jsonPath("$.status").value("PENDING"));}
    @Test void impossibleGeometryIsRejectedEvenWhenVolumeIsSmall() throws Exception {State s=measured(120);s.dimensionProducts.get(0).w=10;s.dimensionProducts.get(0).h=10;store.save(s);mvc.perform(post("/api/simple/calculate").contentType("application/json").content("[{\"productId\":\"product\",\"quantity\":1}]")).andExpect(jsonPath("$.status").value("NOT_FOUND"));}
    @Test void rejectsUnknownProductOrInvalidQuantity() throws Exception {measured(50);mvc.perform(post("/api/simple/calculate").contentType("application/json").content("[{\"productId\":\"product\",\"quantity\":0}]")).andExpect(status().isBadRequest());mvc.perform(post("/api/simple/calculate").contentType("application/json").content("[{\"productId\":\"unknown\",\"quantity\":1}]")).andExpect(status().isBadRequest());}
    @Test void staleWriteDoesNotOverwriteCatalog() throws Exception {State s=measured(50);DimensionProduct p=s.dimensionProducts.get(0);mvc.perform(put("/api/simple/products/"+p.id).header("X-State-Revision",0).contentType("application/json").content(store.json(p))).andExpect(status().isConflict());}
    @Test void backupRoundtripPreservesSimpleCatalog() throws Exception {State s=measured(50);mvc.perform(post("/api/restore").header("X-State-Revision",generation()).contentType("application/json").content(store.json(s))).andExpect(status().isOk());assertEquals(1,store.read().dimensionProducts.size());assertEquals(50,store.read().dimensionProducts.get(0).l);}
    @Test void examplesAreClearlyMarked() throws Exception {mvc.perform(post("/api/simple/examples").header("X-State-Revision",generation()).contentType("application/json").content("{}")).andExpect(status().isOk()).andExpect(jsonPath("$.products[0].demo").value(true));}
}
