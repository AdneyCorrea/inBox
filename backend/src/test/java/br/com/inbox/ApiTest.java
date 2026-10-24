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

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc
class ApiTest {
    @Autowired MockMvc mvc;
    @Autowired Store store;
    @BeforeEach void reset() {State clean=new State();clean.generation=store.read().generation;store.save(clean);}
    private long revision(){return store.read().generation;}
    private State seed() throws Exception {mvc.perform(post("/api/demo").header("X-State-Revision",revision()).contentType("application/json").content("{}")).andExpect(status().isOk());return store.read();}
    @Test void rejectsStaleMutation() throws Exception {long old=revision();seed();mvc.perform(post("/api/demo").header("X-State-Revision",old).contentType("application/json").content("{}")).andExpect(status().isConflict());}
    @Test void blocksDemoConclusionEvenWithPhysicalConfirmation() throws Exception {State s=seed();mvc.perform(post("/api/orders/"+s.orders.get(0).id+"/complete").header("X-State-Revision",revision()).contentType("application/json").content("{\"physicallyValidated\":true}")).andExpect(status().isBadRequest());}
    @Test void checksCodeAndRejectsOvercount() throws Exception {State s=seed();Order o=s.orders.get(0);Api.Expected e=Api.expected(s,o).get(0);String url="/api/orders/"+o.id+"/check";mvc.perform(post(url).header("X-State-Revision",revision()).contentType("application/json").content(store.json(new Api.Check(e.key(),"WRONG",false)))).andExpect(status().isBadRequest());mvc.perform(post(url).header("X-State-Revision",revision()).contentType("application/json").content(store.json(new Api.Check(e.key(),e.sku(),false)))).andExpect(status().isOk());mvc.perform(post(url).header("X-State-Revision",revision()).contentType("application/json").content(store.json(new Api.Check(e.key(),e.sku(),false)))).andExpect(status().isBadRequest());}
    @Test void referencedProductCannotBeDeleted() throws Exception {State s=seed();mvc.perform(delete("/api/products/"+s.products.get(0).id).header("X-State-Revision",revision())).andExpect(status().isBadRequest());}
    @Test void duplicateSkuCannotBeSaved() throws Exception {State s=seed();Product p=s.products.get(0);p.id=Model.id();mvc.perform(put("/api/products/"+p.id).header("X-State-Revision",revision()).contentType("application/json").content(store.json(p))).andExpect(status().isBadRequest());}
    @Test void editingProductClearsOpenChecks() throws Exception {State s=seed();Order o=s.orders.get(0);o.checked.put("test",1);store.save(s);Product p=s.products.get(0);mvc.perform(put("/api/products/"+p.id).header("X-State-Revision",revision()).contentType("application/json").content(store.json(p))).andExpect(status().isOk());assertTrue(store.read().orders.get(0).checked.isEmpty());}
    @Test void unknownProductAndStandaloneOriginRejected() throws Exception {State s=seed();Order o=s.orders.get(0);o.standalone.get(0).origin="BASE";mvc.perform(put("/api/orders/"+o.id).header("X-State-Revision",revision()).contentType("application/json").content(store.json(o))).andExpect(status().isBadRequest());}
    @Test void validWorkflowCompletesAndPreservesSnapshot() throws Exception {
        State s=seed();
        for(Product p:s.products) for(Pack pkg:p.packages) if(pkg.l!=null){pkg.status="VALIDATED";pkg.source="Medição de teste";pkg.measuredBy="Teste";pkg.measuredAt="2026-10-24";}
        for(Box b:s.boxes){b.status="VALIDATED";b.source="Teste";b.measuredBy="Teste";b.measuredAt="2026-10-24";}
        Order o=s.orders.get(0);for(Api.Expected e:Api.expected(s,o))o.checked.put(e.key(),e.quantity());store.save(s);
        mvc.perform(post("/api/orders/"+o.id+"/complete").header("X-State-Revision",revision()).contentType("application/json").content("{\"physicallyValidated\":true,\"note\":\"Teste\"}")).andExpect(status().isOk());
        Order done=store.read().orders.get(0);assertEquals("DONE",done.status);assertNotNull(done.completionSnapshot);
        s=store.read();s.boxes.clear();store.save(s);
        mvc.perform(get("/api/orders/"+o.id+"/plan")).andExpect(status().isOk()).andExpect(jsonPath("$.ready").value(true));
    }
    @Test void restoreResetsImportedCompletionAndChecks() throws Exception {State s=seed();Order o=s.orders.get(0);o.status="DONE";o.checked.put("fabricated",999);o.physicallyValidated=true;mvc.perform(post("/api/restore").header("X-State-Revision",revision()).contentType("application/json").content(store.json(s))).andExpect(status().isOk());Order restored=store.read().orders.get(0);assertEquals("REVIEW",restored.status);assertTrue(restored.checked.isEmpty());assertFalse(restored.physicallyValidated);}
    @Test void validationBlocksNegativeDimensions() throws Exception {State s=seed();Box b=s.boxes.get(0);b.l=-1;mvc.perform(put("/api/boxes/"+b.id).header("X-State-Revision",revision()).contentType("application/json").content(store.json(b))).andExpect(status().isBadRequest());}
    @Test void malformedBackupDoesNotReplaceDatabase() throws Exception {State s=seed();mvc.perform(post("/api/restore").header("X-State-Revision",revision()).contentType("application/json").content("{\"products\":null}")).andExpect(status().isBadRequest());assertEquals(s.products.size(),store.read().products.size());}
    @Test void cannotCompleteBeforeAllItemsChecked() throws Exception {
        State s=seed();Order o=s.orders.get(0);
        for(Product p:s.products)for(Pack pkg:p.packages)if(pkg.l!=null){pkg.status="VALIDATED";pkg.source="Teste";pkg.measuredBy="Teste";pkg.measuredAt="2026-10-24";}
        for(Box b:s.boxes){b.status="VALIDATED";b.source="Teste";b.measuredBy="Teste";b.measuredAt="2026-10-24";}store.save(s);
        mvc.perform(post("/api/orders/"+o.id+"/complete").header("X-State-Revision",revision()).contentType("application/json").content("{\"physicallyValidated\":true}")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Conclua a conferência de todos os itens."));
        assertNotEquals("DONE",store.read().orders.get(0).status);
    }
}
