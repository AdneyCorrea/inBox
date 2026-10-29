// Prepara configurações de PC de exemplo com códigos e medidas fictícios.
package br.com.inbox;

import java.util.*;
import static br.com.inbox.Model.*;

/** Public configuration descriptions; component SKUs and ALL dimensions below are synthetic. */
/** Configuracoes de exemplo; suas dimensoes nao devem ser usadas como medidas reais. */
public class PcExamples {
    /** User-facing seed contains only the requested Highflyer, never unrelated orders. */
    public static void addHighflyer(State s){
        State seed=new State();
        add(seed);
        PcPreset highflyer=seed.dimensionPcs.stream().filter(pc->pc.code.equals(HF)).findFirst().orElseThrow();
        highflyer.items.add(line(product(seed,"hf-power-cable","Cabo de alimentação","CABLE","MAIN",180,120,40)));
        highflyer.items.add(line(product(seed,"hf-hdmi","Cabo HDMI","CABLE","MAIN",180,120,40)));
        for(DimensionProduct p:seed.dimensionProducts)if(p.id.startsWith("pichau-example-hf-")&&s.dimensionProducts.stream().noneMatch(x->x.id.equals(p.id)))s.dimensionProducts.add(p);
        for(PcPreset pc:seed.dimensionPcs)if(pc.code.equals(HF)&&s.dimensionPcs.stream().noneMatch(x->x.code.equalsIgnoreCase(HF)))s.dimensionPcs.add(pc);
        for(DimensionBox b:seed.dimensionBoxes)if(s.dimensionBoxes.stream().noneMatch(x->x.id.equals(b.id)))s.dimensionBoxes.add(b);
        int[][] extraSizes={
            {
                200,150,100
            },{
                300,250,150
            },{
                400,300,200
            },{
                500,400,300
            },{
                600,450,400
            },{
                800,700,700
            },{
                1300,1000,1000
            }
        };
        for(int[] size:extraSizes){
            String id="demo-box-"+size[0]+"-"+size[1]+"-"+size[2];
            if(s.dimensionBoxes.stream().anyMatch(b->b.id.equals(id)))continue;
            DimensionBox b=new DimensionBox();
            b.id=id;
            b.name="Caixa "+size[0]/10+" × "+size[1]/10+" × "+size[2]/10+" cm · exemplo";
            b.l=size[0];
            b.w=size[1];
            b.h=size[2];
            b.demo=true;
            s.dimensionBoxes.add(b);
        }
    }
    public static final String HF="PCM-Pichau-Highflyer-66705", GAMER="PCM-Pichau-Gamer-68475";
    private static DimensionProduct product(State s,String key,String name,String category,String mode,int l,int w,int h){
        String id="pichau-example-"+key;
        DimensionProduct existing=s.dimensionProducts.stream().filter(p->p.id.equals(id)).findFirst().orElse(null);
        if(existing!=null)return existing;
        DimensionProduct p=new DimensionProduct();
        p.id=id;
        p.sku="DEMO-"+key.toUpperCase(Locale.ROOT);
        p.name=name;
        p.category=category;
        p.pcMode=mode;
        p.l=l;
        p.w=w;
        p.h=h;
        p.demo=true;
        s.dimensionProducts.add(p);
        return p;
    }
    private static PcItem line(DimensionProduct p){
        PcItem i=new PcItem();
        i.productId=p.id;
        i.mode=p.pcMode;
        return i;
    }
    private static PcPreset pc(State s,String code,String name,String source,String alias,List<DimensionProduct> products){
        PcPreset existing=s.dimensionPcs.stream().filter(p->p.code.equalsIgnoreCase(code)).findFirst().orElse(null);
        if(existing!=null)return existing;
        PcPreset pc=new PcPreset();
        pc.id="example-"+alias;
        pc.code=code;
        pc.name=name;
        pc.sourceUrl=source;
        pc.demo=true;
        pc.aliases.add(alias);
        pc.note="Composição de demonstração baseada na descrição pública. SKUs das peças e todas as medidas são fictícios. Marcas/modelos e embalagens acompanhantes precisam ser confirmados na operação.";
        products.forEach(p->pc.items.add(line(p)));
        s.dimensionPcs.add(pc);
        return pc;
    }
    public static void add(State s){
        DimensionProduct hfPc=product(s,"hf-pc","Pichau - Voyager One","PREPARED_PC","MAIN",650,330,650);
        DimensionProduct hfCpu=product(s,"hf-cpu","AMD - Ryzen 7 9850X3D","CPU","MAIN",145,135,80);
        DimensionProduct hfMb=product(s,"hf-mb","Placa-mãe X870","MOTHERBOARD","MAIN",360,300,90);
        DimensionProduct hfGpu=product(s,"hf-gpu","GeForce RTX 5070 Ti 16 GB","GPU","MAIN",420,250,120);
        DimensionProduct hfRam=product(s,"hf-ram","Memória 32 GB DDR5","MEMORY","NESTED",170,130,25);
        DimensionProduct hfSsd=product(s,"hf-ssd","SSD M.2 NVMe 2 TB","SSD","NESTED",120,90,20);
        DimensionProduct hfPsu=product(s,"hf-psu","Fonte 850 W","PSU","MAIN",280,210,130);
        DimensionProduct hfCooler=product(s,"hf-cooler","Water Cooler Aqua 360X P1 Black Edition","COOLER","MAIN",460,220,160);
        PcPreset hf=pc(s,HF,"Highflyer · Ryzen 7 / RTX 5070 Ti","https://www.pichau.com.br/pc-gamer-pichau-highflyer-amd-ryzen-7-9850x3d-geforce-rtx-5070-ti-16gb-32gb-ddr5-ssd-m-2-2tb","66705",List.of(hfPc,hfCpu,hfMb,hfGpu,hfRam,hfSsd,hfPsu,hfCooler));
        DimensionProduct gamerPc=product(s,"gamer-pc","Gabinete · Apus RGB","PREPARED_PC","MAIN",500,270,470);
        DimensionProduct gamerCpu=product(s,"gamer-cpu","Ryzen 5 5500","CPU","MAIN",135,130,70);
        DimensionProduct gamerMb=product(s,"gamer-mb","Placa-mãe A520M","MOTHERBOARD","MAIN",300,250,80);
        DimensionProduct gamerGpu=product(s,"gamer-gpu","Radeon RX 9050 8 GB","GPU","MAIN",340,220,90);
        DimensionProduct gamerRam=product(s,"gamer-ram","Memória 16 GB DDR4 (total)","MEMORY","NESTED",170,130,25);
        DimensionProduct gamerSsd=product(s,"gamer-ssd","SSD 480 GB","SSD","NESTED",120,90,20);
        DimensionProduct gamerPsu=product(s,"gamer-psu","Fonte 500 W","PSU","MAIN",240,180,100);
        PcPreset gamer=pc(s,GAMER,"Gamer · Ryzen 5 / Radeon RX 9050","https://www.pichau.com.br/pc-gamer-pichau-amd-ryzen-5-5500-radeon-rx-9050-8gb-16gb-ddr4-ssd-480gb-68475","68475",List.of(gamerPc,gamerCpu,gamerMb,gamerGpu,gamerRam,gamerSsd,gamerPsu));
        DimensionProduct upgradeGpu=product(s,"gpu-upgrade","Placa de vídeo maior · alternativa fictícia","GPU","MAIN",500,300,150);
        DimensionProduct fan=product(s,"fan","Ventoinha adicional","FAN","MAIN",140,140,40);
        DimensionProduct ram32=product(s,"ram-upgrade","Memória 32 GB DDR4 · alternativa fictícia","MEMORY","NESTED",180,140,30);
        DimensionProduct monitor=product(s,"monitor","Monitor · exemplo de item adicional","MONITOR","MAIN",600,180,420);
        DimensionProduct headset=product(s,"headset","Headset · exemplo avulso","PERIPHERAL","MAIN",240,220,120);
        DimensionProduct mouse=product(s,"mouse","Mouse · exemplo avulso","PERIPHERAL","MAIN",160,100,70);
        addCustom(s,hf,"DEMO-PEDIDO-66705",hfGpu,upgradeGpu,fan,monitor,null,null);
        addCustom(s,gamer,"DEMO-PEDIDO-68475",gamerRam,ram32,null,monitor,headset,mouse);
        int[][] sizes={
            {
                700,600,600
            },{
                950,750,750
            },{
                1100,850,850
            }
        };
        for(int i=0;i<sizes.length;i++){
            String id="pc-example-box-"+i;
            if(s.dimensionBoxes.stream().noneMatch(b->b.id.equals(id))){
                DimensionBox b=new DimensionBox();
                b.id=id;
                b.name="PC "+(i+1)+" · exemplo";
                b.l=sizes[i][0];
                b.w=sizes[i][1];
                b.h=sizes[i][2];
                b.demo=true;
                s.dimensionBoxes.add(b);
            }
        }
    }
    private static void addCustom(State s,PcPreset base,String code,DimensionProduct original,DimensionProduct replacement,DimensionProduct fan,DimensionProduct monitor,DimensionProduct headset,DimensionProduct mouse){
        if(s.dimensionPcs.stream().anyMatch(p->p.code.equals(code)))return;
        PcPreset order=new PcPreset();
        order.id="example-"+code;
        order.kind="ORDER";
        order.code=code;
        order.baseCode=base.code;
        order.name=base.name+" · personalizado";
        order.sourceUrl=base.sourceUrl;
        order.demo=true;
        order.note="Pedido fictício para testar substituições e adicionais; não é uma compra real nem representa as opções atuais do site.";
        for(PcItem item:base.items){
            DimensionProduct product=s.dimensionProducts.stream().filter(p->p.id.equals(item.productId)).findFirst().orElseThrow();
            PcItem clone=line(product);
            if(product.id.equals(original.id)){
                clone=line(replacement);
                clone.customized=true;
            }
            order.items.add(clone);
        }
        if(fan!=null){
            PcItem extra=line(fan);
            extra.quantity=3;
            extra.customized=true;
            order.items.add(extra);
        }
        PcItem screen=line(monitor);
        screen.origin=headset==null?"CONFIG":"EXTRA";
        screen.customized=true;
        order.items.add(screen);
        if(headset!=null){
            PcItem extra=line(headset);
            extra.origin="EXTRA";
            order.items.add(extra);
            extra=line(mouse);
            extra.origin="EXTRA";
            order.items.add(extra);
        }
        s.dimensionPcs.add(order);
    }
}
