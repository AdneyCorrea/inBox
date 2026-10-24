package br.com.inbox;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CatalogTopicsTest {
 @Test void topicDefinesPackingSemantics(){var p=new Model.DimensionProduct();p.topic="monitors/support";p.category="OTHER";CatalogTopics.normalize(p);assertEquals("MONITOR_SUPPORT",p.category);assertEquals("FACTORY",PcApi.defaultMode(p,"EXTRA"));p.topic="peripherals/mouse";CatalogTopics.normalize(p);assertEquals("OTHER",p.category);assertEquals("MAIN",PcApi.defaultMode(p,"EXTRA"));}
 @Test void invalidTopicIsRejectedAndLegacyDataRetainsCategory(){var p=new Model.DimensionProduct();p.category="MEMORY";CatalogTopics.normalize(p);assertEquals("MEMORY",p.category);p.topic="invalid";assertThrows(IllegalArgumentException.class,()->CatalogTopics.normalize(p));}
 @Test void allDepartmentsHaveUniqueTopics(){assertEquals(15,CatalogTopics.DEPARTMENTS.size());var topics=CatalogTopics.DEPARTMENTS.stream().flatMap(d->d.topics().stream()).toList();assertEquals(95,topics.size());assertEquals(topics.size(),topics.stream().map(CatalogTopics.Topic::id).distinct().count());assertTrue(topics.stream().allMatch(t->PcApi.CATEGORIES.contains(t.category())));}
}
