package br.com.pichau.inbox.database.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

/** Produto com as dimensões externas da embalagem, em milímetros. */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Produto {

    private String id;

    @JsonAlias("name")
    private String nome;

    private String sku;

    @JsonAlias("category")
    private String categoria;

    @JsonAlias("l")
    private int comprimento;

    @JsonAlias("w")
    private int largura;

    @JsonAlias("h")
    private int altura;
}
