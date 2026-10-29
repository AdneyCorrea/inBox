package br.com.inbox;

import java.util.*;
import static br.com.inbox.Model.*;

/**
 * Tenta acomodar as embalagens, em milímetros, começando pelas maiores.
 * É uma heurística: encontrar um encaixe é uma sugestão válida, mas não encontrar
 * não prova que seria impossível organizar os itens de outra maneira.
 */
public class PackingEngine {
    // Reservamos 15% do volume para bolha. Isso não exige uma margem em cada lateral.
    public static final double MAX_OCCUPANCY = .85;
    public Plan plan(State state, Order order) {
        Map<String,Product> products = new HashMap<>();
        state.products.forEach(p -> products.put(p.id,p));
        Plan plan = new Plan();
        for (Configuration config: order.configurations) {
            Group main = new Group(config.id,"PC · " + config.name,"PC_MAIN");
            plan.groups.add(main);
            Product prepared = products.get(config.preparedProductId);
            if (prepared==null) main.issues.add("Cadastre o PC preparado para esta composição final.");
            else addObjects(main,prepared,1,config.id+":prepared");
            for (Line line: config.lines) route(plan, main, line, products, config);
        }
        if (!order.standalone.isEmpty()) {
            Group shared = new Group("standalone","Avulsos agrupados","STANDALONE_SHARED");
            for (Line line: order.standalone) route(plan,shared,line,products,null);
            if (!shared.objects.isEmpty() || !shared.issues.isEmpty()) plan.groups.add(shared);
        }
        for (Group group: plan.groups) {
            for (ObjectItem object: group.objects) {
                Pack p=object.pack;
                if (!measured(p) || "PENDING".equals(p.status)) group.issues.add(object.sku+" · medidas/peso pendentes: "+p.label);
                group.demo |= object.demo;
            }
            if (group.objects.isEmpty() && group.issues.isEmpty()) group.issues.add("Grupo sem objetos físicos. Revise a preparação.");
            if ("FACTORY_DIRECT".equals(group.kind)) {
                group.ownVolumes=group.objects.stream().mapToInt(o->o.quantity).sum();
                plan.knownFactoryVolumes += group.ownVolumes;
            }
            else if (group.issues.isEmpty()) {
                if(group.objects.stream().mapToLong(o->o.quantity).sum()>100) {
                    group.issues.add("Limite do protótipo: até 100 objetos físicos por caixa. Revisão necessária.");
                    continue;
                }
                group.candidate = choose(group.objects,state.boxes);
                if (group.candidate==null) group.issues.add("Nenhuma disposição encontrada nas caixas disponíveis. Revisão necessária; o grupo não foi dividido.");
                else {
                    group.demo |= "DEMO".equals(group.candidate.box.status);
                    plan.suggestedBoxes++;
                }
            }
            plan.demo |= group.demo;
        }
        plan.ready=!plan.groups.isEmpty() && !plan.demo && plan.groups.stream().allMatch(g->g.issues.isEmpty());
        return plan;
    }
    private void route(Plan plan,Group main,Line line,Map<String,Product> products,Configuration config) {
        Product p=products.get(line.productId);
        if(p==null) {
            main.issues.add("Produto não encontrado: "+line.productId);
            return;
        }
        boolean factory = Set.of("CHAIR","DESK","DESK_LEG").contains(p.category) || (config==null && Set.of("MONITOR","MONITOR_SUPPORT","UPS").contains(p.category));
        if(factory) {
            Group g=new Group(line.id,"Embalagem própria · "+p.name,"FACTORY_DIRECT");
            addObjects(g,p,line.quantity,line.id);
            plan.groups.add(g);
            return;
        }
        if(config!=null && "INSTALLED".equals(line.role)) {
            main.notes.add(p.sku+" × "+line.quantity+" instalado; peso incluído no PC preparado.");
            return;
        }
        if(config!=null && "NESTED".equals(line.role)) {
            boolean recipient=config.lines.stream().anyMatch(l -> {
                Product parent=products.get(l.productId); return parent!=null && "MOTHERBOARD".equals(parent.category) && "ACCOMPANYING".equals(l.role);
            });
            if(!recipient) main.issues.add(p.sku+" · caixa de placa-mãe de destino ausente.");
            main.notes.add(p.sku+" × "+line.quantity+" acondicionado na caixa da placa-mãe; conferir capacidade física.");
            return;
        }
        addObjects(main,p,line.quantity,line.id);
    }
    private void addObjects(Group group,Product product,int quantity,String key) {
        if(product.packages.isEmpty()) group.issues.add(product.sku+" · perfil de embalagem ausente.");
        for(int i=0;i<product.packages.size();i++) {
            Pack pack=product.packages.get(i);
            group.objects.add(new ObjectItem(key+":"+i,product,pack,quantity*pack.quantity));
        }
    }
    public static boolean measured(Pack p) {
        return p.l!=null && p.w!=null && p.h!=null && p.weight!=null;
    }
    public static boolean measured(Box b) {
        return b.l!=null && b.w!=null && b.h!=null && b.maxWeight!=null && b.tare!=null && b.outerL!=null && b.outerW!=null && b.outerH!=null;
    }
    public static long volume(int l,int w,int h) {
        return (long)l*w*h;
    }
    public Candidate choose(List<ObjectItem> objects,List<Box> boxes) {
        long total=objects.stream().mapToLong(o->volume(o.pack.l,o.pack.w,o.pack.h)*o.quantity).sum();
        long weight=objects.stream().mapToLong(o->(long)o.pack.weight*o.quantity).sum();
        List<Box> sorted=boxes.stream().filter(b->b.available && measured(b) && !"PENDING".equals(b.status)) .sorted(Comparator.comparingLong(b->volume(b.l,b.w,b.h))).toList();
        // A primeira caixa que passa no volume, peso e encaixe é a menor encontrada.
        for(Box box: sorted) {
            if(weight>box.maxWeight || total>volume(box.l,box.w,box.h)*MAX_OCCUPANCY) continue;
            List<Placement> placements=fit(objects,box);
            if(placements!=null) {
                Candidate c=new Candidate();
                c.box=box;
                c.placements=placements;
                c.occupancy=(double)total/volume(box.l,box.w,box.h);
                c.shippedWeight=weight+box.tare;
                return c;
            }
        }
        return null;
    }
    public List<Placement> fit(List<ObjectItem> objects,Box box) {
        List<ObjectItem> units=new ArrayList<>();
        for(ObjectItem o:objects) for(int i=0;i<o.quantity;i++) {
            if(units.size()>=100) return null;
            units.add(o);
        }
        units.sort(Comparator.comparingLong((ObjectItem o)->volume(o.pack.l,o.pack.w,o.pack.h)).reversed());
        // Unidades do mesmo produto compartilham o perfil. Calculamos suas rotações
        // só uma vez nesta tentativa, em vez de recriar seis arrays a cada posição.
        Map<Pack,List<int[]>> orientations=new IdentityHashMap<>();
        List<Placement> placed=new ArrayList<>();
        List<int[]> points=new ArrayList<>();
        points.add(new int[]{
            0,0,0
        });
        for(ObjectItem unit:units) {
            Placement best=null;
            List<int[]> unitRotations=orientations.computeIfAbsent(unit.pack,this::rotations);
            points.sort(Comparator.<int[]>comparingInt(p->p[2]).thenComparingInt(p->p[1]).thenComparingInt(p->p[0]));
            outer: for(int[] point:points) for(int[] dims:unitRotations) {
                Placement next=new Placement(unit,point[0],point[1],point[2],dims[0],dims[1],dims[2]);
                if((long)next.x+next.l>box.l || (long)next.y+next.w>box.w || (long)next.z+next.h>box.h) continue;
                if(placed.stream().anyMatch(p->overlap(p,next))) continue;
                // Um item elevado precisa de apoio completo em uma embalagem que
                // permita empilhar. Não sugerimos caixas flutuando ou apoiadas só na borda.
                boolean supported=next.z==0 || placed.stream().anyMatch(p->p.stackable && p.z+p.h==next.z && next.x>=p.x && next.y>=p.y && next.x+next.l<=p.x+p.l && next.y+next.w<=p.y+p.w);
                if(!supported) continue;
                best=next;
                break outer;
            }
            if(best==null) return null;
            placed.add(best);
            // Pontos dentro do item recém-colocado nunca mais poderão receber outro.
            // Mantemos as faces superiores/laterais: elas podem servir para empilhar.
            Placement occupied=best;
            points.removeIf(p->p[0]>=occupied.x && p[0]<occupied.x+occupied.l
                && p[1]>=occupied.y && p[1]<occupied.y+occupied.w
                && p[2]>=occupied.z && p[2]<occupied.z+occupied.h);
            int[][] newPoints={
                {
                    best.x+best.l,best.y,best.z
                },{
                    best.x,best.y+best.w,best.z
                },{
                    best.x,best.y,best.z+best.h
                }
            };
            for(int[] point:newPoints) if(points.stream().noneMatch(p->Arrays.equals(p,point))) points.add(point);
        }
        return placed;
    }
    public static boolean overlap(Placement a,Placement b) {
        return a.x<b.x+b.l && a.x+a.l>b.x && a.y<b.y+b.w && a.y+a.w>b.y && a.z<b.z+b.h && a.z+a.h>b.z;
    }
    private List<int[]> rotations(Pack p) {
        if(!p.rotatable) return List.of(new int[]{
            p.l,p.w,p.h
        });
        List<int[]> candidates=List.of(new int[]{
            p.l,p.w,p.h
        },new int[]{
            p.w,p.l,p.h
        },new int[]{
            p.l,p.h,p.w
        },new int[]{
            p.h,p.l,p.w
        },new int[]{
            p.w,p.h,p.l
        },new int[]{
            p.h,p.w,p.l
        });
        // Cubos têm uma orientação útil; peças com dois lados iguais têm três.
        // Preservamos a ordem original para que a sugestão continue previsível.
        List<int[]> unique=new ArrayList<>();
        for(int[] candidate:candidates)
            if(unique.stream().noneMatch(d->Arrays.equals(d,candidate)))unique.add(candidate);
        return unique;
    }
}
