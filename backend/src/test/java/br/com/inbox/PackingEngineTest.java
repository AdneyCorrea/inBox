package br.com.inbox;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static br.com.inbox.Model.*;

class PackingEngineTest {
    private final PackingEngine engine=new PackingEngine();
    private Pack pack(int l,int w,int h,int weight) {Pack p=new Pack();p.l=l;p.w=w;p.h=h;p.weight=weight;p.status="DEMO";return p;}
    private Box box(int l,int w,int h,int maxWeight) {Box b=new Box();b.l=l;b.w=w;b.h=h;b.outerL=l+10;b.outerW=w+10;b.outerH=h+10;b.maxWeight=maxWeight;b.tare=50;b.status="DEMO";return b;}
    private ObjectItem item(Pack pack,int q) {Product p=new Product();p.name="Item";return new ObjectItem(p.id,p,pack,q);}
    @Test void allowsExactlyEightyFivePercent() {Candidate c=engine.choose(List.of(item(pack(85,100,100,100),1)),List.of(box(100,100,100,1000)));assertNotNull(c);assertEquals(.85,c.occupancy);assertEquals(150,c.shippedWeight);}
    @Test void rejectsAboveEightyFivePercent() {assertNull(engine.choose(List.of(item(pack(86,100,100,100),1)),List.of(box(100,100,100,1000))));}
    @Test void volumeAloneDoesNotProveFit() {assertNull(engine.choose(List.of(item(pack(120,20,20,100),1)),List.of(box(100,100,100,1000))));}
    @Test void checksContentWeightNotTare() {assertNotNull(engine.choose(List.of(item(pack(10,10,10,100),1)),List.of(box(100,100,100,100))));assertNull(engine.choose(List.of(item(pack(10,10,10,101),1)),List.of(box(100,100,100,100))));}
    @Test void respectsRotationRestriction() {Pack p=pack(40,70,20,10);p.rotatable=false;assertNull(engine.choose(List.of(item(p,1)),List.of(box(80,50,50,100))));p.rotatable=true;assertNotNull(engine.choose(List.of(item(p,1)),List.of(box(80,50,50,100))));}
    @Test void selectsSmallestAvailablePassingBox() {Box small=box(40,40,40,100);Box large=box(100,100,100,100);assertSame(small,engine.choose(List.of(item(pack(20,20,20,10),1)),List.of(large,small)).box);small.available=false;assertSame(large,engine.choose(List.of(item(pack(20,20,20,10),1)),List.of(large,small)).box);}
    @Test void objectsHaveNoOverlapAndStayInside() {Box b=box(100,100,100,1000);List<Placement> placements=engine.fit(List.of(item(pack(30,30,30,10),12)),b);assertNotNull(placements);assertEquals(12,placements.size());for(int i=0;i<placements.size();i++){Placement a=placements.get(i);assertTrue(a.x+a.l<=b.l && a.y+a.w<=b.w && a.z+a.h<=b.h);for(int j=i+1;j<placements.size();j++)assertFalse(PackingEngine.overlap(a,placements.get(j)));}}
    @Test void noStackingOnFragilePackage() {Pack p=pack(90,90,30,10);p.stackable=false;assertNull(engine.fit(List.of(item(p,2)),box(100,100,100,1000)));}
    @Test void demoOrderSeparatesOriginAndFactoryPackages() {State s=Demo.create();Plan plan=engine.plan(s,s.orders.get(0));assertEquals(5,plan.groups.size());assertEquals(4,plan.knownFactoryVolumes);assertTrue(plan.demo);assertFalse(plan.ready);Group main=plan.groups.stream().filter(g->g.kind.equals("PC_MAIN")).findFirst().orElseThrow();assertEquals(1,main.objects.stream().filter(o->o.sku.equals("DEMO-MON")).count());Group shared=plan.groups.stream().filter(g->g.kind.equals("STANDALONE_SHARED")).findFirst().orElseThrow();assertTrue(shared.objects.stream().noneMatch(o->o.sku.equals("DEMO-MON")));assertNotNull(main.candidate);}
    @Test void nestedItemsAreNotCountedTwice() {State s=Demo.create();Plan p=engine.plan(s,s.orders.get(0));Group main=p.groups.get(0);assertTrue(main.objects.stream().noneMatch(o->Set.of("DEMO-RAM","DEMO-SSD","DEMO-CPU-OEM").contains(o.sku)));assertTrue(main.notes.size()>=3);}
    @Test void missingMotherboardRecipientRequiresReview() {State s=Demo.create();Order o=s.orders.get(0);o.configurations.get(0).lines.removeIf(l->s.products.stream().anyMatch(p->p.id.equals(l.productId)&&p.category.equals("MOTHERBOARD")));assertTrue(engine.plan(s,o).groups.get(0).issues.stream().anyMatch(x->x.contains("destino ausente")));}
    @Test void pendingMeasurementsNeverSuggestABox() {State s=Demo.create();Plan p=engine.plan(s,s.orders.get(1));assertFalse(p.ready);assertEquals(0,p.suggestedBoxes);assertNull(p.groups.get(0).candidate);}
    @Test void validatedDataRequiresProvenance() {Product p=new Product();p.sku="A";p.name="Produto";Pack pkg=pack(10,10,10,1);pkg.status="VALIDATED";p.packages.add(pkg);assertThrows(IllegalArgumentException.class,()->Validation.product(p));}
    @Test void twoPcInstancesMustHaveDifferentIds() {State s=Demo.create();Order o=s.orders.get(0);o.configurations.add(o.configurations.get(0));assertThrows(IllegalArgumentException.class,()->Validation.order(o,s));}
}
