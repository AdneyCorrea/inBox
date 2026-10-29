// Informa o estado da sessão e fornece o token CSRF para operações autenticadas.
package br.com.inbox;

import java.util.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.csrf.CsrfToken;

/** Consulta de sessao e token CSRF para o login e as operacoes autenticadas. */
@RestController
public class SessionApi {
    @Value("${inbox.auth.enabled:false}") boolean enabled;
    @GetMapping("/auth/session")
public Map<String,Object> session(HttpServletRequest request){
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("enabled",enabled);
        result.put("authenticated",request.getUserPrincipal()!=null);
        CsrfToken token=(CsrfToken)request.getAttribute(CsrfToken.class.getName());
        if(token!=null){
            result.put("token",token.getToken());
            result.put("headerName",token.getHeaderName());
            result.put("parameterName",token.getParameterName());
        }
        return result;
    }
}
