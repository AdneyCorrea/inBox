package br.com.inbox;
import java.util.Arrays;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
/** Interactive local utility; never sends or stores the chosen password. */
public final class PasswordHash {
 public static void main(String[] args){
  var console=System.console();if(console==null)throw new IllegalStateException("Execute gerar-login.cmd em um terminal interativo.");
  char[] password=console.readPassword("Senha de acesso ao inBox (mínimo 12 caracteres): ");
  if(password==null||password.length<12)throw new IllegalArgumentException("Use pelo menos 12 caracteres.");
  char[] confirmation=console.readPassword("Repita a senha: ");
  try{if(!Arrays.equals(password,confirmation))throw new IllegalArgumentException("As senhas não coincidem.");console.printf("%nAPP_LOGIN_PASSWORD_HASH=%s%n",new BCryptPasswordEncoder(12).encode(new String(password)));}
  finally{Arrays.fill(password,'\0');if(confirmation!=null)Arrays.fill(confirmation,'\0');}
 }
}
