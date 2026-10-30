package br.com.pichau.inbox.controller;

import br.com.pichau.inbox.service.EmbalagemService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.NoSuchElementException;
import java.util.List;

/** Recebe os itens do pedido e solicita o cálculo da caixa. */
@RestController
@RequestMapping("/api/embalagens")
public class EmbalagemController {

    private final EmbalagemService service;

    public EmbalagemController(EmbalagemService service) {
        this.service = service;
    }

    @PostMapping("/calcular")
    public EmbalagemService.Resultado calcular(@RequestBody Solicitacao solicitacao) {
        try {
            return service.calcular(solicitacao.itens());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        } catch (NoSuchElementException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage());
        }
    }

    public record Solicitacao(List<EmbalagemService.ItemPedido> itens) { }
}
