package br.com.inbox;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static br.com.inbox.Model.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:sitetest;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc
class SiteApiTest {
    @Autowired MockMvc mvc;
    @Autowired Store store;
    @MockitoBean PichauSite site;
    @BeforeEach void setup(){State s=new State();s.generation=store.read().generation;PcExamples.add(s);store.save(s);}
    @Test void liveProductIsMatchedToMeasuredEnvelopes() throws Exception {
        when(site.lookup(any(),eq("68475"))).thenReturn(new PichauSite.ProductPage(PcExamples.GAMER,"Ryzen 5 5500 RX 9050 16GB DDR4 SSD 480GB","https://www.pichau.com.br/exemplo","",List.of("Gabinete APUS","Placa-mãe A520M","Fonte 500W")));
        mvc.perform(post("/api/simple/site/lookup").contentType("application/json").content("{\"query\":\"68475\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("MATCHED")).andExpect(jsonPath("$.plan.groups[0].result.status").value("FOUND"));verify(site).lookup(any(),eq("68475"));
    }
    @Test void outageDoesNotSilentlyUseStoredComposition() throws Exception {when(site.lookup(any(),anyString())).thenThrow(new IOException("Pichau indisponível (HTTP 503)"));mvc.perform(post("/api/simple/site/lookup").contentType("application/json").content("{\"query\":\"68475\"}")).andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.plan").doesNotExist());}
    @Test void unknownProductIsPendingRatherThanInvented() throws Exception {when(site.lookup(any(),anyString())).thenReturn(new PichauSite.ProductPage("PCM-NOVO-99999","PC novo","https://www.pichau.com.br/novo","",List.of()));mvc.perform(post("/api/simple/site/lookup").contentType("application/json").content("{\"query\":\"99999\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDING")).andExpect(jsonPath("$.plan").isEmpty());}
    @Test void aChangedConfigurationIsBlocked() throws Exception {when(site.lookup(any(),anyString())).thenReturn(new PichauSite.ProductPage(PcExamples.GAMER,"Outro PC","https://www.pichau.com.br/exemplo","",List.of()));mvc.perform(post("/api/simple/site/lookup").contentType("application/json").content("{\"query\":\"68475\"}")).andExpect(jsonPath("$.status").value("PENDING")).andExpect(jsonPath("$.plan").isEmpty());}
}
