package br.com.inbox;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static br.com.inbox.Model.*;

/** Cenários físicos: validar a caixa importa mais do que testar detalhes do algoritmo. */
class PackagingRegressionTest {
    final PackingEngine engine=new PackingEngine();
    final DimensionsApi dimensions=new DimensionsApi(null);
    final PcApi pcs=new PcApi(null,dimensions);

    static Box box(int l,int w,int h){Box b=new Box();b.l=l;b.w=w;b.h=h;return b;}
    static ObjectItem item(int l,int w,int h,int quantity){
        Pack p=new Pack();p.l=l;p.w=w;p.h=h;p.weight=0;
        return new ObjectItem(Model.id(),new Product(),p,quantity);
    }
    static DimensionProduct product(State s,String category,int l,int w,int h){
        DimensionProduct p=new DimensionProduct();p.name=category;p.category=category;p.l=l;p.w=w;p.h=h;
        s.dimensionProducts.add(p);return p;
    }
    static PcItem line(PcPreset pc,DimensionProduct p,String origin){
        PcItem i=new PcItem();i.productId=p.id;i.origin=origin;i.mode=PcApi.defaultMode(p,origin);pc.items.add(i);return i;
    }
    static State catalog(){
        State s=new State();DimensionBox b=new DimensionBox();b.name="Teste";b.l=100;b.w=100;b.h=100;s.dimensionBoxes.add(b);return s;
    }
    static PcPreset pc(){PcPreset p=new PcPreset();p.code="TESTE";p.name="Teste de embalagem";return p;}

    @Test void rotatesOnlyWhenAllowed(){
        ObjectItem o=item(80,40,30,1);Box b=box(40,80,30);
        assertNotNull(engine.fit(List.of(o),b));
        o.pack.rotatable=false;assertNull(engine.fit(List.of(o),b));
    }
    @Test void stacksOnCompleteSupport(){
        var placements=engine.fit(List.of(item(40,40,20,2)),box(40,40,40));
        assertNotNull(placements);assertEquals(2,placements.size());assertEquals(20,placements.get(1).z);
    }
    @Test void respectsNonStackableBase(){
        var o=item(40,40,20,2);o.pack.rotatable=false;o.pack.stackable=false;
        assertNull(engine.fit(List.of(o),box(40,40,40)));
    }
    @Test void rejectsInsufficientDimensionsEvenWhenVolumeIsEnough(){
        assertNull(engine.fit(List.of(item(110,10,10,1)),box(100,100,100)));
    }
    @Test void supportsOneHundredUnitsAndRejectsMore(){
        assertEquals(100,engine.fit(List.of(item(10,10,10,100)),box(100,100,100)).size());
        assertNull(engine.fit(List.of(item(10,10,10,101)),box(100,100,100)));
    }
    @Test void volumeDoesNotOverflowInteger(){assertEquals(1_000_000_000_000_000L,PackingEngine.volume(100000,100000,100000));}
    @Test void exactlyFifteenPercentFreeIsAccepted(){
        State s=catalog();DimensionProduct p=product(s,"OTHER",100,100,85);
        var r=dimensions.calculate(s,List.of(new DimensionsApi.Selection(p.id,1)));
        assertEquals("FOUND",r.status());assertEquals(.85,r.occupancy(),1e-10);
        p.h=86;assertEquals("NOT_FOUND",dimensions.calculate(s,List.of(new DimensionsApi.Selection(p.id,1))).status());
    }
    @Test void smallestCompatibleBoxWins(){
        State s=catalog();DimensionBox small=new DimensionBox();small.name="Pequena";small.l=30;small.w=30;small.h=30;s.dimensionBoxes.add(small);
        DimensionProduct p=product(s,"OTHER",20,20,20);
        assertEquals(small.id,dimensions.calculate(s,List.of(new DimensionsApi.Selection(p.id,1))).box().id);
    }
    @Test void missingMeasurementsBlockStandalone(){
        State s=catalog();DimensionProduct p=product(s,"CPU",10,10,10);p.oem=true;p.l=null;
        PcPreset pc=pc();line(pc,p,"EXTRA");
        assertEquals("PENDING",pcs.calculate(s,pc).groups.get(0).result.status());
    }
    @Test void oemAndIncludedCablesDoNotNeedMeasurements(){
        State s=catalog();PcPreset pc=pc();line(pc,product(s,"PREPARED_PC",40,40,40),"CONFIG");
        var cpu=product(s,"CPU",10,10,10);cpu.oem=true;cpu.l=null;
        var cable=product(s,"CABLE",10,10,10);cable.h=null;
        line(pc,cpu,"CONFIG");line(pc,cable,"CONFIG");
        var plan=pcs.calculate(s,pc);assertTrue(plan.issues.isEmpty());
        assertEquals(1,plan.groups.get(0).selection.size());assertEquals("FOUND",plan.groups.get(0).result.status());
    }
    @Test void standaloneOemAndCableStillOccupySpace(){
        State s=catalog();PcPreset pc=pc();var cpu=product(s,"CPU",10,10,10);cpu.oem=true;
        line(pc,cpu,"EXTRA");line(pc,product(s,"CABLE",10,10,10),"EXTRA");
        assertEquals(2,pcs.calculate(s,pc).groups.get(0).selection.size());
    }
    @Test void peripheralsStaySeparateFromPc(){
        State s=catalog();PcPreset pc=pc();line(pc,product(s,"PREPARED_PC",40,40,40),"CONFIG");line(pc,product(s,"PERIPHERAL",10,10,10),"EXTRA");
        assertEquals(List.of("MAIN","EXTRA"),pcs.calculate(s,pc).groups.stream().map(g->g.kind).toList());
    }
    @Test void monitorsSupportsAndUpsUseManufacturerBoxWhenStandalone(){
        for(String category:List.of("MONITOR","MONITOR_SUPPORT","UPS")){
            State s=catalog();PcPreset pc=pc();line(pc,product(s,category,10,10,10),"EXTRA");
            var group=pcs.calculate(s,pc).groups.get(0);assertEquals("FACTORY",group.kind);assertEquals(1,group.volumes);
        }
    }
    @Test void furnitureUsesManufacturerBoxEvenWhenIncluded(){
        for(String category:List.of("CHAIR","DESK","DESK_LEG")){
            State s=catalog();PcPreset pc=pc();line(pc,product(s,"PREPARED_PC",40,40,40),"CONFIG");
            line(pc,product(s,category,10,10,10),"CONFIG").quantity=2;
            assertEquals(2,pcs.calculate(s,pc).groups.stream().filter(g->g.kind.equals("FACTORY")).findFirst().orElseThrow().volumes);
        }
    }
    @Test void memoryNeedsMotherboardRecipient(){
        State s=catalog();PcPreset pc=pc();line(pc,product(s,"PREPARED_PC",40,40,40),"CONFIG");line(pc,product(s,"MEMORY",10,10,10),"CONFIG");
        assertFalse(pcs.calculate(s,pc).issues.isEmpty());
        line(pc,product(s,"MOTHERBOARD",30,30,10),"CONFIG");assertTrue(pcs.calculate(s,pc).issues.isEmpty());
    }
    @Test void nullLinesBecomeValidationErrors(){
        State s=catalog();PcPreset pc=pc();pc.items.add(null);
        assertThrows(IllegalArgumentException.class,()->pcs.calculate(s,pc));
        assertThrows(IllegalArgumentException.class,()->dimensions.calculate(s,Arrays.asList((DimensionsApi.Selection)null)));
    }
    @Test void seededRandomArrangementsNeverOverlapOrFloat(){
        Random random=new Random(20261028);int successes=0;
        for(int attempt=0;attempt<150;attempt++){
            List<ObjectItem> objects=new ArrayList<>();
            for(int i=0;i<8;i++)objects.add(item(10+random.nextInt(40),10+random.nextInt(40),10+random.nextInt(40),1));
            Box box=box(120,100,100);var placed=engine.fit(objects,box);if(placed==null)continue;successes++;
            assertEquals(8,placed.size());
            for(int i=0;i<placed.size();i++){
                Placement p=placed.get(i);assertTrue(p.x>=0&&p.y>=0&&p.z>=0&&p.x+p.l<=box.l&&p.y+p.w<=box.w&&p.z+p.h<=box.h);
                for(int j=0;j<i;j++)assertFalse(PackingEngine.overlap(p,placed.get(j)));
                assertTrue(p.z==0||placed.stream().anyMatch(b->b!=p&&b.stackable&&b.z+b.h==p.z&&p.x>=b.x&&p.y>=b.y&&p.x+p.l<=b.x+b.l&&p.y+p.w<=b.y+b.w));
            }
        }
        assertTrue(successes>100,"A amostra precisa exercitar encaixes reais, não só falhas.");
    }
}
