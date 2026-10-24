package br.com.inbox;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static br.com.inbox.Model.*;
class HighflyerCatalogTest {
 @Test void cleanSeedHasOnlyHighflyerAndCategorizedParts(){State s=new State();PcExamples.addHighflyer(s);DimensionsApi.validate(s);assertEquals(1,s.dimensionPcs.size());assertEquals(PcExamples.HF,s.dimensionPcs.get(0).code);assertEquals(10,s.dimensionProducts.size());assertEquals(10,s.dimensionPcs.get(0).items.size());assertTrue(s.dimensionProducts.stream().allMatch(p->!p.category.equals("OTHER")&&p.demo));assertEquals(10,s.dimensionBoxes.size());}
 @Test void repeatedSeedPreservesMeasurements(){State s=new State();PcExamples.addHighflyer(s);s.dimensionProducts.get(0).l=999;PcExamples.addHighflyer(s);assertEquals(10,s.dimensionProducts.size());assertEquals(1,s.dimensionPcs.size());assertEquals(999,s.dimensionProducts.get(0).l);}
}
