package com.urbanservices.web;

import com.urbanservices.model.Booking;
import com.urbanservices.model.BookingStatus;
import com.urbanservices.model.ServiceOffering;
import com.urbanservices.model.UserRole;
import com.urbanservices.repository.BookingRepository;
import com.urbanservices.repository.ProviderProfileRepository;
import com.urbanservices.repository.ServiceOfferingRepository;
import com.urbanservices.repository.UserAccountRepository;
import com.urbanservices.web.form.ServiceForm;
import jakarta.validation.Valid;
import java.util.Arrays;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final UserAccountRepository users;
    private final ProviderProfileRepository providers;
    private final ServiceOfferingRepository services;
    private final BookingRepository bookings;

    public AdminController(
        UserAccountRepository users,
        ProviderProfileRepository providers,
        ServiceOfferingRepository services,
        BookingRepository bookings
    ) {
        this.users = users;
        this.providers = providers;
        this.services = services;
        this.bookings = bookings;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("customerCount", users.countByRole(UserRole.CUSTOMER));
        model.addAttribute("providerCount", users.countByRole(UserRole.PROVIDER));
        model.addAttribute("serviceCount", services.count());
        model.addAttribute("bookingCount", bookings.count());
        model.addAttribute("recentBookings", bookings.findTop8ByOrderByCreatedAtDesc());
        return "admin/dashboard";
    }

    @GetMapping("/customers")
    public String customers(Model model) {
        model.addAttribute("customers", users.findAllByRoleOrderByNameAsc(UserRole.CUSTOMER));
        return "admin/customers";
    }

    @GetMapping("/providers")
    public String providers(Model model) {
        model.addAttribute("providers", providers.findAllByOrderByUser_NameAsc());
        return "admin/providers";
    }

    @GetMapping("/bookings")
    public String manageBookings(Model model) {
        model.addAttribute("bookings", bookings.findAll());
        model.addAttribute("providers", providers.findAllByOrderByUser_NameAsc());
        model.addAttribute("statuses", BookingStatus.values());
        return "admin/bookings";
    }

    @PostMapping("/bookings/{id}/assign")
    public String assignProvider(
        @PathVariable Long id,
        @RequestParam(required = false) Long providerId,
        RedirectAttributes redirect
    ) {
        var booking = bookings.findById(id);
        if (booking.isEmpty()) return "redirect:/admin/bookings";
        if (providerId == null) {
            booking.get().setProviderProfile(null);
        } else {
            var provider = providers.findById(providerId);
            if (provider.isEmpty()) {
                redirect.addFlashAttribute("error", "Please choose a valid provider.");
                return "redirect:/admin/bookings";
            }
            booking.get().setProviderProfile(provider.get());
        }
        bookings.save(booking.get());
        redirect.addFlashAttribute("success", "Provider assignment updated.");
        return "redirect:/admin/bookings";
    }

    @PostMapping("/bookings/{id}/status")
    public String updateBookingStatus(
        @PathVariable Long id,
        @RequestParam String status,
        RedirectAttributes redirect
    ) {
        var booking = bookings.findById(id);
        if (booking.isEmpty()) return "redirect:/admin/bookings";
        try {
            booking.get().setStatus(BookingStatus.valueOf(status));
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", "Please choose a valid booking status.");
            return "redirect:/admin/bookings";
        }
        bookings.save(booking.get());
        redirect.addFlashAttribute("success", "Booking status updated.");
        return "redirect:/admin/bookings";
    }

    @GetMapping("/services")
    public String manageServices(Model model) {
        model.addAttribute("services", services.findAllByOrderByNameAsc());
        return "admin/services";
    }

    @GetMapping("/services/new")
    public String createServiceForm(Model model) {
        model.addAttribute("form", new ServiceForm());
        model.addAttribute("editing", false);
        return "admin/service-form";
    }

    @PostMapping("/services")
    public String createService(
        @Valid @ModelAttribute("form") ServiceForm form,
        BindingResult result,
        Model model,
        RedirectAttributes redirect
    ) {
        if (result.hasErrors()) {
            model.addAttribute("editing", false);
            return "admin/service-form";
        }
        ServiceOffering service = new ServiceOffering();
        copyForm(form, service);
        services.save(service);
        redirect.addFlashAttribute("success", "Service added successfully.");
        return "redirect:/admin/services";
    }

    @GetMapping("/services/{id}/edit")
    public String editServiceForm(@PathVariable Long id, Model model) {
        var service = services.findById(id);
        if (service.isEmpty()) return "redirect:/admin/services";
        model.addAttribute("form", toForm(service.get()));
        model.addAttribute("serviceId", id);
        model.addAttribute("editing", true);
        return "admin/service-form";
    }

    @PostMapping("/services/{id}")
    public String editService(
        @PathVariable Long id,
        @Valid @ModelAttribute("form") ServiceForm form,
        BindingResult result,
        Model model,
        RedirectAttributes redirect
    ) {
        var service = services.findById(id);
        if (service.isEmpty()) return "redirect:/admin/services";
        if (result.hasErrors()) {
            model.addAttribute("serviceId", id);
            model.addAttribute("editing", true);
            return "admin/service-form";
        }
        copyForm(form, service.get());
        services.save(service.get());
        redirect.addFlashAttribute("success", "Service updated successfully.");
        return "redirect:/admin/services";
    }

    @PostMapping("/services/{id}/delete")
    public String deleteService(@PathVariable Long id, RedirectAttributes redirect) {
        var service = services.findById(id);
        if (service.isEmpty()) return "redirect:/admin/services";
        if (bookings.existsByService_Id(id)) {
            redirect.addFlashAttribute("error", "This service has bookings and cannot be deleted. Mark it inactive instead.");
            return "redirect:/admin/services";
        }
        try {
            services.delete(service.get());
            redirect.addFlashAttribute("success", "Service deleted successfully.");
        } catch (DataIntegrityViolationException ex) {
            redirect.addFlashAttribute("error", "This service is referenced by existing records. Mark it inactive instead.");
        }
        return "redirect:/admin/services";
    }

    private ServiceForm toForm(ServiceOffering service) {
        ServiceForm form = new ServiceForm();
        form.setName(service.getName());
        form.setDescription(service.getDescription());
        form.setCategory(service.getCategory());
        form.setPrice(service.getPrice());
        form.setDurationMinutes(service.getDurationMinutes());
        form.setImageUrl(service.getImageUrl());
        form.setActive(service.isActive());
        return form;
    }

    private void copyForm(ServiceForm form, ServiceOffering service) {
        service.setName(form.getName().trim());
        service.setDescription(form.getDescription().trim());
        service.setCategory(form.getCategory().trim());
        service.setPrice(form.getPrice());
        service.setDurationMinutes(form.getDurationMinutes());
        service.setImageUrl(form.getImageUrl() == null || form.getImageUrl().isBlank()
            ? null : form.getImageUrl().trim());
        service.setActive(form.isActive());
    }
}
