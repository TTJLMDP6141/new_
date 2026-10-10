package com.urbanservices.web;

import com.urbanservices.repository.ServiceOfferingRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    private final ServiceOfferingRepository services;

    public HomeController(ServiceOfferingRepository services) {
        this.services = services;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("services", services.findTop8ByActiveTrueOrderByNameAsc());
        return "home";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access-denied";
    }
}
