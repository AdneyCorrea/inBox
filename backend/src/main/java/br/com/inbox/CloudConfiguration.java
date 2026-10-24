package br.com.inbox;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.boot.ApplicationRunner;
@Configuration @Profile("cloud")
public class CloudConfiguration {
 @Bean ApplicationRunner validateCloud(Environment env){return args->{
  String url=env.getRequiredProperty("spring.datasource.url");
  if(!url.startsWith("jdbc:postgresql://")||!url.contains("sslmode=verify-full"))throw new IllegalStateException("A nuvem exige PostgreSQL com sslmode=verify-full.");
  if(!env.getProperty("inbox.auth.enabled",Boolean.class,false))throw new IllegalStateException("A nuvem exige login habilitado.");
  if(!"inbox_private.app_state".equals(env.getProperty("inbox.database.table")))throw new IllegalStateException("Use o schema privado do inBox.");
 };}
}
