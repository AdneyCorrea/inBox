package br.com.pichau.inbox.controller;

import br.com.pichau.inbox.database.model.Caixa;
import br.com.pichau.inbox.service.CaixaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;

/** Recebe as requisições de caixas e chama o CaixaService. */
@RestController
@RequestMapping("/api/caixas")
public class CaixaController {

    private final CaixaService service;

    public CaixaController(CaixaService service) {
        this.service = service;
    }

    @GetMapping
    public List<Caixa> buscarTodos() {
        return service.buscarTodos();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Caixa cadastrar(@RequestBody Caixa registro) {
        return service.cadastrar(registro);
    }

    @PutMapping("/{id}")
    public Caixa atualizar(@PathVariable String id, @RequestBody Caixa registro) {
        return service.atualizar(id, registro);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable String id) {
        service.remover(id);
    }
}
