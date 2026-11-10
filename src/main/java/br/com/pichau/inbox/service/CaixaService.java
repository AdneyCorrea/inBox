package br.com.pichau.inbox.service;

import br.com.pichau.inbox.database.model.Caixa;
import br.com.pichau.inbox.database.repository.CaixaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.NoSuchElementException;

/** Valida as caixas antes de consultar, salvar ou remover pelo repository. */
@Service
public class CaixaService {

    private final CaixaRepository repository;

    public CaixaService(CaixaRepository repository) {
        this.repository = repository;
    }

    public List<Caixa> buscarTodos() {
        return repository.buscarTodos();
    }

    public Caixa cadastrar(Caixa caixa) {
        validar(caixa);
        caixa.setId(UUID.randomUUID().toString());
        return repository.salvar(caixa);
    }

    // Repetir a importação não duplica nem sobrescreve um cadastro já importado.
    public synchronized Caixa importar(UUID origem, long idAntigo, Caixa registro) {
        if (idAntigo < 0) throw new IllegalArgumentException("ID antigo inválido.");
        var id = "legacy-" + UUID.nameUUIDFromBytes((origem + ":Caixa:" + idAntigo)
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var existente = repository.buscarPorId(id);
        if (existente.isPresent()) return existente.get();
        validar(registro);
        registro.setId(id);
        return repository.salvar(registro);
    }
    public Caixa atualizar(String id, Caixa caixa) {
        exigirExistente(id);
        validar(caixa);
        caixa.setId(id);
        return repository.salvar(caixa);
    }

    public void remover(String id) {
        exigirExistente(id);
        repository.remover(id);
    }

    private void exigirExistente(String id) {
        if (repository.buscarPorId(id).isEmpty()) throw new NoSuchElementException("Cadastro não encontrado.");
    }

    private void validar(Caixa caixa) {
        if (caixa == null || caixa.getNome() == null || caixa.getNome().isBlank()
                || caixa.getNome().length() > 120 || caixa.getComprimento() < 1
                || caixa.getLargura() < 1 || caixa.getAltura() < 1
                || caixa.getComprimento() > 100000 || caixa.getLargura() > 100000
                || caixa.getAltura() > 100000) {
            throw new IllegalArgumentException("Informe nome e medidas entre 0,1 e 10.000 cm.");
        }
        caixa.setNome(caixa.getNome().trim());

    }
}
