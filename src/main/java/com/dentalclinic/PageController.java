package com.dentalclinic;

import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping({"/", "/index.html"})
    public String home(Principal principal, Model model) {
        model.addAttribute("username", principal.getName());
        return "home";
    }
}
