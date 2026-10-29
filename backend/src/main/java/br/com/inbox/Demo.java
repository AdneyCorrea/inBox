package br.com.inbox;

import java.util.*;
import static br.com.inbox.Model.*;

/** Dados ficticios usados apenas pelas rotas de demonstracao do prototipo. */
public class Demo {
    private static Product product(State s,String sku,String name,String cat,String role,int l,int w,int h,int weight) {
        Product p=new Product();
        p.sku=sku;
        p.name=name;
        p.category=cat;
        p.role=role;
        Pack pkg=new Pack();
        pkg.l=l;
        pkg.w=w;
        pkg.h=h;
        pkg.weight=weight;
        pkg.status="DEMO";
        pkg.source="Exemplo fictício";
        p.packages.add(pkg);
        s.products.add(p);
        return p;
    }
    private static Line line(Product p,String origin,int quantity) {
        Line l=new Line();
        l.productId=p.id;
        l.role=p.role;
        l.origin=origin;
        l.quantity=quantity;
        return l;
    }
    public static State create() {
        State s=new State();
        Product pc=product(s,"DEMO-PC-PREP","PC preparado · exemplo","PC_PREPARED","PACKAGED",470,250,450,7000);
        Product mb=product(s,"DEMO-MB","Caixa da placa-mãe + itens internos","MOTHERBOARD","ACCOMPANYING",310,250,70,500);
        Product cpu=product(s,"DEMO-CPU-OEM","Processador OEM instalado","CPU","INSTALLED",10,10,10,0);
        cpu.oem=true;
        cpu.packages.clear();
        Product ram=product(s,"DEMO-RAM","Memória · caixa dentro da placa-mãe","MEMORY","NESTED",150,80,15,0);
        Product ssd=product(s,"DEMO-SSD","SSD · caixa dentro da placa-mãe","SSD","NESTED",120,90,20,0);
        Product fan=product(s,"DEMO-FAN","Ventoinha adicional · caixa acompanhante","FAN","ACCOMPANYING",130,130,30,100);
        Product monitor=product(s,"DEMO-MON","Monitor com embalagem","MONITOR","PACKAGED",550,130,380,4000);
        monitor.packages.get(0).rotatable=false;
        monitor.packages.get(0).stackable=false;
        Product keyboard=product(s,"DEMO-TEC","Teclado com embalagem","PERIPHERAL","PACKAGED",450,160,50,600);
        Product mouse=product(s,"DEMO-MOUSE","Mouse com embalagem","PERIPHERAL","PACKAGED",130,90,60,150);
        Product headset=product(s,"DEMO-FONE","Headset com embalagem","PERIPHERAL","PACKAGED",220,200,100,400);
        Product chair=product(s,"DEMO-CADEIRA","Cadeira · embalagem do fabricante","CHAIR","PACKAGED",800,600,300,15000);
        Product desk=product(s,"DEMO-MESA","Mesa · dois pacotes de fábrica","DESK","PACKAGED",1200,650,70,12000);
        Pack legs=new Pack();
        legs.label="Estrutura";
        legs.l=750;
        legs.w=300;
        legs.h=120;
        legs.weight=8000;
        legs.status="DEMO";
        desk.packages.get(0).label="Tampo";
        desk.packages.add(legs);
        product(s,"DEMO-SUPORTE","Suporte de monitor","MONITOR_SUPPORT","PACKAGED",400,200,100,2500);
        product(s,"DEMO-NOBREAK","Nobreak","UPS","PACKAGED",400,250,300,8000);
        Product pending=product(s,"PENDENTE-EXEMPLO","Produto aguardando medição","OTHER","PACKAGED",1,1,1,0);
        Pack pp=pending.packages.get(0);
        pp.l=null;
        pp.w=null;
        pp.h=null;
        pp.weight=null;
        pp.status="PENDING";
        for(int i=0;i<3;i++) {
            Box b=new Box();
            b.code="DEMO-CX-0"+(i+1);
            b.l=new int[]{
                600,700,850
            }
            [i];
            b.w=new int[]{
                450,600,700
            }
            [i];
            b.h=new int[]{
                500,600,650
            }
            [i];
            b.outerL=b.l+10;
            b.outerW=b.w+10;
            b.outerH=b.h+10;
            b.tare=1000;
            b.maxWeight=30000;
            b.status="DEMO";
            s.boxes.add(b);
        }
        Configuration basic=new Configuration();
        basic.sku="DEMO-CONFIG-01";
        basic.name="PC + kit periféricos";
        basic.preparedProductId=pc.id;
        basic.lines.addAll(List.of(line(mb,"BASE",1),line(cpu,"BASE",1),line(ram,"BASE",1),line(ssd,"BASE",1),line(keyboard,"BASE",1),line(mouse,"BASE",1)));
        s.templates.add(basic);
        Configuration full=new Configuration();
        full.sku="DEMO-CONFIG-02";
        full.name="PC + monitor + cadeira";
        full.preparedProductId=pc.id;
        full.lines.addAll(List.of(line(mb,"BASE",1),line(cpu,"BASE",1),line(ram,"BASE",1),line(ssd,"BASE",1),line(monitor,"BASE",1),line(keyboard,"BASE",1),line(mouse,"BASE",1),line(chair,"BASE",1),line(fan,"CUSTOMIZATION",2)));
        s.templates.add(full);
        Order o=new Order();
        o.reference="DEMO-1001";
        o.operator="Operador demonstração";
        o.configurations.add(full);
        o.standalone.addAll(List.of(line(headset,"STANDALONE",1),line(mouse,"STANDALONE",1),line(monitor,"STANDALONE",1),line(desk,"STANDALONE",1)));
        o.note="Pedido fictício: monitor incluído e monitor avulso têm destinos distintos.";
        s.orders.add(o);
        Order pendingOrder=new Order();
        pendingOrder.reference="DEMO-1002";
        pendingOrder.status="REVIEW";
        pendingOrder.standalone.add(line(pending,"STANDALONE",1));
        s.orders.add(pendingOrder);
        Validation.state(s);
        return s;
    }
}
