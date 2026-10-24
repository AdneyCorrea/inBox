package br.com.inbox;

import java.net.*;
import java.net.http.*;
import java.io.*;
import java.time.*;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.springframework.stereotype.Component;
import static br.com.inbox.Model.*;
import static br.com.inbox.Validation.require;

@Component
public class PichauSite {
    private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(7)).followRedirects(HttpClient.Redirect.NEVER).build();
    public static URI allowed(String value){
        URI uri;try{uri=URI.create(value);}catch(Exception e){throw new IllegalArgumentException("Link inválido.");}
        require("https".equalsIgnoreCase(uri.getScheme()) && "www.pichau.com.br".equalsIgnoreCase(uri.getHost()) && uri.getUserInfo()==null && (uri.getPort()==-1 || uri.getPort()==443),"Use um link HTTPS de www.pichau.com.br.");
        return uri;
    }
    public String fetch(URI uri) throws IOException,InterruptedException {
        for(int redirects=0;redirects<4;redirects++){
            allowed(uri.toString());
            HttpRequest request=HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(12)).header("User-Agent","inBox/0.1 (consulta de catalogo publico)").header("Accept","text/html").GET().build();
            HttpResponse<InputStream> response=client.send(request,HttpResponse.BodyHandlers.ofInputStream());
            try(InputStream stream=response.body()){
                if(response.statusCode()>=300&&response.statusCode()<400){String target=response.headers().firstValue("Location").orElseThrow(()->new IOException("Redirecionamento inválido."));uri=allowed(uri.resolve(target).toString());continue;}
                if(response.statusCode()==403)throw new IOException("A Pichau bloqueou a consulta automática (HTTP 403). Esta busca precisa de acesso autorizado ao catálogo.");
                if(response.statusCode()!=200)throw new IOException("A consulta à Pichau está indisponível (HTTP "+response.statusCode()+"). Tente novamente mais tarde.");
                byte[] bytes=stream.readNBytes(4_000_001);if(bytes.length>4_000_000)throw new IOException("Página maior que o limite da consulta.");
                String html=new String(bytes,java.nio.charset.StandardCharsets.UTF_8);
                if(html.toLowerCase(Locale.ROOT).contains("site em manuten") || html.contains("cf-chl-") || html.contains("Just a moment"))throw new IOException("O site apresentou manutenção ou uma verificação de acesso. Tente novamente mais tarde.");
                return html;
            }
        }
        throw new IOException("Não foi possível concluir o redirecionamento da Pichau.");
    }
    public record ProductPage(String sku,String name,String url,String text,List<String> headings) {}
    public static String normalized(String text){return Normalizer.normalize(text,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]","");}
    public static ProductPage parse(String html,URI url){
        Document doc=Jsoup.parse(html,url.toString());String name=doc.select("h1").text();
        String text=doc.body()==null?"":doc.body().text();String sku="";
        Matcher matcher=Pattern.compile("(?i)\\bsku\\s*:\\s*([a-z0-9][a-z0-9._-]+)").matcher(text);if(matcher.find())sku=matcher.group(1);
        if(sku.isBlank()){
            for(Element script:doc.select("script[type=application/ld+json]")){
                try{var json=new com.fasterxml.jackson.databind.ObjectMapper().readTree(script.data());List<com.fasterxml.jackson.databind.JsonNode> nodes=new ArrayList<>();if(json.isArray())json.forEach(nodes::add);else{nodes.add(json);if(json.path("@graph").isArray())json.path("@graph").forEach(nodes::add);}
                    for(var node:nodes)if(node.path("@type").asText().equals("Product")&&normalized(node.path("name").asText()).equals(normalized(name)))sku=node.path("sku").asText("");
                }catch(com.fasterxml.jackson.core.JsonProcessingException ignored){}
            }
        }
        require(!name.isBlank()&&!sku.isBlank(),"A página não forneceu nome e SKU identificáveis. Use o link direto do produto.");
        List<String> headings=doc.select("h2,h3").eachText().stream().filter(s->s.length()<180).toList();
        return new ProductPage(sku,name,url.toString(),text,headings);
    }
    public URI resolve(State state,String query) throws IOException,InterruptedException {
        String value=query.trim();if(value.startsWith("https://")||value.contains("://"))return allowed(value);
        require(value.matches("[A-Za-z0-9._-]{1,120}"),"Digite um SKU ou cole o link direto do produto.");
        Optional<PcPreset> known=state.dimensionPcs.stream().filter(p->p.kind.equals("BASE")&&(p.code.equalsIgnoreCase(value)||p.aliases.stream().anyMatch(a->a.equalsIgnoreCase(value)))).findFirst();
        if(known.isPresent())return allowed(known.get().sourceUrl);
        URI search=URI.create("https://www.pichau.com.br/search?q="+URLEncoder.encode(value,java.nio.charset.StandardCharsets.UTF_8));
        Document doc=Jsoup.parse(fetch(search),search.toString());Set<String> candidates=new LinkedHashSet<>();
        Matcher number=Pattern.compile("(\\d{4,})$").matcher(value);String suffix=number.find()?number.group(1):value;
        for(Element link:doc.select("a[href]")){String href=link.absUrl("href");try{URI candidate=allowed(href);if(candidate.getQuery()==null && candidate.getPath().matches("/.+-[0-9]{4,}/?") && (candidate.getPath().endsWith("-"+suffix)||normalized(link.text()).contains(normalized(value))))candidates.add(candidate.toString());}catch(IllegalArgumentException ignored){}}
        require(candidates.size()==1,"Não foi possível localizar um único produto por esse SKU. Cole o link direto da Pichau.");return allowed(candidates.iterator().next());
    }
    public ProductPage lookup(State state,String query) throws IOException,InterruptedException {URI url=resolve(state,query);return parse(fetch(url),url);}
    public static List<String> missingClaims(ProductPage page,PcPreset recipe){
        String headline=normalized(page.name()),headings=normalized(String.join(" ",page.headings()));
        List<String> missing=new ArrayList<>();
        List<String> claims=recipe.code.equals(PcExamples.HF)?List.of("9850x3d","5070ti","32gb","ddr5","2tb"):
            recipe.code.equals(PcExamples.GAMER)?List.of("5500","9050","16gb","ddr4","480gb"):List.of();
        List<String> details=recipe.code.equals(PcExamples.HF)?List.of("voyager","x870","850w","watercooler"):recipe.code.equals(PcExamples.GAMER)?List.of("apus","a520m","500w"):List.of();
        for(String claim:claims)if(!headline.contains(claim))missing.add(claim);
        for(String claim:details)if(!headings.contains(claim))missing.add(claim);
        if(claims.isEmpty())missing.add("composição exata não disponível no catálogo público");
        return missing;
    }
}
