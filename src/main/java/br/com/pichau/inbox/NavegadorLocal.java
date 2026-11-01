package br.com.pichau.inbox;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "inbox.abrir-navegador", havingValue = "true")
public class NavegadorLocal {
    @EventListener(ApplicationReadyEvent.class)
    public void abrir(ApplicationReadyEvent evento) {
        var contexto = (WebServerApplicationContext) evento.getApplicationContext();
        String url = "http://127.0.0.1:" + contexto.getWebServer().getPort();
        System.out.println("\nAbra o inBox em: " + url);
        try {
            new ProcessBuilder("rundll32.exe", "url.dll,FileProtocolHandler", url).start();
        } catch (java.io.IOException exception) {
            System.out.println("Abra o endereco acima no navegador para acessar o inBox.");
        }
    }
}
