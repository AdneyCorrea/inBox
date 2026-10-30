package br.com.pichau.inbox.handler;

import br.com.pichau.inbox.controller.ProdutoController;
import br.com.pichau.inbox.controller.CaixaController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Map;
import java.util.NoSuchElementException;

/** Devolve os erros de cadastro como respostas HTTP para a interface. */
@RestControllerAdvice(assignableTypes = {ProdutoController.class, CaixaController.class})
public class CadastroExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> invalido(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("mensagem", exception.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> inexistente(NoSuchElementException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("mensagem", exception.getMessage()));
    }
}
