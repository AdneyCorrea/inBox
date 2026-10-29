// Expõe a API legada de pedidos e conferências, além de exportar e restaurar backups.
package br.com.inbox;

import java.util.*;
import java.time.Instant;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import static br.com.inbox.Model.*;
import static br.com.inbox.Validation.require;

/** API de compatibilidade e backups. Preserva o formato antigo de pedidos e conferencias. */
@RestController @RequestMapping("/api") public class Api {
    private final Store store;
    private final PackingEngine engine=new PackingEngine();
    public Api(Store store) {
        this.store=store;
    }
    private State current(long generation) {
        State s=store.read();
        if(s.generation!=generation) throw new ResponseStatusException(HttpStatus.CONFLICT,"Os dados foram atualizados. Recarregue antes de salvar.");
        return s;
    }
    private void audit(State s,String action,String reference,String operator,String detail) {
        s.history.add(0,new Event(action,reference,operator,detail));
    }
    private void invalidate(State s) {
        for(Order o:s.orders) if(!"DONE".equals(o.status)) {
            o.checked.clear();
            o.physicallyValidated=false;
        }
    }
    @GetMapping("/state")
public synchronized State state() {
        return store.read();
    }
    @GetMapping("/health")
public Map<String,String> health() {
        return Map.of("app","inbox","version","0.1.0");
    }
    @GetMapping(value="/backup",produces="application/json")
public synchronized ResponseEntity<String> backup() {
        return ResponseEntity.ok().header("Content-Disposition","attachment; filename=inbox-backup.json").body(store.json(store.read()));
    }
    @PostMapping("/restore")
public synchronized State restore(@RequestBody String body,@RequestHeader("X-State-Revision") long revision) throws Exception {
        synchronized(store) {
            State old=current(revision);
            require(body.length()<=5_000_000,"Backup limitado a 5 MB.");
            State imported;
            try {
                imported=store.mapper.readValue(body,State.class);
                Validation.state(imported);
            }
            catch(com.fasterxml.jackson.core.JsonProcessingException | NullPointerException e) {
                throw new IllegalArgumentException("Backup inválido ou incompleto. A base atual foi preservada.");
            }
            // Reimported operational claims require a fresh local review.
            for(Order o:imported.orders) {
                o.checked.clear();
                o.physicallyValidated=false;
                if("DONE".equals(o.status)) {
                    o.status="REVIEW";
                    o.completedAt=null;
                    o.completionSnapshot=null;
                }
            }
            imported.generation=old.generation;
            audit(imported,"IMPORTAÇÃO","Backup","","Backup restaurado; conferências e conclusões exigem nova validação.");
            store.save(imported);
            return imported;
        }
    }
    @PutMapping("/products/{id}")
public synchronized State product(@PathVariable String id,@RequestBody Product p,@RequestHeader("X-State-Revision") long revision) {
        State s=current(revision);
        require(id.equals(p.id),"ID divergente.");
        Validation.product(p);
        Product previous=s.products.stream().filter(x->x.id.equals(id)).findFirst().orElse(null);
        p.revision=previous==null?1:previous.revision+1;
        s.products.removeIf(x->x.id.equals(id));
        s.products.add(p);
        Validation.state(s);
        invalidate(s);
        audit(s,"PRODUTO",p.sku,"","Cadastro salvo. Conferências abertas reiniciadas.");
        store.save(s);
        return s;
    }
    @PutMapping("/boxes/{id}")
public synchronized State box(@PathVariable String id,@RequestBody Box b,@RequestHeader("X-State-Revision") long revision) {
        State s=current(revision);
        require(id.equals(b.id),"ID divergente.");
        Validation.box(b);
        Box previous=s.boxes.stream().filter(x->x.id.equals(id)).findFirst().orElse(null);
        b.revision=previous==null?1:previous.revision+1;
        s.boxes.removeIf(x->x.id.equals(id));
        s.boxes.add(b);
        Validation.state(s);
        invalidate(s);
        audit(s,"CAIXA",b.code,"","Cadastro salvo. Conferências abertas reiniciadas.");
        store.save(s);
        return s;
    }
    @PutMapping("/templates/{id}")
public synchronized State template(@PathVariable String id,@RequestBody Configuration c,@RequestHeader("X-State-Revision") long revision) {
        State s=current(revision);
        require(id.equals(c.id),"ID divergente.");
        Validation.configuration(c,s);
        s.templates.removeIf(x->x.id.equals(id));
        s.templates.add(c);
        audit(s,"CONFIGURAÇÃO",c.sku,"","Modelo salvo; pedidos existentes preservados.");
        store.save(s);
        return s;
    }
    @DeleteMapping("/{type}/{id}")
public synchronized State delete(@PathVariable String type,@PathVariable String id,@RequestHeader("X-State-Revision") long revision) {
        State s=current(revision);
        if("products".equals(type)) {
            boolean used=s.templates.stream().anyMatch(c->uses(c,id)) || s.orders.stream().anyMatch(o->o.configurations.stream().anyMatch(c->uses(c,id)) || o.standalone.stream().anyMatch(l->l.productId.equals(id)));
            require(!used,"Produto utilizado em configuração ou pedido. Não pode ser excluído.");
            s.products.removeIf(p->p.id.equals(id));
        }
        else if("boxes".equals(type)) {
            s.boxes.removeIf(b->b.id.equals(id));
            invalidate(s);
        }
        else if("templates".equals(type)) s.templates.removeIf(c->c.id.equals(id));
        else throw new IllegalArgumentException("Tipo de exclusão inválido.");
        audit(s,"EXCLUSÃO",id,"",type);
        store.save(s);
        return s;
    }
    private boolean uses(Configuration c,String id) {
        return c.preparedProductId.equals(id)||c.lines.stream().anyMatch(l->l.productId.equals(id));
    }
    @PutMapping("/orders/{id}")
public synchronized State order(@PathVariable String id,@RequestBody Order o,@RequestHeader("X-State-Revision") long revision) {
        State s=current(revision);
        require(id.equals(o.id),"ID divergente.");
        Validation.order(o,s);
        Order previous=s.orders.stream().filter(x->x.id.equals(id)).findFirst().orElse(null);
        require(previous==null || !"DONE".equals(previous.status),"Pedido concluído mantém seu registro histórico.");
        require(!"DONE".equals(o.status),"Use a ação de concluir após a conferência.");
        o.checked=new HashMap<>();
        o.physicallyValidated=false;
        o.completedAt=null;
        o.completionSnapshot=null;
        o.createdAt=previous==null?Instant.now().toString():previous.createdAt;
        s.orders.removeIf(x->x.id.equals(id));
        s.orders.add(0,o);
        Validation.state(s);
        audit(s,"PEDIDO",o.reference,o.operator,"Composição final salva; conferência reiniciada.");
        store.save(s);
        return s;
    }
    private Order find(State s,String id) {
        return s.orders.stream().filter(o->o.id.equals(id)).findFirst().orElseThrow(()->new IllegalArgumentException("Pedido não encontrado."));
    }
    @GetMapping("/orders/{id}/plan")
public synchronized Object plan(@PathVariable String id) throws Exception {
        State s=store.read();
        Order o=find(s,id);
        if("DONE".equals(o.status) && o.completionSnapshot!=null) return store.mapper.readTree(o.completionSnapshot).get("plan");
        return engine.plan(s,o);
    }
    public static record Check(String key,String code,boolean manual) {
    }
    public static record Expected(String key,String sku,String barcode,String name,int quantity) {
    }
    public static List<Expected> expected(State s,Order o) {
        List<Expected> result=new ArrayList<>();
        Map<String,Product> map=new HashMap<>();
        s.products.forEach(p->map.put(p.id,p));
        for(Configuration c:o.configurations) {
            Product p=map.get(c.preparedProductId);
            if(p!=null)result.add(new Expected(c.id+":prepared",p.sku,p.barcode,"PC preparado · "+c.name,1));
            for(Line l:c.lines) {
                p=map.get(l.productId);
                if(p!=null)result.add(new Expected(l.id,p.sku,p.barcode,p.name,l.quantity));
            }
        }
        for(Line l:o.standalone) {
            Product p=map.get(l.productId);
            if(p!=null)result.add(new Expected(l.id,p.sku,p.barcode,p.name,l.quantity));
        }
        return result;
    }
    @GetMapping("/orders/{id}/checklist")
public synchronized List<Expected> checklist(@PathVariable String id) {
        State s=store.read();
        return expected(s,find(s,id));
    }
    @PostMapping("/orders/{id}/check")
public synchronized State check(@PathVariable String id,@RequestBody Check request,@RequestHeader("X-State-Revision") long revision) {
        State s=current(revision);
        Order o=find(s,id);
        require(!"DONE".equals(o.status),"Pedido concluído.");
        require(!o.operator.isBlank(),"Informe o operador no pedido antes de conferir.");
        Expected item=expected(s,o).stream().filter(e->e.key.equals(request.key)).findFirst().orElseThrow(()->new IllegalArgumentException("Item de conferência não encontrado."));
        require(request.manual || item.sku.equalsIgnoreCase(request.code) || (!item.barcode.isBlank() && item.barcode.equals(request.code)),"Código não corresponde ao item.");
        int n=o.checked.getOrDefault(item.key,0);
        require(n<item.quantity,"Quantidade já conferida.");
        o.checked.put(item.key,n+1);
        o.status="PACKING";
        audit(s,"CONFERÊNCIA",o.reference,o.operator,item.sku+" · "+(n+1)+"/"+item.quantity+(request.manual?" · manual":" · código"));
        store.save(s);
        return s;
    }
    public static record Complete(boolean physicallyValidated,String note) {
    }
    @PostMapping("/orders/{id}/complete")
public synchronized State complete(@PathVariable String id,@RequestBody Complete request,@RequestHeader("X-State-Revision") long revision) {
        State s=current(revision);
        Order o=find(s,id);
        require(!"DONE".equals(o.status),"Pedido já concluído.");
        Plan plan=engine.plan(s,o);
        require(plan.ready,"Medidas pendentes, demonstração ou problemas de encaixe impedem a conclusão.");
        require(!o.operator.isBlank(),"Informe o operador.");
        require(request.physicallyValidated,"Confirme a validação física da composição, proteção e acondicionamento interno.");
        require(expected(s,o).stream().allMatch(e->o.checked.getOrDefault(e.key,0)==e.quantity),"Conclua a conferência de todos os itens.");
        o.status="DONE";
        o.physicallyValidated=true;
        o.completedAt=Instant.now().toString();
        o.completionSnapshot=store.json(Map.of("plan",plan,"order",store.mapper.valueToTree(o),"products",s.products));
        audit(s,"CONCLUSÃO",o.reference,o.operator,request.note==null?"":request.note);
        store.save(s);
        return s;
    }
    public static record Note(String text) {
    }
    @PostMapping("/orders/{id}/notes")
public synchronized State note(@PathVariable String id,@RequestBody Note request,@RequestHeader("X-State-Revision") long revision) {
        State s=current(revision);
        Order o=find(s,id);
        Validation.text(request.text,"Motivo/observação");
        audit(s,"OCORRÊNCIA",o.reference,o.operator,request.text);
        store.save(s);
        return s;
    }
    @PostMapping("/demo")
public synchronized State demo(@RequestHeader("X-State-Revision") long revision) {
        State s=current(revision);
        require(s.products.isEmpty() && s.orders.isEmpty() && s.templates.isEmpty() && s.boxes.isEmpty(),"A demonstração só pode ser carregada em uma base vazia.");
        State demo=Demo.create();
        demo.generation=s.generation;
        audit(demo,"DEMONSTRAÇÃO","Dados fictícios","","Nenhuma dimensão representa produtos reais da Pichau.");
        store.save(demo);
        return demo;
    }
    @ExceptionHandler(IllegalArgumentException.class)
public ResponseEntity<Map<String,String>> invalid(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("message",Objects.toString(e.getMessage(),"Dados inválidos.")));
    }
    @ExceptionHandler(ResponseStatusException.class)
public ResponseEntity<Map<String,String>> conflict(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("message",Objects.toString(e.getReason(),"Atualize os dados.")));
    }
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
public ResponseEntity<Map<String,String>> unreadable() {
        return ResponseEntity.badRequest().body(Map.of("message","Formato inválido. Confira os campos e use números inteiros para medidas e quantidades."));
    }
    @ExceptionHandler(NullPointerException.class)
public ResponseEntity<Map<String,String>> incomplete() {
        return ResponseEntity.badRequest().body(Map.of("message","Dados incompletos. Revise os campos obrigatórios e o formato do arquivo."));
    }
}
