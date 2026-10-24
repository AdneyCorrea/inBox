package br.com.inbox;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class Home {
    @GetMapping({"/","/index.html"}) public String home(){return "forward:/simple.html";}
}
