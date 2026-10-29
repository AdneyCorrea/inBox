// Recebe uma lista de peças em JSON e retorna sua composição e previsão de embalagem.
package br.com.inbox;

import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import static br.com.inbox.Model.*;
import static br.com.inbox.Validation.require;

/** Stateless boundary for a future ERP adapter: only the final item list is needed. */
/** Entrada da lista final de pecas por JSON; nao depende de consulta ao site. */
@RestController @RequestMapping("/api/simple/packing") public class PackingListApi {
    private final Store store;
    private final PcApi pcs;
    public PackingListApi(Store store,PcApi pcs){
        this.store=store;
        this.pcs=pcs;
    }
    @PostMapping("/preview")
public Map<String,Object> preview(@RequestBody PcApi.ImportedOrder input){
        State s;
        synchronized(store){
            s=store.read();
        }
        require(input.items!=null&&!input.items.isEmpty()&&input.items.size()<=100,"Informe a lista final de 1 a 100 linhas.");
        PcPreset pc=new PcPreset();
        pc.kind="ORDER";
        pc.code="SIMULACAO";
        pc.name="Lista final de peças";
        pc.demo=input.demo;
        for(PcApi.ImportedLine line:input.items){
            if (line == null) throw new IllegalArgumentException("Informe o SKU de cada peça.");
            require(!PcApi.normalized(line.sku).isEmpty(),"Informe o SKU de cada peça.");
            DimensionProduct p=s.dimensionProducts.stream().filter(x->!x.sku.isBlank()&&PcApi.normalized(x.sku).equals(PcApi.normalized(line.sku))).findFirst().orElseThrow(()->new IllegalArgumentException("SKU não cadastrado: "+line.sku));
            PcItem item=new PcItem();
            item.productId=p.id;
            item.quantity=line.quantity;
            item.origin=line.origin;
            item.mode=line.mode==null?PcApi.defaultMode(p,line.origin):line.mode;
            pc.items.add(item);
        }
        return Map.of("composition",pc,"plan",pcs.calculate(s,pc));
    }
    @ExceptionHandler({
        IllegalArgumentException.class,NullPointerException.class
    }) public ResponseEntity<Map<String,String>> invalid(Exception e){
        return ResponseEntity.badRequest().body(Map.of("message",Objects.toString(e.getMessage(),"Lista incompleta.")));
    }
}
