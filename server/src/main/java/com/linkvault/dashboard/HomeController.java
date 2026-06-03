package com.linkvault.dashboard;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping({"/", "/api"})
    public String swagger() {
        return "redirect:/swagger-ui.html";
    }
}
