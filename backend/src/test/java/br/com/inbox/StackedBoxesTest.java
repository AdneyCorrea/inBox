package br.com.inbox;
import java.util.List;
import org.junit.jupiter.api.Test;
import static br.com.inbox.Model.*;
import static org.junit.jupiter.api.Assertions.*;
class StackedBoxesTest {
 @Test void choosesSmallestCartonByStackingWithBubbleReserve(){
  State s=new State();DimensionProduct p=new DimensionProduct();p.name="Produto";p.l=200;p.w=200;p.h=100;s.dimensionProducts.add(p);
  DimensionBox small=new DimensionBox();small.name="Pequena";small.l=220;small.w=220;small.h=250;
  DimensionBox large=new DimensionBox();large.name="Grande";large.l=500;large.w=500;large.h=500;
  s.dimensionBoxes.add(large);s.dimensionBoxes.add(small);
  var result=new DimensionsApi(null).calculate(s,List.of(new DimensionsApi.Selection(p.id,2)));
  assertEquals("FOUND",result.status());assertEquals(small.id,result.box().id);assertEquals(2,result.placements().size());assertTrue(result.placements().stream().anyMatch(x->x.z>0));assertTrue(result.occupancy()<=.85);
  var a=result.placements().get(0);var b=result.placements().get(1);assertFalse(PackingEngine.overlap(a,b));assertEquals(a.z+a.h,b.z);
 }
}
