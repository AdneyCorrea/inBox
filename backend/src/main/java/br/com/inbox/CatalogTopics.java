package br.com.inbox;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import static br.com.inbox.Validation.require;
public final class CatalogTopics {
 public record Topic(String id,String label,String category){}
 public record Department(String id,String label,List<Topic> topics){}
 public static final List<Department> DEPARTMENTS;
 static {try(var in=CatalogTopics.class.getResourceAsStream("/static/catalog-categories.json")){DEPARTMENTS=List.of(new ObjectMapper().readValue(in,Department[].class));}catch(Exception e){throw new ExceptionInInitializerError(e);}}
 public static void normalize(Model.DimensionProduct p){
  require(p.topic!=null,"Categoria ausente.");
  if(p.topic.isBlank()){
   p.topic=switch(p.category){case "OTHER"->"other/other";case "CABLE"->"peripherals/cables";default->DEPARTMENTS.stream().flatMap(d->d.topics().stream()).filter(t->t.category().equals(p.category)).map(Topic::id).findFirst().orElse("other/other");};
  }
  Topic topic=DEPARTMENTS.stream().flatMap(d->d.topics().stream()).filter(t->t.id().equals(p.topic)).findFirst().orElseThrow(()->new IllegalArgumentException("Categoria de catálogo inválida."));
  p.category=topic.category();
 }
}
