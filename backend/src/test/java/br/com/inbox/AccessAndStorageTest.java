// Testa autenticação, proteção CSRF, persistência e conflitos de revisão em banco de teste.
package br.com.inbox;

import org.junit.jupiter.api.Test;
import static java.util.Objects.requireNonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

/** Banco descartável em memória: estes testes nunca acessam os cadastros reais. */
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:review;DB_CLOSE_DELAY=-1","inbox.auth.enabled=true"})
@AutoConfigureMockMvc
class AccessAndStorageTest {
    @Autowired MockMvc mvc;
    @Autowired Store store;
    @DynamicPropertySource static void login(DynamicPropertyRegistry r){
        r.add("APP_LOGIN_USERNAME",()->"teste");
        String hash=new BCryptPasswordEncoder(12).encode("senha-apenas-do-teste");
        r.add("APP_LOGIN_PASSWORD_HASH",()->hash);
    }
    @Test void anonymousCannotReadCatalogOrBackup() throws Exception {
        mvc.perform(get("/api/simple/state")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/backup")).andExpect(status().isUnauthorized());
    }
    @Test void loginAssetsAndSessionArePublic() throws Exception {
        for(String path:new String[]{"/login.html","/login.js","/js/http.js","/theme.css"})mvc.perform(get(requireNonNull(path))).andExpect(status().isOk());
        mvc.perform(get("/auth/session")).andExpect(status().isOk()).andExpect(jsonPath("$.authenticated").value(false));
    }
    @Test void loginAcceptsOnlyCorrectPassword() throws Exception {
        mvc.perform(post("/login").with(requireNonNull(csrf())).param("username","teste").param("password","errada")).andExpect(redirectedUrl("/login.html?error"));
        mvc.perform(post("/login").with(requireNonNull(csrf())).param("username","teste").param("password","senha-apenas-do-teste")).andExpect(redirectedUrl("/"));
    }
    @Test void loggedUserStillNeedsCsrfForChanges() throws Exception {
        mvc.perform(put("/api/simple/products/teste").with(requireNonNull(user("teste"))).contentType("application/json").content("{}")).andExpect(status().isForbidden());
        mvc.perform(get("/api/simple/state").with(requireNonNull(user("teste")))).andExpect(status().isOk());
    }
    @Test void olderScreenCannotOverwriteNewerSave(){
        var first=store.read();var olderScreen=store.read();first.version=42;store.save(first);
        var error=assertThrows(ResponseStatusException.class,()->store.save(olderScreen));
        assertEquals(409,error.getStatusCode().value());assertEquals(42,store.read().version);
    }
    @Test void oemFlagAndMeasurementsSurviveRoundTrip(){
        var state=store.read();var p=new Model.DimensionProduct();p.name="Produto de teste";p.oem=true;p.l=123;p.w=80;p.h=30;state.dimensionProducts.add(p);store.save(state);
        var restored=store.read().dimensionProducts.stream().filter(x->x.id.equals(p.id)).findFirst().orElseThrow();
        assertTrue(restored.oem);assertEquals(123,restored.l);
    }
}
