package br.com.inbox;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Value;
@RestController
public class ReadinessApi {
 private final JdbcTemplate jdbc;
 @Value("${inbox.database.table:app_state}") String table;
 public ReadinessApi(JdbcTemplate jdbc){this.jdbc=jdbc;}
 @GetMapping("/api/health/ready") public ResponseEntity<Map<String,String>> ready(){
  try{jdbc.query("SELECT id FROM "+table+" WHERE id=1",(rs,n)->rs.getInt(1));return ResponseEntity.ok(Map.of("status","ready"));}
  catch(Exception e){return ResponseEntity.status(503).body(Map.of("status","unavailable"));}
 }
}
