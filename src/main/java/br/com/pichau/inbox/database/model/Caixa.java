package br.com.pichau.inbox.database.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

/** Caixa com as dimensões internas disponíveis, em milímetros. */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Caixa {

    private String id;

    @JsonAlias("name")
    private String nome;

    @JsonAlias("l")
    private int comprimento;

    @JsonAlias("w")
    private int largura;

    @JsonAlias("h")
    private int altura;
}
