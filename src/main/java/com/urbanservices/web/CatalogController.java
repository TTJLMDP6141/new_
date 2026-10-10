package com.urbanservices.web;

import com.urbanservices.repository.ServiceOfferingRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CatalogController {
    private final ServiceOfferingRepository services;

    public CatalogController(ServiceOfferingRepository services) {
        this.services = services;
    }

    @GetMapping("/services")
    public String list(@RequestParam(required = false) String search, Model model) {
        model.addAttribute("search", search == null ? "" : search);
        model.addAttribute("services", search == null || search.isBlank()
            ? services.findByActiveTrueOrderByNameAsc()
            : services.searchActive(search.trim()));
        return "services/index";
    }

    @GetMapping("/services/{id}")
    public String details(@PathVariable Long id, Model model) {
        var service = services.findByIdAndActiveTrue(id);
        if (service.isEmpty()) return "redirect:/services";
        model.addAttribute("service", service.get());
        return "services/details";
    }
}
