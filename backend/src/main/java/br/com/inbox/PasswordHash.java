// Gera e valida hashes de senha para configurar o acesso à aplicação.
package br.com.inbox;

import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.nio.file.*;
import java.security.SecureRandom;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Interactive local utility; never sends or stores the chosen password. */
public final class PasswordHash {
    public static void main(String[] args) throws Exception{
        if(args.length==2&&"--generate".equals(args[0])){
            Path file=Path.of(args[1]);
            byte[] bytes=new byte[24];
            new SecureRandom().nextBytes(bytes);
            String password=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            String hash=new BCryptPasswordEncoder(12).encode(password);
            Files.writeString(file,new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(Map.of("username","inbox","password",password,"APP_LOGIN_PASSWORD_HASH",hash)),StandardOpenOption.CREATE_NEW);
            System.out.println("Credenciais iniciais gravadas em arquivo privado. A senha não foi exibida.");
            return;
        }
        var console=System.console();
        if(console==null)throw new IllegalStateException("Execute o utilitario PasswordHash em um terminal interativo.");
        char[] password=console.readPassword("Senha de acesso ao inBox (mínimo 12 caracteres): ");
        if(password==null||password.length<12)throw new IllegalArgumentException("Use pelo menos 12 caracteres.");
        char[] confirmation=console.readPassword("Repita a senha: ");
        try{
            if(!Arrays.equals(password,confirmation))throw new IllegalArgumentException("As senhas não coincidem.");
            console.printf("%nAPP_LOGIN_PASSWORD_HASH=%s%n",new BCryptPasswordEncoder(12).encode(new String(password)));
        }
        finally{
            Arrays.fill(password,'\0');
            if(confirmation!=null)Arrays.fill(confirmation,'\0');
        }
    }
}
