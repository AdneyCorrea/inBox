package br.com.inbox;

import java.util.*;

/** Contratos persistidos. Campos antigos sao mantidos para ler e restaurar backups existentes. */
public final class Model {
    private Model() {
    }
    public static String id() {
        return UUID.randomUUID().toString();
    }
    public static class Pack {
        public String label = "Embalagem";
        public Integer l, w, h, weight;
        public int quantity = 1;
        public boolean rotatable = true, stackable = true;
        public String status = "PENDING", source = "", measuredBy = "", measuredAt = "";
    }
    public static class Product {
        public String id = Model.id(), sku = "", name = "", category = "OTHER", role = "PACKAGED", barcode = "";
        public boolean oem;
        public int revision = 1;
        public List<Pack> packages = new ArrayList<>();
    }
    public static class Box {
        public String id = Model.id(), code = "", status = "PENDING";
        public String source = "", measuredBy = "", measuredAt = "";
        public Integer l, w, h, outerL, outerW, outerH, tare, maxWeight;
        public boolean available = true;
        public int revision = 1;
    }
    public static class Line {
        public String id = Model.id(), productId = "", origin = "BASE", role = "PACKAGED";
        public int quantity = 1;
    }
    public static class Configuration {
        public String id = Model.id(), sku = "", name = "", preparedProductId = "";
        public List<Line> lines = new ArrayList<>();
    }
    public static class Order {
        public String id = Model.id(), reference = "", operator = "", status = "WAITING", note = "";
        public String createdAt = java.time.Instant.now().toString();
        public List<Configuration> configurations = new ArrayList<>();
        public List<Line> standalone = new ArrayList<>();
        public Map<String,Integer> checked = new HashMap<>();
        public boolean physicallyValidated;
        public String completedAt;
        public String completionSnapshot;
    }
    public static class Event {
        public String id = Model.id(), at = java.time.Instant.now().toString(), action, reference, operator, detail;
        public Event() {
        }
        public Event(String a,String r,String o,String d) {
            action=a;
            reference=r;
            operator=o;
            detail=d;
        }
    }
    public static class State {
        // Mantemos também os campos do protótipo anterior para ler backups antigos.
        public int version = 1;
        // Número da última gravação. A interface devolve esse valor ao salvar,
        // para o Store perceber se outra tela já alterou o cadastro.
        public long generation;
        public List<Product> products = new ArrayList<>();
        public List<Box> boxes = new ArrayList<>();
        public List<Configuration> templates = new ArrayList<>();
        public List<Order> orders = new ArrayList<>();
        public List<Event> history = new ArrayList<>();
        public List<DimensionProduct> dimensionProducts = new ArrayList<>();
        public List<DimensionBox> dimensionBoxes = new ArrayList<>();
        public List<PcPreset> dimensionPcs = new ArrayList<>();
    }
    public static class DimensionProduct {
        // A tela mostra centímetros, mas aqui e no banco trabalhamos em milímetros.
        // null significa "ainda não medido"; não é uma embalagem de tamanho zero.
        public String id = Model.id(), name = "", sku = "";
        public Integer l, w, h;
        public boolean demo;
        public boolean oem;
        public String category = "OTHER", pcMode = "MAIN", topic = "";
    }
    public static class DimensionBox {
        public String id = Model.id(), name = "";
        public Integer l, w, h;
        public boolean demo;
    }
    public static class PcItem {
        public String id=Model.id(), productId="", mode="MAIN", origin="CONFIG";
        public int quantity=1;
        public boolean customized;
    }
    public static class PcPreset {
        public String id=Model.id(), code="", name="", kind="BASE", baseCode="", sourceUrl="", note="";
        public boolean demo;
        public List<String> aliases=new ArrayList<>();
        public List<PcItem> items=new ArrayList<>();
    }
    public static class ObjectItem {
        public String key, sku, name;
        public Pack pack;
        public int quantity;
        public boolean demo;
        public ObjectItem(String k,Product p,Pack pkg,int q) {
            key=k;
            sku=p.sku;
            name=p.name;
            pack=pkg;
            quantity=q;
            demo="DEMO".equals(pkg.status);
        }
    }
    public static class Placement {
        // Posição do canto inferior: X = comprimento, Y = largura, Z = altura.
        // O 3D desenha exatamente estas posições; ele não decide como encaixar.
        public String key, name;
        public int x,y,z,l,w,h;
        public boolean stackable;
        public Placement(ObjectItem item,int x,int y,int z,int l,int w,int h) {
            key=item.key;
            name=item.name;
            this.x=x;
            this.y=y;
            this.z=z;
            this.l=l;
            this.w=w;
            this.h=h;
            stackable=item.pack.stackable;
        }
    }
    public static class Candidate {
        public Box box;
        public double occupancy;
        public long shippedWeight;
        public List<Placement> placements = new ArrayList<>();
    }
    public static class Group {
        public String id, label, kind;
        public List<ObjectItem> objects = new ArrayList<>();
        public List<String> notes = new ArrayList<>(), issues = new ArrayList<>();
        public Candidate candidate;
        public boolean demo;
        public int ownVolumes;
        public Group(String id,String label,String kind) {
            this.id=id;
            this.label=label;
            this.kind=kind;
        }
    }
    public static class Plan {
        public List<Group> groups = new ArrayList<>();
        public boolean ready, demo;
        public int knownFactoryVolumes, suggestedBoxes;
    }
}
