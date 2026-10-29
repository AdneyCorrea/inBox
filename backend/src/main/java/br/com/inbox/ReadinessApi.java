package br.com.inbox;

import java.util.Map;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Value;

/** Verifica a disponibilidade do banco sem expor conteudo ou credenciais. */
@RestController
public class ReadinessApi {
    private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(ReadinessApi.class);
    private String lastFailure="";
    private final JdbcTemplate jdbc;
    @Value("${inbox.database.table:app_state}") String table;
    public ReadinessApi(JdbcTemplate jdbc){
        this.jdbc=jdbc;
    }
    @GetMapping("/api/health/ready")
public ResponseEntity<Map<String,String>> ready(){
        try{
            jdbc.query("SELECT id FROM "+table+" WHERE id=1",(rs,n)->rs.getInt(1));
            return ResponseEntity.ok(Map.of("status","ready"));
        }
        catch(Exception e){
            String category="database unavailable";
            for(Throwable cause=e;cause!=null;cause=cause.getCause()){
                String message=String.valueOf(cause.getMessage()).toLowerCase(java.util.Locale.ROOT);
                if(message.contains("password authentication")||message.contains("tenant or user"))category="database credentials rejected";
                else if(message.contains("certificate")||message.contains("pkix")||message.contains("ssl"))category="database TLS verification failed";
                else if(message.contains("permission denied"))category="database permission denied";
                else if(message.contains("does not exist"))category="database schema missing";
                else if(message.contains("unknownhost"))category="database hostname unresolved";
            }
            if(!category.equals(lastFailure)){
                log.warn("Readiness: {}",category);
                lastFailure=category;
            }
            return ResponseEntity.status(503).body(Map.of("status","unavailable"));
        }
    }
}
