package com.urbanservices.web;

import com.urbanservices.model.Booking;
import com.urbanservices.model.BookingStatus;
import com.urbanservices.model.ProviderProfile;
import com.urbanservices.repository.BookingRepository;
import com.urbanservices.repository.ProviderProfileRepository;
import com.urbanservices.repository.UserAccountRepository;
import com.urbanservices.web.form.ProviderProfileForm;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/provider")
@PreAuthorize("hasRole('PROVIDER')")
public class ProviderController {
    private final ProviderProfileRepository providers;
    private final BookingRepository bookings;
    private final UserAccountRepository users;

    public ProviderController(
        ProviderProfileRepository providers,
        BookingRepository bookings,
        UserAccountRepository users
    ) {
        this.providers = providers;
        this.bookings = bookings;
        this.users = users;
    }

    @GetMapping
    public String dashboard(Authentication authentication, Model model) {
        ProviderProfile provider = currentProvider(authentication);
        List<Booking> assigned = bookings.findByProviderProfile_IdOrderByBookingDateAscBookingTimeAsc(provider.getId());
        model.addAttribute("provider", provider);
        model.addAttribute("bookings", assigned);
        model.addAttribute("pendingCount", assigned.stream().filter(b -> b.getStatus() == BookingStatus.PENDING).count());
        model.addAttribute("activeCount", assigned.stream().filter(b ->
            b.getStatus() == BookingStatus.ACCEPTED || b.getStatus() == BookingStatus.IN_PROGRESS).count());
        model.addAttribute("completedCount", assigned.stream().filter(b -> b.getStatus() == BookingStatus.COMPLETED).count());
        return "provider/dashboard";
    }

    @PostMapping("/bookings/{id}/status")
    public String updateStatus(
        @PathVariable Long id,
        @RequestParam String status,
        Authentication authentication,
        RedirectAttributes redirect
    ) {
        ProviderProfile provider = currentProvider(authentication);
        var bookingResult = bookings.findByIdAndProviderProfile_Id(id, provider.getId());
        if (bookingResult.isEmpty()) return "redirect:/provider";
        Booking booking = bookingResult.get();
        BookingStatus next;
        try {
            next = BookingStatus.valueOf(status);
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", "That booking status change is not allowed.");
            return "redirect:/provider";
        }

        boolean allowed =
            (booking.getStatus() == BookingStatus.PENDING &&
                (next == BookingStatus.ACCEPTED || next == BookingStatus.CANCELLED)) ||
            (booking.getStatus() == BookingStatus.ACCEPTED && next == BookingStatus.IN_PROGRESS) ||
            (booking.getStatus() == BookingStatus.IN_PROGRESS && next == BookingStatus.COMPLETED);
        if (!allowed) {
            redirect.addFlashAttribute("error", "That booking status change is not allowed.");
            return "redirect:/provider";
        }
        booking.setStatus(next);
        bookings.save(booking);
        redirect.addFlashAttribute("success", next == BookingStatus.CANCELLED
            ? "Booking rejected." : "Booking status updated.");
        return "redirect:/provider";
    }

    @GetMapping("/profile")
    public String profile(Authentication authentication, Model model) {
        ProviderProfile provider = currentProvider(authentication);
        ProviderProfileForm form = new ProviderProfileForm();
        form.setName(provider.getUser().getName());
        form.setPhone(provider.getUser().getPhone());
        form.setExperience(provider.getExperience());
        form.setAvailability(provider.getAvailability());
        model.addAttribute("form", form);
        model.addAttribute("email", provider.getUser().getEmail());
        return "provider/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(
        @Valid @ModelAttribute("form") ProviderProfileForm form,
        BindingResult result,
        Authentication authentication,
        Model model,
        RedirectAttributes redirect
    ) {
        ProviderProfile provider = currentProvider(authentication);
        if (result.hasErrors()) {
            model.addAttribute("email", provider.getUser().getEmail());
            return "provider/profile";
        }
        provider.getUser().setName(form.getName().trim());
        provider.getUser().setPhone(form.getPhone().trim());
        provider.setExperience(form.getExperience());
        provider.setAvailability(form.getAvailability().trim());
        providers.save(provider);
        users.save(provider.getUser());
        redirect.addFlashAttribute("success", "Provider profile updated successfully.");
        return "redirect:/provider/profile";
    }

    private ProviderProfile currentProvider(Authentication authentication) {
        var user = users.findByEmailIgnoreCase(authentication.getName())
            .orElseThrow(() -> new IllegalStateException("Signed-in account no longer exists."));
        return providers.findByUser_Id(user.getId())
            .orElseThrow(() -> new IllegalStateException("Provider profile not found."));
    }
}
