package br.com.inbox;

import java.util.*;
import java.time.Instant;
import java.io.IOException;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import static br.com.inbox.Model.*;
import static br.com.inbox.Validation.require;

@RestController
@RequestMapping("/api/simple/site")
public class SiteApi {
    private final Store store;private final PichauSite site;private final PcApi pcs;private final DimensionsApi dimensions;
    public SiteApi(Store store,PichauSite site,PcApi pcs,DimensionsApi dimensions){this.store=store;this.site=site;this.pcs=pcs;this.dimensions=dimensions;}
    public record Lookup(String query) {}
    public record Response(String sku,String name,String url,String checkedAt,String status,String message,PcPreset pc,PcApi.PcPlan plan,DimensionsApi.Result result,List<String> details) {}
    @PostMapping("/lookup") public Response lookup(@RequestBody Lookup input) throws IOException,InterruptedException {
        require(input!=null&&input.query()!=null&&!input.query().isBlank(),"Digite um SKU ou link da Pichau.");
        State s;synchronized(store){s=store.read();}PichauSite.ProductPage page=site.lookup(s,input.query());return match(s,page);
    }
    public Response match(State s,PichauSite.ProductPage page){
        String now=Instant.now().toString();
        Optional<PcPreset> recipe=s.dimensionPcs.stream().filter(p->p.kind.equals("BASE")&&p.code.equalsIgnoreCase(page.sku())).findFirst();
        if(recipe.isPresent()){
            List<String> missing=PichauSite.missingClaims(page,recipe.get());
            if(!missing.isEmpty())return new Response(page.sku(),page.name(),page.url(),now,"PENDING","A composição pública mudou ou está incompleta. Confira o vínculo das embalagens antes de sugerir uma caixa.",null,null,null,missing);
            PcApi.PcPlan plan=pcs.calculate(s,recipe.get());
            return new Response(page.sku(),page.name(),page.url(),now,"MATCHED","Consulta ao catálogo público concluída. As embalagens são do cadastro local.",recipe.get(),plan,null,List.of());
        }
        // A commercial PC SKU needs all accompanying envelopes, not just its tower.
        if(!page.sku().toUpperCase(Locale.ROOT).startsWith("PCM-")){
            Optional<DimensionProduct> product=s.dimensionProducts.stream().filter(p->p.sku.equalsIgnoreCase(page.sku())).findFirst();
            if(product.isPresent())return new Response(page.sku(),page.name(),page.url(),now,"MATCHED","Produto encontrado; medidas da embalagem consultadas no cadastro local.",null,null,dimensions.calculate(s,List.of(new DimensionsApi.Selection(product.get().id,1))),List.of());
        }
        return new Response(page.sku(),page.name(),page.url(),now,"PENDING","Produto encontrado no site. Falta cadastrar/vincular as medidas das embalagens desse produto ou configuração.",null,null,null,page.headings());
    }
    @ExceptionHandler(IllegalArgumentException.class) public ResponseEntity<Map<String,String>> invalid(IllegalArgumentException e){return ResponseEntity.badRequest().body(Map.of("message",Objects.toString(e.getMessage(),"Consulta inválida.")));}
    @ExceptionHandler(IOException.class) public ResponseEntity<Map<String,String>> unavailable(IOException e){return ResponseEntity.status(503).body(Map.of("message",Objects.toString(e.getMessage(),"Não foi possível consultar a Pichau agora.")));}
    @ExceptionHandler(InterruptedException.class) public ResponseEntity<Map<String,String>> interrupted(){Thread.currentThread().interrupt();return ResponseEntity.status(503).body(Map.of("message","A consulta foi interrompida. Tente novamente."));}
}
