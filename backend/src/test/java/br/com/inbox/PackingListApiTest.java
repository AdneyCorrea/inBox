package br.com.inbox;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;
import static br.com.inbox.Model.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:packinglist;DB_CLOSE_DELAY=-1"})
class PackingListApiTest {
 @Autowired Store store;
 @Autowired PackingListApi api;
 @BeforeEach void setup(){State s=new State();s.generation=store.read().generation;PcExamples.add(s);store.save(s);}
 PcApi.ImportedOrder input(){State s=store.read();PcApi.ImportedOrder input=new PcApi.ImportedOrder();for(PcItem i:PcApi.resolve(s,"DEMO-PEDIDO-68475").items){PcApi.ImportedLine line=new PcApi.ImportedLine();line.sku=s.dimensionProducts.stream().filter(p->p.id.equals(i.productId)).findFirst().orElseThrow().sku;line.quantity=i.quantity;line.origin=i.origin;line.mode=i.mode;input.items.add(line);}return input;}
 @Test void finalListWorksWithoutBaseSkuAndDoesNotWrite(){State before=store.read();var response=api.preview(input());var plan=(PcApi.PcPlan)response.get("plan");assertTrue(plan.issues.isEmpty());assertEquals(3,plan.groups.size());assertEquals(1,plan.groups.stream().filter(g->g.kind.equals("FACTORY")).count());assertEquals(2,plan.groups.stream().filter(g->g.kind.equals("EXTRA")).findFirst().orElseThrow().selection.size());for(var g:plan.groups)if(g.result!=null){assertEquals("FOUND",g.result.status());assertTrue(g.result.occupancy()<=.85);}assertEquals(before.generation,store.read().generation);}
 @Test void unknownSkuFailsWithoutPartialSuggestion(){var input=input();input.items.get(0).sku="UNKNOWN";assertThrows(IllegalArgumentException.class,()->api.preview(input));}
 @Test void invalidQuantityAndEmptyListFail(){var input=input();input.items.get(0).quantity=0;assertThrows(IllegalArgumentException.class,()->api.preview(input));assertThrows(IllegalArgumentException.class,()->api.preview(new PcApi.ImportedOrder()));}
 @Test void absentMeasuresBlockRecommendation(){var input=input();State s=store.read();s.dimensionProducts.stream().filter(p->p.category.equals("PREPARED_PC")).forEach(p->p.l=null);store.save(s);var plan=(PcApi.PcPlan)api.preview(input).get("plan");assertEquals("PENDING",plan.groups.get(0).result.status());}
 @Test void standaloneMemoryUsesOwnMeasuresWithoutRequiringPc(){
  var input=new PcApi.ImportedOrder();var line=new PcApi.ImportedLine();line.sku="DEMO-HF-RAM";line.origin="EXTRA";input.items.add(line);
  var response=api.preview(input);var pc=(PcPreset)response.get("composition");assertEquals("MAIN",pc.items.get(0).mode);
  var plan=(PcApi.PcPlan)response.get("plan");assertTrue(plan.issues.isEmpty());assertEquals("EXTRA",plan.groups.get(0).kind);assertEquals("FOUND",plan.groups.get(0).result.status());assertEquals(1,plan.groups.get(0).result.placements().size());
  State s=store.read();s.dimensionProducts.stream().filter(p->p.sku.equals(line.sku)).forEach(p->p.l=null);store.save(s);
  plan=(PcApi.PcPlan)api.preview(input).get("plan");assertEquals("PENDING",plan.groups.get(0).result.status());
 }
}
