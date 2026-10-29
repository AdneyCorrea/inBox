// Encaminha a página inicial e os endereços antigos para a interface atual.
package br.com.inbox;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Entrada unica da aplicacao: encaminha os enderecos antigos para a interface atual. */
@Controller
public class Home {
    @GetMapping({
        "/","/index.html"
    }) public String home(){
        return "forward:/simple.html";
    }
}
