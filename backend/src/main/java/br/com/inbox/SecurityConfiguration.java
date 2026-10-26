package br.com.inbox;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.RequestMatcher;
import java.util.LinkedHashMap;

@Configuration
public class SecurityConfiguration {
 @Bean BCryptPasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder(12);}
 @Bean UserDetailsService users(@Value("${inbox.auth.enabled:false}") boolean enabled,@Value("${APP_LOGIN_USERNAME:}") String username,@Value("${APP_LOGIN_PASSWORD_HASH:}") String hash){
  if(!enabled)return new InMemoryUserDetailsManager();
  if(username.isBlank()||!hash.matches("\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}"))throw new IllegalStateException("Configure APP_LOGIN_USERNAME e APP_LOGIN_PASSWORD_HASH (BCrypt) antes de publicar.");
  return new InMemoryUserDetailsManager(User.withUsername(username).password(hash).roles("OPERATOR").build());
 }
 @Bean SecurityFilterChain security(HttpSecurity http,@Value("${inbox.auth.enabled:false}") boolean enabled) throws Exception {
  if(!enabled){http.authorizeHttpRequests(a->a.anyRequest().permitAll()).csrf(c->c.disable());return http.build();}
  var entries=new LinkedHashMap<RequestMatcher,AuthenticationEntryPoint>();
  entries.put(new AntPathRequestMatcher("/api/**"),new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED));
  var entryPoint=new DelegatingAuthenticationEntryPoint(entries);
  entryPoint.setDefaultEntryPoint(new LoginUrlAuthenticationEntryPoint("/login.html"));
  http.authorizeHttpRequests(a->a.requestMatchers("/login.html","/login.js","/theme.css","/simple.css","/auth/session","/api/health","/api/health/ready","/error").permitAll().anyRequest().authenticated())
   .formLogin(f->f.loginPage("/login.html").loginProcessingUrl("/login").defaultSuccessUrl("/",true).failureUrl("/login.html?error").permitAll())
   .logout(l->l.logoutUrl("/logout").logoutSuccessUrl("/login.html?logout").invalidateHttpSession(true).deleteCookies("JSESSIONID"))
   .exceptionHandling(e->e.authenticationEntryPoint(entryPoint))
   .headers(h->h.contentSecurityPolicy(c->c.policyDirectives("default-src 'self'; script-src 'self' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self'; object-src 'none'; base-uri 'self'; frame-ancestors 'none'; form-action 'self'")));
  return http.build();
 }
}
