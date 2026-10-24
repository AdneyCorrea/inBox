package br.com.inbox;

import org.junit.jupiter.api.Test;
import java.net.URI;
import static org.junit.jupiter.api.Assertions.*;
import static br.com.inbox.Model.*;

class PichauSiteTest {
    @Test void acceptsOnlyThePublicPichauHost(){for(String url:new String[]{"http://www.pichau.com.br/a","https://127.0.0.1/a","https://www.pichau.com.br.evil.test/a","https://user@www.pichau.com.br/a","https://www.pichau.com.br:8443/a","file:///etc/passwd"})assertThrows(IllegalArgumentException.class,()->PichauSite.allowed(url));assertEquals("www.pichau.com.br",PichauSite.allowed("https://www.pichau.com.br/produto").getHost());}
    @Test void extractsIdentityFromProductPage(){var page=PichauSite.parse("<h1>Computador de exemplo</h1><p>Sku: PCM-Exemplo-12345</p><h3>Placa-mãe A520M</h3>",URI.create("https://www.pichau.com.br/produto"));assertEquals("PCM-Exemplo-12345",page.sku());assertEquals("Computador de exemplo",page.name());assertEquals(1,page.headings().size());}
    @Test void rejectsCategoryOrIncompletePage(){assertThrows(IllegalArgumentException.class,()->PichauSite.parse("<h1>Computadores</h1>",URI.create("https://www.pichau.com.br/computadores")));}
    @Test void changedPublicSpecsDoNotUseAnOldRecipe(){State s=new State();PcExamples.add(s);PcPreset pc=PcApi.resolve(s,"68475");var page=new PichauSite.ProductPage(pc.code,"Ryzen 5 5600",pc.sourceUrl,"RX 9050 16GB DDR4 SSD 480GB APUS A520M 500W",java.util.List.of());assertTrue(PichauSite.missingClaims(page,pc).contains("5500"));}
}
