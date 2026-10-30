package br.com.pichau.inbox.service;

import br.com.pichau.inbox.database.model.Caixa;
import br.com.pichau.inbox.database.repository.CaixaRepository;
import br.com.pichau.inbox.database.repository.ProdutoRepository;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

/** Busca uma caixa que acomode os itens, permitindo rotações e reservando 15% para bolha. */
@Service
public class EmbalagemService {
    private final ProdutoRepository produtoRepository;
    private final CaixaRepository caixaRepository;

    public EmbalagemService(ProdutoRepository produtoRepository, CaixaRepository caixaRepository) {
        this.produtoRepository = produtoRepository;
        this.caixaRepository = caixaRepository;
    }

    public Resultado calcular(List<ItemPedido> itens) {
        if (itens == null || itens.isEmpty() || itens.size() > 200)
            throw new IllegalArgumentException("Selecione entre 1 e 200 unidades por cálculo.");
        var unidades = new ArrayList<Unidade>();
        double volumeProdutos = 0;
        for (var item : itens) {
            if (item == null || item.produtoId() == null || item.produtoId().isBlank()
                    || item.quantidade() < 1 || item.quantidade() > 200 || unidades.size() + item.quantidade() > 200)
                throw new IllegalArgumentException("Informe produtos válidos e no máximo 200 unidades por cálculo.");
            var produto = produtoRepository.buscarPorId(item.produtoId())
                    .orElseThrow(() -> new NoSuchElementException("Produto não encontrado no servidor."));
            int c = produto.getComprimento(), l = produto.getLargura(), a = produto.getAltura();
            if (c <= 0 || l <= 0 || a <= 0) throw new IllegalArgumentException("As dimensões devem ser positivas.");
            for (int i = 0; i < item.quantidade(); i++) {
                unidades.add(new Unidade(item.produtoId(), produto.getNome(), c, l, a));
                volumeProdutos += (double) c * l * a;
            }
        }
        // Testa as caixas da menor para a maior.
        var candidatas = caixaRepository.buscarTodos().stream()
                .filter(caixa -> caixa.getComprimento() > 0 && caixa.getLargura() > 0 && caixa.getAltura() > 0)
                .sorted(Comparator.comparingDouble(caixa -> volume(caixa))).toList();
        for (var caixa : candidatas) {
            if (volumeProdutos > volume(caixa) * 0.85) continue;
            // Três ordens dos produtos e duas divisões do espaço: seis tentativas por caixa.
            for (int ordem = 0; ordem < 3; ordem++) {
                var ordenadas = new ArrayList<>(unidades);
                final int criterio = ordem;
                ordenadas.sort(Comparator.comparingDouble((Unidade u) -> switch (criterio) {
                    case 1 -> Math.max(u.c(), Math.max(u.l(), u.a()));
                    case 2 -> Math.min(u.c(), Math.min(u.l(), u.a()));
                    default -> (double) u.c() * u.l() * u.a();
                }).reversed());
                for (boolean dividirComprimento : new boolean[]{true, false}) {
                    var posicoes = acomodar(caixa, ordenadas, dividirComprimento);
                    if (posicoes != null) return new Resultado(caixa, posicoes,
                            (1 - volumeProdutos / volume(caixa)) * 100,
                            "Menor caixa com disposição encontrada pelo algoritmo.");
                }
            }
        }
        return new Resultado(null, List.of(), 0,
                "Não foi encontrada uma disposição com pelo menos 15% de volume livre. O algoritmo tenta combinações, mas não esgota todas as possibilidades.");
    }

    public Resultado calcular(String produtoId, int quantidade) {
        return calcular(List.of(new ItemPedido(produtoId, quantidade)));
    }

    private List<Posicao> acomodar(Caixa caixa, List<Unidade> unidades, boolean dividirComprimento) {
        var livres = new ArrayList<Espaco>();
        livres.add(new Espaco(0, 0, 0, caixa.getComprimento(), caixa.getLargura(), caixa.getAltura()));
        var posicoes = new ArrayList<Posicao>();
        for (var unidade : unidades) {
            Espaco escolhido = null;
            int[] orientacao = null;
            double melhor = Double.POSITIVE_INFINITY;
            int c = unidade.c(), l = unidade.l(), a = unidade.a();
            int[][] rotacoes = {{c,l,a}, {c,a,l}, {l,c,a}, {l,a,c}, {a,c,l}, {a,l,c}};
            for (var espaco : livres) {
                for (var r : rotacoes) {
                    if (r[0] > espaco.c() || r[1] > espaco.l() || r[2] > espaco.a()) continue;
                    double score = (double) espaco.c() * espaco.l() * espaco.a();
                    if (score < melhor || (score == melhor && orientacao != null && r[2] < orientacao[2])) {
                        melhor = score; escolhido = espaco; orientacao = r;
                    }
                }
            }
            if (escolhido == null || orientacao == null) return null;
            livres.remove(escolhido);
            var e = escolhido;
            int pc = orientacao[0], pl = orientacao[1], pa = orientacao[2];
            posicoes.add(new Posicao(unidade.id(), unidade.nome(), e.x(), e.y(), e.z(), pc, pl, pa));
            // Partições sem sobreposição. O espaço superior fica apoiado no produto.
            if (dividirComprimento) {
                adicionar(livres, new Espaco(e.x()+pc, e.y(), e.z(), e.c()-pc, e.l(), e.a()));
                adicionar(livres, new Espaco(e.x(), e.y()+pl, e.z(), pc, e.l()-pl, e.a()));
            } else {
                adicionar(livres, new Espaco(e.x()+pc, e.y(), e.z(), e.c()-pc, pl, e.a()));
                adicionar(livres, new Espaco(e.x(), e.y()+pl, e.z(), e.c(), e.l()-pl, e.a()));
            }
            adicionar(livres, new Espaco(e.x(), e.y(), e.z()+pa, pc, pl, e.a()-pa));
        }
        return List.copyOf(posicoes);
    }

    private void adicionar(List<Espaco> livres, Espaco espaco) {
        if (espaco.c() > 0 && espaco.l() > 0 && espaco.a() > 0) livres.add(espaco);
    }
    private double volume(Caixa caixa) {
        return (double) caixa.getComprimento() * caixa.getLargura() * caixa.getAltura();
    }
    // Produto selecionado e quantidade pedida.
    public record ItemPedido(String produtoId, int quantidade) { }
    // Cada unidade é acomodada separadamente.
    private record Unidade(String id, String nome, int c, int l, int a) { }
    // Trecho da caixa que ainda está livre.
    private record Espaco(int x, int y, int z, int c, int l, int a) { }
    // Local e orientação de uma unidade, usados para desenhar a vista 3D.
    public record Posicao(String produtoId, String nome, int x, int y, int z, int comprimento, int largura, int altura) { }
    // Caixa recomendada e disposição dos produtos.
    public record Resultado(Caixa caixa, List<Posicao> posicoes, double percentualLivre, String mensagem) { }
}
