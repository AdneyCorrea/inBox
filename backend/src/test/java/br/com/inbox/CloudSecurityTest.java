package br.com.inbox;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:security;DB_CLOSE_DELAY=-1","inbox.auth.enabled=true","APP_LOGIN_USERNAME=operator"})
@AutoConfigureMockMvc
class CloudSecurityTest {
 @Autowired MockMvc mvc;
 @DynamicPropertySource static void credentials(DynamicPropertyRegistry registry){registry.add("APP_LOGIN_PASSWORD_HASH",()->new BCryptPasswordEncoder(12).encode("test-only-operator-password"));}
 @Test void anonymousCannotReadCatalogOrBackup()throws Exception{mvc.perform(get("/api/simple/state")).andExpect(status().isUnauthorized());mvc.perform(get("/api/backup")).andExpect(status().isUnauthorized());mvc.perform(get("/")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("http://localhost/login.html"));}
 @Test void healthAndLoginArePublic()throws Exception{mvc.perform(get("/api/health")).andExpect(status().isOk());mvc.perform(get("/login.html")).andExpect(status().isOk());mvc.perform(get("/auth/session")).andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(true)).andExpect(jsonPath("$.authenticated").value(false)).andExpect(jsonPath("$.token").isNotEmpty());}
 @Test void correctPasswordCreatesSessionAndWrongPasswordFails()throws Exception{mvc.perform(formLogin("/login").user("operator").password("test-only-operator-password")).andExpect(authenticated()).andExpect(redirectedUrl("/"));mvc.perform(formLogin("/login").user("operator").password("wrong")).andExpect(unauthenticated()).andExpect(redirectedUrl("/login.html?error"));}
 @Test void writesRequireCsrfEvenWhenLoggedIn()throws Exception{mvc.perform(post("/api/simple/calculate").with(user("operator")).contentType("application/json").content("[]")).andExpect(status().isForbidden());mvc.perform(post("/api/simple/calculate").with(user("operator")).with(csrf()).contentType("application/json").content("[]")).andExpect(status().isBadRequest());}
 @Test void authenticatedCatalogAndLogoutWork()throws Exception{mvc.perform(get("/api/simple/state").with(user("operator"))).andExpect(status().isOk());mvc.perform(post("/logout").with(user("operator")).with(csrf())).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login.html?logout"));}
}
