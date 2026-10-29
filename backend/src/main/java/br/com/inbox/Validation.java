package br.com.inbox;

import java.util.*;
import static br.com.inbox.Model.*;

/** Validacao de dados atuais e de backups do prototipo, sem migracoes destrutivas. */
public class Validation {
    public static void require(boolean condition,String message) {
        if(!condition) throw new IllegalArgumentException(message);
    }
    public static void text(String value,String name) {
        require(value!=null && !value.isBlank() && value.length()<=300,name+" é obrigatório (até 300 caracteres).");
    }
    private static void dimension(Integer x) {
        require(x==null || x>0 && x<=100000,"Medidas devem ser inteiras entre 1 e 100.000 mm, ou vazias.");
    }
    private static void weight(Integer x) {
        require(x==null || x>=0 && x<=100000000,"Peso inválido; use gramas ou deixe vazio.");
    }
    private static void status(String s) {
        require(Set.of("PENDING","DEMO","VALIDATED").contains(s),"Situação de medidas inválida.");
    }
    private static void provenance(String source,String by,String at) {
        text(source,"Origem da medição");
        text(by,"Responsável pela medição");
        text(at,"Data da medição");
        try {
            java.time.LocalDate.parse(at);
        }
        catch(java.time.format.DateTimeParseException e) {
            throw new IllegalArgumentException("Data de medição inválida.");
        }
    }
    private static void unique(List<String> ids,String name) {
        require(ids.stream().noneMatch(Objects::isNull) && new HashSet<>(ids).size()==ids.size(),"Identificadores duplicados em "+name);
    }
    public static void product(Product p) {
        text(p.id,"ID");
        text(p.sku,"SKU");
        text(p.name,"Nome");
        require(Set.of("PC_PREPARED","CPU","MOTHERBOARD","MEMORY","SSD","FAN","COOLER","GPU","PSU","CASE","PERIPHERAL","MONITOR","MONITOR_SUPPORT","UPS","CHAIR","DESK","DESK_LEG","OTHER").contains(p.category),"Categoria inválida.");
        role(p.role);
        require(p.revision>0,"Revisão inválida.");
        require(p.packages!=null && p.packages.size()<=20,"Máximo de 20 perfis por produto.");
        for(Pack pkg:p.packages) {
            text(pkg.label,"Nome do pacote");
            dimension(pkg.l);
            dimension(pkg.w);
            dimension(pkg.h);
            weight(pkg.weight);
            status(pkg.status);
            require(pkg.quantity>0 && pkg.quantity<=100,"Quantidade de pacotes inválida.");
            if("VALIDATED".equals(pkg.status)) {
                require(PackingEngine.measured(pkg),"Preencha medidas e peso antes de validar.");
                provenance(pkg.source,pkg.measuredBy,pkg.measuredAt);
            }
        }
    }
    public static void box(Box b) {
        text(b.id,"ID");
        text(b.code,"Código da caixa");
        require(b.revision>0,"Revisão inválida.");
        status(b.status);
        for(Integer d:Arrays.asList(b.l,b.w,b.h,b.outerL,b.outerW,b.outerH)) dimension(d);
        weight(b.tare);
        weight(b.maxWeight);
        require(b.maxWeight==null || b.maxWeight>0,"Capacidade de carga deve ser positiva.");
        require(b.l==null || b.outerL==null || b.outerL>=b.l,"Comprimento externo menor que o interno.");
        require(b.w==null || b.outerW==null || b.outerW>=b.w,"Largura externa menor que a interna.");
        require(b.h==null || b.outerH==null || b.outerH>=b.h,"Altura externa menor que a interna.");
        require(!"VALIDATED".equals(b.status) || PackingEngine.measured(b),"Preencha medidas internas, externas, tara e capacidade antes de validar.");
        if("VALIDATED".equals(b.status))provenance(b.source,b.measuredBy,b.measuredAt);
    }
    public static void role(String role) {
        require(Set.of("PACKAGED","ACCOMPANYING","NESTED","INSTALLED").contains(role),"Tratamento físico inválido.");
    }
    public static void configuration(Configuration c,State state) {
        text(c.id,"ID da configuração");
        text(c.sku,"SKU comercial");
        text(c.name,"Nome da configuração");
        require(c.preparedProductId!=null,"PC preparado inválido.");
        require(c.preparedProductId.isBlank() || state.products.stream().anyMatch(p->p.id.equals(c.preparedProductId) && "PC_PREPARED".equals(p.category)),"Selecione um cadastro do tipo PC preparado.");
        lines(c.lines,state,true);
    }
    public static void lines(List<Line> lines,State state,boolean configuration) {
        if (lines == null) throw new IllegalArgumentException("Informe as linhas do grupo.");
        require(lines.size()<=100,"Máximo de 100 linhas por grupo.");
        unique(lines.stream().map(l->l.id).toList(),"linhas");
        for(Line l:lines) {
            text(l.id,"ID da linha");
            role(l.role);
            require(l.quantity>0 && l.quantity<=100,"Quantidade deve ser de 1 a 100.");
            require(state.products.stream().anyMatch(p->p.id.equals(l.productId) && !"PC_PREPARED".equals(p.category)),"Produto da linha não encontrado ou reservado ao PC preparado.");
            require(configuration ? Set.of("BASE","CUSTOMIZATION").contains(l.origin) : "STANDALONE".equals(l.origin),"Origem incompatível com o grupo.");
            require(configuration || "PACKAGED".equals(l.role),"Avulsos devem usar o produto embalado.");
        }
    }
    public static void order(Order o,State state) {
        text(o.id,"ID do pedido");
        text(o.reference,"Referência do pedido");
        require(o.configurations!=null && o.configurations.size()<=20,"Máximo de 20 PCs por pedido.");
        unique(o.configurations.stream().map(c->c.id).toList(),"PCs");
        for(Configuration c:o.configurations) configuration(c,state);
        lines(o.standalone,state,false);
        require(!o.configurations.isEmpty() || !o.standalone.isEmpty(),"Adicione um PC ou item avulso.");
        List<String> all=new ArrayList<>();
        for(Configuration c:o.configurations) {
            all.add(c.id);
            all.addAll(c.lines.stream().map(l->l.id).toList());
        }
        all.addAll(o.standalone.stream().map(l->l.id).toList());
        unique(all,"identificadores do pedido");
        require(Set.of("WAITING","REVIEW","PACKING","DONE").contains(o.status),"Etapa inválida.");
        require(o.operator!=null && o.operator.length()<=150 && o.note!=null && o.note.length()<=4000,"Operador ou observação inválidos.");
    }
    public static void state(State s) {
        DimensionsApi.validate(s);
        require(s.version==1,"Versão de arquivo não suportada.");
        require(s.products!=null && s.boxes!=null && s.templates!=null && s.orders!=null && s.history!=null,"Arquivo incompleto.");
        require(s.products.size()<=10000 && s.orders.size()<=10000,"Limite do protótipo excedido.");
        unique(s.products.stream().map(p->p.id).toList(),"produtos");
        unique(s.products.stream().map(p->p.sku.toUpperCase(Locale.ROOT)).toList(),"SKUs");
        unique(s.boxes.stream().map(b->b.id).toList(),"caixas");
        unique(s.boxes.stream().map(b->b.code.toUpperCase(Locale.ROOT)).toList(),"códigos de caixa");
        unique(s.orders.stream().map(o->o.id).toList(),"pedidos");
        unique(s.orders.stream().map(o->o.reference.toUpperCase(Locale.ROOT)).toList(),"referências de pedido");
        unique(s.templates.stream().map(c->c.id).toList(),"modelos");
        s.products.forEach(Validation::product);
        s.boxes.forEach(Validation::box);
        s.templates.forEach(c->configuration(c,s));
        s.orders.forEach(o->order(o,s));
    }
}
