package br.com.inbox;

import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import static br.com.inbox.Model.*;
import static br.com.inbox.Validation.require;

/** Small dimension-only workflow, independent of the earlier order-management prototype. */
@RestController
@RequestMapping("/api/simple")
public class DimensionsApi {
    private final Store store;
    private final PackingEngine engine=new PackingEngine();
    public DimensionsApi(Store store) {this.store=store;}
    private State current(long revision) {
        State s=store.read();
        if(s.generation!=revision)throw new org.springframework.web.server.ResponseStatusException(HttpStatus.CONFLICT,"Os cadastros mudaram. Atualize a tela antes de salvar.");
        return s;
    }
    public static Map<String,Object> catalog(State s) {return Map.of("products",s.dimensionProducts,"boxes",s.dimensionBoxes,"presets",s.dimensionPcs,"generation",s.generation);}
    public static void dims(Integer l,Integer w,Integer h) {
        for(Integer value:Arrays.asList(l,w,h))require(value==null || value>0 && value<=100000,"As medidas devem ser positivas e ter no máximo 10.000 cm.");
    }
    public static void validate(State s) {
        require(s.dimensionProducts!=null && s.dimensionBoxes!=null,"Cadastros de dimensões ausentes.");
        Set<String> ids=new HashSet<>(),skus=new HashSet<>();
        for(DimensionProduct p:s.dimensionProducts) {
            CatalogTopics.normalize(p);
            Validation.text(p.id,"ID");Validation.text(p.name,"Nome do produto");require(ids.add(p.id),"Produto duplicado no arquivo.");dims(p.l,p.w,p.h);
            require(p.sku!=null && p.sku.length()<=100,"Código inválido.");
            require(p.sku.isBlank()||skus.add(PcApi.normalized(p.sku)),"SKU de produto duplicado.");
            require(PcApi.CATEGORIES.contains(p.category) && PcApi.MODES.contains(p.pcMode),"Categoria ou uso no PC inválido.");
        }
        ids.clear();
        for(DimensionBox b:s.dimensionBoxes){Validation.text(b.id,"ID");Validation.text(b.name,"Nome da caixa");require(ids.add(b.id),"Caixa duplicada no arquivo.");dims(b.l,b.w,b.h);}
        PcApi.validatePresets(s);
    }
    @GetMapping("/state") public Map<String,Object> state() {synchronized(store){return catalog(store.read());}}
    @PutMapping("/products/{id}") public Map<String,Object> product(@PathVariable String id,@RequestBody DimensionProduct p,@RequestHeader("X-State-Revision") long revision) {
        synchronized(store){State s=current(revision);require(id.equals(p.id),"ID inválido.");s.dimensionProducts.removeIf(x->x.id.equals(id));s.dimensionProducts.add(p);validate(s);store.save(s);return catalog(s);}
    }
    @PutMapping("/boxes/{id}") public Map<String,Object> box(@PathVariable String id,@RequestBody DimensionBox b,@RequestHeader("X-State-Revision") long revision) {
        synchronized(store){State s=current(revision);require(id.equals(b.id),"ID inválido.");s.dimensionBoxes.removeIf(x->x.id.equals(id));s.dimensionBoxes.add(b);validate(s);store.save(s);return catalog(s);}
    }
    @DeleteMapping("/{type}/{id}") public Map<String,Object> delete(@PathVariable String type,@PathVariable String id,@RequestHeader("X-State-Revision") long revision) {
        synchronized(store){State s=current(revision);if(type.equals("products")){require(s.dimensionPcs.stream().noneMatch(p->p.items.stream().anyMatch(i->i.productId.equals(id))),"Este produto pertence a uma configuração ou pedido salvo. Substitua-o nessa composição antes de excluir.");s.dimensionProducts.removeIf(x->x.id.equals(id));}else if(type.equals("boxes"))s.dimensionBoxes.removeIf(x->x.id.equals(id));else throw new IllegalArgumentException("Cadastro inválido.");store.save(s);return catalog(s);}
    }
    public static record Selection(String productId,int quantity) {}
    public static record Result(String status,String message,DimensionBox box,Double occupancy,boolean demo,List<Placement> placements) {}
    @PostMapping("/calculate") public Result calculate(@RequestBody List<Selection> selection) {
        State s; synchronized(store){s=store.read();}
        return calculate(s,selection);
    }
    public Result calculate(State s,List<Selection> selection) {
        require(selection!=null && !selection.isEmpty() && selection.size()<=100,"Selecione pelo menos um produto (até 100 itens).");
        Map<String,DimensionProduct> map=new HashMap<>();s.dimensionProducts.forEach(p->map.put(p.id,p));
        List<ObjectItem> objects=new ArrayList<>();long total=0;boolean demo=false;int units=0;
        for(Selection selected:selection){
            require(selected.quantity>0 && selected.quantity<=100,"Quantidade deve ser de 1 a 100.");units+=selected.quantity;require(units<=100,"Selecione até 100 unidades por cálculo.");
            DimensionProduct p=map.get(selected.productId);require(p!=null,"Produto não encontrado. Atualize a seleção.");
            if(p.l==null || p.w==null || p.h==null)return new Result("PENDING","Preencha as três medidas de "+p.name+".",null,null,false,List.of());
            Product physical=new Product();physical.id=p.id;physical.name=p.name;physical.sku=p.sku;
            Pack pkg=new Pack();pkg.l=p.l;pkg.w=p.w;pkg.h=p.h;pkg.weight=0;pkg.status=p.demo?"DEMO":"VALIDATED";
            objects.add(new ObjectItem(p.id,physical,pkg,selected.quantity));total+=PackingEngine.volume(p.l,p.w,p.h)*selected.quantity;demo|=p.demo;
        }
        List<DimensionBox> boxes=s.dimensionBoxes.stream().filter(b->b.l!=null && b.w!=null && b.h!=null)
            .sorted(Comparator.comparingLong(b->PackingEngine.volume(b.l,b.w,b.h))).toList();
        if(boxes.isEmpty())return new Result("PENDING","Cadastre uma caixa com as três medidas internas.",null,null,demo,List.of());
        for(DimensionBox b:boxes){
            long volume=PackingEngine.volume(b.l,b.w,b.h);if(total>volume*.85)continue;
            Box physical=new Box();physical.l=b.l;physical.w=b.w;physical.h=b.h;
            List<Placement> placed=engine.fit(objects,physical);
            if(placed!=null)return new Result("FOUND","Caixa sugerida",b,(double)total/volume,demo||b.demo,placed);
        }
        return new Result("NOT_FOUND","Não foi encontrado encaixe nas caixas cadastradas com 15% de espaço livre. Confira as medidas ou cadastre uma caixa maior.",null,null,demo,List.of());
    }
    @PostMapping("/examples") public Map<String,Object> examples(@RequestHeader("X-State-Revision") long revision) {
        synchronized(store){State s=current(revision);require(s.dimensionProducts.isEmpty() && s.dimensionBoxes.isEmpty(),"Os exemplos só podem ser carregados com os cadastros vazios.");
            String[] names={"Caixa do gabinete · exemplo","Caixa da placa-mãe · exemplo","Kit de periféricos · exemplo"};
            int[][] sizes={{470,250,450},{310,250,70},{450,180,60}};
            for(int i=0;i<names.length;i++){DimensionProduct p=new DimensionProduct();p.name=names[i];p.sku="EXEMPLO-"+(i+1);p.l=sizes[i][0];p.w=sizes[i][1];p.h=sizes[i][2];p.demo=true;s.dimensionProducts.add(p);}
            int[][] boxSizes={{600,450,500},{700,600,600}};
            for(int i=0;i<boxSizes.length;i++){DimensionBox b=new DimensionBox();b.name="Caixa "+(i+1)+" · exemplo";b.l=boxSizes[i][0];b.w=boxSizes[i][1];b.h=boxSizes[i][2];b.demo=true;s.dimensionBoxes.add(b);}
            store.save(s);return catalog(s);
        }
    }
    @ExceptionHandler(IllegalArgumentException.class) public ResponseEntity<Map<String,String>> invalid(IllegalArgumentException e) {return ResponseEntity.badRequest().body(Map.of("message",Objects.toString(e.getMessage(),"Confira os campos.")));}
    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class) public ResponseEntity<Map<String,String>> conflict(org.springframework.web.server.ResponseStatusException e) {return ResponseEntity.status(e.getStatusCode()).body(Map.of("message",Objects.toString(e.getReason(),"Atualize a tela.")));}
    @ExceptionHandler({NullPointerException.class,org.springframework.http.converter.HttpMessageNotReadableException.class}) public ResponseEntity<Map<String,String>> incomplete() {return ResponseEntity.badRequest().body(Map.of("message","Confira os campos e use números válidos."));}
}
