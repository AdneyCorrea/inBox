package br.com.inbox;

import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import static br.com.inbox.Model.*;
import static br.com.inbox.Validation.require;

@RestController
@RequestMapping("/api/simple/pc")
public class PcApi {
    public static final Set<String> MODES=Set.of("MAIN","NESTED","OEM","FACTORY");
    public static final Set<String> CATEGORIES=Set.of("PREPARED_PC","CPU","MOTHERBOARD","MEMORY","SSD","GPU","PSU","COOLER","FAN","PERIPHERAL","MONITOR","MONITOR_SUPPORT","UPS","CHAIR","DESK","DESK_LEG","CABLE","OTHER");
    private final Store store;
    private final DimensionsApi dimensions;
    public PcApi(Store store,DimensionsApi dimensions){this.store=store;this.dimensions=dimensions;}
    private State current(long revision){State s=store.read();if(s.generation!=revision)throw new ResponseStatusException(HttpStatus.CONFLICT,"Os cadastros mudaram. Atualize antes de salvar.");return s;}
    public static String normalized(String code){return code==null?"":code.trim().toUpperCase(Locale.ROOT);}
    public static String defaultMode(DimensionProduct p,String origin){
        if("CONFIG".equals(origin)&&(p.oem||"CABLE".equals(p.category)))return "OEM";
        if(Set.of("CHAIR","DESK","DESK_LEG").contains(p.category)||"EXTRA".equals(origin)&&Set.of("MONITOR","MONITOR_SUPPORT","UPS").contains(p.category))return "FACTORY";
        return "CONFIG".equals(origin)&&Set.of("MEMORY","SSD").contains(p.category)?"NESTED":"MAIN";
    }
    public static void validatePreset(State s,PcPreset pc){
        Validation.text(pc.id,"ID da composição");Validation.text(pc.code,"Código/SKU");Validation.text(pc.name,"Nome");
        require(Set.of("BASE","ORDER").contains(pc.kind),"Tipo de composição inválido.");
        require(pc.items!=null && !pc.items.isEmpty() && pc.items.size()<=100,"A composição final precisa de 1 a 100 linhas.");
        require(pc.aliases!=null && pc.aliases.size()<=10,"Aliases inválidos.");
        Set<String> ids=new HashSet<>();int units=0;
        for(PcItem i:pc.items){
            Validation.text(i.id,"ID do item");require(ids.add(i.id),"Linhas duplicadas.");
            require(s.dimensionProducts.stream().anyMatch(p->p.id.equals(i.productId)),"Produto não cadastrado na composição.");
            require(MODES.contains(i.mode) && Set.of("CONFIG","EXTRA").contains(i.origin),"Origem ou tratamento inválido.");
            require(i.quantity>0 && i.quantity<=100,"Use quantidades inteiras de 1 a 100.");units+=i.quantity;
        }
        require(units<=100,"O protótipo aceita até 100 unidades na composição.");
        require(pc.sourceUrl!=null && pc.note!=null && pc.baseCode!=null,"Composição incompleta.");
        require(pc.note.length()<=2000,"Observação muito longa.");
        require(pc.sourceUrl.isEmpty() || pc.sourceUrl.startsWith("https://www.pichau.com.br/"),"Referência deve ser uma página da Pichau.");
    }
    public static void validatePresets(State s){
        require(s.dimensionPcs!=null,"Configurações ausentes do backup.");Set<String> ids=new HashSet<>(),codes=new HashSet<>();
        for(PcPreset pc:s.dimensionPcs){validatePreset(s,pc);require(ids.add(pc.id),"Configuração duplicada.");require(codes.add(normalized(pc.code)),"Código/SKU duplicado.");
            for(String alias:pc.aliases){Validation.text(alias,"Alias");require(codes.add(normalized(alias)),"Código alternativo duplicado.");}}
    }
    public static PcPreset resolve(State s,String code){String key=normalized(code);require(!key.isEmpty(),"Digite ou leia um código.");return s.dimensionPcs.stream().filter(pc->normalized(pc.code).equals(key)||pc.aliases.stream().anyMatch(a->normalized(a).equals(key))).findFirst().orElseThrow(()->new IllegalArgumentException("Código não cadastrado. Use um dos exemplos, salve a composição do pedido ou importe seu arquivo."));}
    @GetMapping("/lookup") public PcPreset lookup(@RequestParam String code){synchronized(store){return resolve(store.read(),code);}}
    @PutMapping("/{id}") public Map<String,Object> save(@PathVariable String id,@RequestBody PcPreset pc,@RequestHeader("X-State-Revision") long revision){
        synchronized(store){State s=current(revision);require(id.equals(pc.id),"ID inválido.");validatePreset(s,pc);
            PcPreset existing=s.dimensionPcs.stream().filter(p->p.id.equals(id)).findFirst().orElse(null);
            require(existing==null || !existing.kind.equals("BASE") || pc.kind.equals("BASE"),"Salve o pedido com um identificador independente da configuração base.");
            s.dimensionPcs.removeIf(p->p.id.equals(id));s.dimensionPcs.add(pc);DimensionsApi.validate(s);store.save(s);return DimensionsApi.catalog(s);}
    }
    @DeleteMapping("/{id}") public Map<String,Object> delete(@PathVariable String id,@RequestHeader("X-State-Revision") long revision){synchronized(store){State s=current(revision);s.dimensionPcs.removeIf(pc->pc.id.equals(id));store.save(s);return DimensionsApi.catalog(s);}}
    public static class Group {
        public String id,name,kind;
        public int volumes;
        public List<DimensionsApi.Selection> selection=new ArrayList<>();
        public DimensionsApi.Result result;
        public Group(String id,String name,String kind){this.id=id;this.name=name;this.kind=kind;}
    }
    public static class PcPlan {
        public List<Group> groups=new ArrayList<>();
        public List<String> notes=new ArrayList<>(),issues=new ArrayList<>();
        public boolean demo;
    }
    @PostMapping("/calculate") public PcPlan calculate(@RequestBody PcPreset pc){State s;synchronized(store){s=store.read();}return calculate(s,pc);}
    public PcPlan calculate(State s,PcPreset pc){
        validatePreset(s,pc);Map<String,DimensionProduct> products=new HashMap<>();s.dimensionProducts.forEach(p->products.put(p.id,p));
        PcPlan plan=new PcPlan();plan.demo=pc.demo;
        Group main=new Group("pc","Computador e itens incluídos","MAIN"),extras=new Group("extras","Avulsos agrupados","EXTRA");
        boolean motherboard=pc.items.stream().anyMatch(i->i.origin.equals("CONFIG")&&i.mode.equals("MAIN")&&products.get(i.productId).category.equals("MOTHERBOARD"));
        long prepared=pc.items.stream().filter(i->i.origin.equals("CONFIG")&&products.get(i.productId).category.equals("PREPARED_PC")&&i.mode.equals("MAIN")).mapToInt(i->i.quantity).sum();
        if(pc.items.stream().anyMatch(i->i.origin.equals("CONFIG"))&&prepared!=1)plan.issues.add("Selecione exatamente um gabinete para a configuração.");
        for(PcItem item:pc.items){
            DimensionProduct product=products.get(item.productId);plan.demo|=product.demo;
            if(item.origin.equals("CONFIG")&&(product.oem||"CABLE".equals(product.category))){plan.notes.add(product.name+": "+(product.oem?"OEM":"cabo incluído")+", medidas desconsideradas no PC montado.");continue;}
            boolean factory=Set.of("CHAIR","DESK","DESK_LEG").contains(product.category)||item.origin.equals("CONFIG")&&item.mode.equals("FACTORY")
                ||item.origin.equals("EXTRA")&&Set.of("MONITOR","MONITOR_SUPPORT","UPS").contains(product.category);
            if(factory){Group g=new Group(item.id,product.name,"FACTORY");g.selection.add(new DimensionsApi.Selection(item.productId,item.quantity));g.volumes=item.quantity;plan.groups.add(g);continue;}
            if(item.origin.equals("CONFIG")&&item.mode.equals("NESTED")){
                plan.notes.add(product.name+": dentro da caixa da placa-mãe, sem volume externo adicional.");
                if(!motherboard)plan.issues.add("Caixa da placa-mãe de destino ausente para "+product.name+".");continue;
            }
            if(item.origin.equals("CONFIG")&&item.mode.equals("OEM")){plan.notes.add(product.name+": instalado/OEM, sem caixa separada.");continue;}
            Group destination=item.origin.equals("EXTRA")?extras:main;
            destination.selection.add(new DimensionsApi.Selection(item.productId,item.quantity));
        }
        if(!main.selection.isEmpty())plan.groups.add(0,main);
        if(!extras.selection.isEmpty())plan.groups.add(extras);
        for(Group group:plan.groups){
            if(group.kind.equals("FACTORY"))continue;
            group.result=plan.issues.isEmpty()?dimensions.calculate(s,group.selection):new DimensionsApi.Result("PENDING",String.join(" ",plan.issues),null,null,plan.demo,List.of());
            // The whole composition may still be a demonstrative recipe, even with measured envelopes.
            if(plan.demo&&!group.result.demo())group.result=new DimensionsApi.Result(group.result.status(),group.result.message(),group.result.box(),group.result.occupancy(),true,group.result.placements());
        }
        return plan;
    }
    public static class ImportedLine {public String sku="",mode,origin="CONFIG";public int quantity=1;}
    public static class ImportedOrder {public String code="",configurationSku="",name="";public boolean demo;public List<ImportedLine> items=new ArrayList<>();}
    @PostMapping("/import") public Map<String,Object> importOrder(@RequestBody ImportedOrder input,@RequestHeader("X-State-Revision") long revision){
        synchronized(store){State s=current(revision);require(input.items!=null&&!input.items.isEmpty(),"O arquivo deve conter a lista final completa de peças em items. Alterações não podem ser deduzidas do SKU base.");
            PcPreset base=resolve(s,input.configurationSku);require(base.kind.equals("BASE"),"Informe o SKU da configuração base.");
            PcPreset pc=new PcPreset();pc.code=input.code.trim();pc.name=input.name.isBlank()?base.name:input.name;pc.kind="ORDER";pc.baseCode=base.code;pc.sourceUrl=base.sourceUrl;pc.demo=input.demo;
            for(ImportedLine line:input.items){DimensionProduct p=s.dimensionProducts.stream().filter(x->normalized(x.sku).equals(normalized(line.sku))&&!x.sku.isBlank()).findFirst().orElseThrow(()->new IllegalArgumentException("SKU de componente não cadastrado: "+line.sku));
                PcItem item=new PcItem();item.productId=p.id;item.quantity=line.quantity;item.mode=line.mode==null?PcApi.defaultMode(p,line.origin):line.mode;item.origin=line.origin;pc.items.add(item);}
            validatePreset(s,pc);s.dimensionPcs.add(pc);DimensionsApi.validate(s);store.save(s);return Map.of("catalog",DimensionsApi.catalog(s),"preset",pc);}
    }
    @PostMapping("/examples") public Map<String,Object> examples(@RequestHeader("X-State-Revision") long revision){synchronized(store){State s=current(revision);PcExamples.addHighflyer(s);DimensionsApi.validate(s);store.save(s);return DimensionsApi.catalog(s);}}
    @ExceptionHandler(IllegalArgumentException.class) public ResponseEntity<Map<String,String>> invalid(IllegalArgumentException e){return ResponseEntity.badRequest().body(Map.of("message",Objects.toString(e.getMessage(),"Confira a composição.")));}
    @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<Map<String,String>> conflict(ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).body(Map.of("message",Objects.toString(e.getReason(),"Atualize a tela.")));}
    @ExceptionHandler({NullPointerException.class,org.springframework.http.converter.HttpMessageNotReadableException.class}) public ResponseEntity<Map<String,String>> incomplete(){return ResponseEntity.badRequest().body(Map.of("message","Composição incompleta ou arquivo inválido."));}
}
