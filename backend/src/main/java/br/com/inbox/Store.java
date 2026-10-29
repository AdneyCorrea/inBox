package br.com.inbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static br.com.inbox.Model.*;

/** Persistencia do documento JSON com controle de revisao para acessos concorrentes. */
@Component
public class Store {
    private final JdbcTemplate jdbc;
    public final ObjectMapper mapper;
    @Value("${inbox.database.table:app_state}")
private String table="app_state";
    public Store(JdbcTemplate jdbc,ObjectMapper mapper) {
        this.jdbc=jdbc;
        this.mapper=mapper;
    }
    public State read() {
        var rows=jdbc.query("SELECT payload FROM "+table+" WHERE id=1",(rs,n)->rs.getString(1));
        try {
            return rows.isEmpty()?new State():mapper.readValue(rows.get(0),State.class);
        }
        catch(Exception e) {
            throw new IllegalStateException("Não foi possível ler o banco de dados.",e);
        }
    }
    public void save(State state) {
        long previous=state.generation;
        try {
            var rows=jdbc.query("SELECT payload FROM "+table+" WHERE id=1",(rs,n)->rs.getString(1));
            String old=rows.isEmpty()?null:rows.get(0);
            // Se outra pessoa salvou depois que esta tela foi aberta, pedimos para
            // atualizar. Não deixamos uma edição antiga apagar o trabalho mais novo.
            if(old!=null&&mapper.readValue(old,State.class).generation!=previous)throw conflict();
            state.generation=previous+1;
            String payload=mapper.writeValueAsString(state);
            // A revisão acima, sozinha, não basta: outro servidor pode salvar entre
            // o SELECT e o UPDATE. O payload na condição fecha essa janela de corrida.
            int changed=old==null?jdbc.update("INSERT INTO "+table+" (id,payload) SELECT 1,? WHERE NOT EXISTS (SELECT 1 FROM "+table+" WHERE id=1)",payload) :jdbc.update("UPDATE "+table+" SET payload=? WHERE id=1 AND payload=?",payload,old);
            if(changed!=1)throw conflict();
        }
        catch(ResponseStatusException e){
            state.generation=previous;
            throw e;
        }
        catch(org.springframework.dao.DuplicateKeyException e){
            state.generation=previous;
            throw conflict();
        }
        catch(Exception e) {
            state.generation=previous;
            throw new IllegalStateException("Não foi possível gravar o banco de dados.",e);
        }
    }
    private ResponseStatusException conflict(){
        return new ResponseStatusException(HttpStatus.CONFLICT,"Os dados mudaram em outro acesso. Atualize a tela e tente novamente.");
    }
    public String json(Object o) {
        try {
            return mapper.writeValueAsString(o);
        }
        catch(Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
