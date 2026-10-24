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

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:pctest;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc
class PcApiTest {
    @Autowired MockMvc mvc;
    @Autowired Store store;
    @Autowired PcApi api;
    @BeforeEach void setup(){State s=new State();s.generation=store.read().generation;PcExamples.add(s);store.save(s);}
    @Test void scansFullOrShortSkuCaseInsensitively() throws Exception {
        mvc.perform(get("/api/simple/pc/lookup").param("code"," pcm-pichau-highflyer-66705 ")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(PcExamples.HF));
        mvc.perform(get("/api/simple/pc/lookup").param("code","68475")).andExpect(jsonPath("$.code").value(PcExamples.GAMER));
    }
    @Test void unknownOrderNeverFallsBackToBase() throws Exception {mvc.perform(get("/api/simple/pc/lookup").param("code","PEDIDO-66705-DESCONHECIDO")).andExpect(status().isBadRequest());}
    @Test void baseAndBothPersonalizedPcsHaveValidBoxSuggestions(){State s=store.read();for(PcPreset pc:s.dimensionPcs){PcApi.PcPlan plan=api.calculate(s,pc);assertTrue(plan.issues.isEmpty(),pc.code);assertEquals("FOUND",plan.groups.get(0).result.status(),pc.code);assertTrue(plan.groups.get(0).result.occupancy()<=.85);assertTrue(plan.groups.get(0).result.demo());}}
    @Test void customizedOrderContainsReplacementAndThreeFans(){State s=store.read();PcPreset base=PcApi.resolve(s,"66705"),order=PcApi.resolve(s,"DEMO-PEDIDO-66705");assertTrue(base.items.stream().anyMatch(i->i.productId.equals("pichau-example-hf-gpu")));assertFalse(order.items.stream().anyMatch(i->i.productId.equals("pichau-example-hf-gpu")));assertTrue(order.items.stream().anyMatch(i->i.productId.equals("pichau-example-gpu-upgrade")));assertEquals(3,order.items.stream().filter(i->i.productId.equals("pichau-example-fan")).findFirst().orElseThrow().quantity);}
    @Test void nestedMemoryAndStorageDoNotAddExternalBoxes(){State s=store.read();PcApi.PcPlan plan=api.calculate(s,PcApi.resolve(s,"66705"));assertEquals(2,plan.notes.size());assertEquals(6,plan.groups.get(0).selection.size());}
    @Test void monitorIncludedIsMainButStandaloneIsFactory(){State s=store.read();PcApi.PcPlan included=api.calculate(s,PcApi.resolve(s,"DEMO-PEDIDO-66705"));assertTrue(included.groups.get(0).selection.stream().anyMatch(i->i.productId().equals("pichau-example-monitor")));PcApi.PcPlan extra=api.calculate(s,PcApi.resolve(s,"DEMO-PEDIDO-68475"));assertEquals(3,extra.groups.size());assertEquals(1,extra.groups.stream().filter(g->g.kind.equals("FACTORY")).findFirst().orElseThrow().volumes);assertEquals(2,extra.groups.stream().filter(g->g.kind.equals("EXTRA")).findFirst().orElseThrow().selection.size());}
    @Test void missingMotherboardBlocksSuggestion(){State s=store.read();PcPreset pc=PcApi.resolve(s,"66705");pc.items.removeIf(i->i.productId.equals("pichau-example-hf-mb"));PcApi.PcPlan plan=api.calculate(s,pc);assertFalse(plan.issues.isEmpty());assertEquals("PENDING",plan.groups.get(0).result.status());}
    @Test void missingPreparedPcBlocksSuggestion(){State s=store.read();PcPreset pc=PcApi.resolve(s,"66705");pc.items.removeIf(i->i.productId.equals("pichau-example-hf-pc"));assertEquals("PENDING",api.calculate(s,pc).groups.get(0).result.status());}
    @Test void oemWithoutBoxIsExcludedFromMain(){State s=store.read();PcPreset pc=PcApi.resolve(s,"68475");pc.items.stream().filter(i->i.productId.equals("pichau-example-gamer-cpu")).findFirst().orElseThrow().mode="OEM";assertTrue(api.calculate(s,pc).groups.get(0).selection.stream().noneMatch(i->i.productId().equals("pichau-example-gamer-cpu")));}
    @Test void seedIsIdempotentAndPreservesEditedMeasurements(){State s=store.read();s.dimensionProducts.get(0).l=777;int products=s.dimensionProducts.size();PcExamples.add(s);assertEquals(products,s.dimensionProducts.size());assertEquals(777,s.dimensionProducts.get(0).l);assertEquals(4,s.dimensionPcs.size());}
    @Test void fullFinalCompositionImportsWithoutMergingOriginalParts() throws Exception {
        State s=store.read();PcApi.ImportedOrder input=new PcApi.ImportedOrder();input.code="PEDIDO-REAL-FORMATO";input.configurationSku=PcExamples.HF;
        for(PcItem item:PcApi.resolve(s,"DEMO-PEDIDO-66705").items){PcApi.ImportedLine line=new PcApi.ImportedLine();line.sku=s.dimensionProducts.stream().filter(p->p.id.equals(item.productId)).findFirst().orElseThrow().sku;line.mode=item.mode;line.origin=item.origin;line.quantity=item.quantity;input.items.add(line);}
        mvc.perform(post("/api/simple/pc/import").header("X-State-Revision",s.generation).contentType("application/json").content(store.json(input))).andExpect(status().isOk());
        PcPreset imported=PcApi.resolve(store.read(),input.code);assertFalse(imported.items.stream().anyMatch(i->i.productId.equals("pichau-example-hf-gpu")));assertEquals("ORDER",imported.kind);
    }
    @Test void importRequiresFullFinalListAndDoesNotOverwriteOnFailure() throws Exception {State s=store.read();int before=s.dimensionPcs.size();mvc.perform(post("/api/simple/pc/import").header("X-State-Revision",s.generation).contentType("application/json").content("{\"code\":\"X\",\"configurationSku\":\"66705\"}")).andExpect(status().isBadRequest());assertEquals(before,store.read().dimensionPcs.size());}
    @Test void duplicateOrderCodeAndReferencedDeletionAreRejected() throws Exception {State s=store.read();PcPreset pc=PcApi.resolve(s,"DEMO-PEDIDO-66705");pc.id=Model.id();mvc.perform(put("/api/simple/pc/"+pc.id).header("X-State-Revision",s.generation).contentType("application/json").content(store.json(pc))).andExpect(status().isBadRequest());mvc.perform(delete("/api/simple/products/pichau-example-hf-pc").header("X-State-Revision",s.generation)).andExpect(status().isBadRequest());}
    @Test void oneBaseSkuCanHaveIndependentOrderCodes() throws Exception {State s=store.read();PcPreset pc=PcApi.resolve(s,"DEMO-PEDIDO-66705");pc.id=Model.id();pc.code="PEDIDO-OUTRO";pc.items.stream().filter(i->i.productId.equals("pichau-example-fan")).findFirst().orElseThrow().quantity=1;mvc.perform(put("/api/simple/pc/"+pc.id).header("X-State-Revision",s.generation).contentType("application/json").content(store.json(pc))).andExpect(status().isOk());assertEquals(3,PcApi.resolve(store.read(),"DEMO-PEDIDO-66705").items.stream().filter(i->i.productId.equals("pichau-example-fan")).findFirst().orElseThrow().quantity);}
    @Test void deletingCompositionKeepsTheProductCatalog() throws Exception {State s=store.read();PcPreset pc=PcApi.resolve(s,"DEMO-PEDIDO-66705");int products=s.dimensionProducts.size();mvc.perform(delete("/api/simple/pc/"+pc.id).header("X-State-Revision",s.generation)).andExpect(status().isOk());assertEquals(products,store.read().dimensionProducts.size());assertThrows(IllegalArgumentException.class,()->PcApi.resolve(store.read(),pc.code));}
}
