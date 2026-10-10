package com.urbanservices.web;

import com.urbanservices.model.Booking;
import com.urbanservices.model.BookingStatus;
import com.urbanservices.model.Review;
import com.urbanservices.repository.BookingRepository;
import com.urbanservices.repository.ReviewRepository;
import com.urbanservices.repository.ServiceOfferingRepository;
import com.urbanservices.repository.UserAccountRepository;
import com.urbanservices.web.form.BookingForm;
import com.urbanservices.web.form.ProfileForm;
import com.urbanservices.web.form.ReviewForm;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/customer")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerController {
    private static final List<String> TIME_SLOTS =
        List.of("09:00 AM", "11:00 AM", "01:00 PM", "03:00 PM", "05:00 PM");

    private final UserAccountRepository users;
    private final ServiceOfferingRepository services;
    private final BookingRepository bookings;
    private final ReviewRepository reviews;

    public CustomerController(
        UserAccountRepository users,
        ServiceOfferingRepository services,
        BookingRepository bookings,
        ReviewRepository reviews
    ) {
        this.users = users;
        this.services = services;
        this.bookings = bookings;
        this.reviews = reviews;
    }

    @GetMapping
    public String dashboard(Authentication authentication, Model model) {
        var user = currentUser(authentication);
        var customerBookings = bookings.findByCustomer_IdOrderByCreatedAtDesc(user.getId());
        model.addAttribute("user", user);
        model.addAttribute("bookings", customerBookings.stream().limit(5).toList());
        model.addAttribute("upcomingCount", customerBookings.stream().filter(b ->
            b.getStatus() == BookingStatus.PENDING ||
            b.getStatus() == BookingStatus.ACCEPTED ||
            b.getStatus() == BookingStatus.IN_PROGRESS).count());
        model.addAttribute("completedCount", customerBookings.stream()
            .filter(b -> b.getStatus() == BookingStatus.COMPLETED).count());
        return "customer/dashboard";
    }

    @GetMapping("/book/{serviceId}")
    public String bookingForm(
        @PathVariable Long serviceId,
        Authentication authentication,
        Model model,
        RedirectAttributes redirect
    ) {
        var service = services.findByIdAndActiveTrue(serviceId);
        if (service.isEmpty()) {
            redirect.addFlashAttribute("error", "That service is not available.");
            return "redirect:/services";
        }
        BookingForm form = new BookingForm();
        form.setServiceId(serviceId);
        form.setBookingDate(LocalDate.now().plusDays(1));
        form.setPhone(currentUser(authentication).getPhone());
        model.addAttribute("form", form);
        model.addAttribute("service", service.get());
        model.addAttribute("timeSlots", TIME_SLOTS);
        return "customer/book";
    }

    @PostMapping("/book")
    public String createBooking(
        @Valid @ModelAttribute("form") BookingForm form,
        BindingResult result,
        Authentication authentication,
        Model model,
        RedirectAttributes redirect
    ) {
        var service = form.getServiceId() == null ? java.util.Optional.<com.urbanservices.model.ServiceOffering>empty()
            : services.findByIdAndActiveTrue(form.getServiceId());
        if (service.isEmpty()) {
            redirect.addFlashAttribute("error", "That service is not available.");
            return "redirect:/services";
        }
        if (form.getBookingDate() != null && form.getBookingDate().isBefore(LocalDate.now())) {
            result.rejectValue("bookingDate", "past", "Please choose today or a future date.");
        }
        if (!TIME_SLOTS.contains(form.getBookingTime())) {
            result.rejectValue("bookingTime", "invalid", "Choose one of the available time slots.");
        }
        if (result.hasErrors()) {
            model.addAttribute("service", service.get());
            model.addAttribute("timeSlots", TIME_SLOTS);
            return "customer/book";
        }

        Booking booking = new Booking();
        booking.setCustomer(currentUser(authentication));
        booking.setService(service.get());
        booking.setBookingDate(form.getBookingDate());
        booking.setBookingTime(form.getBookingTime());
        booking.setAddress(form.getAddress().trim());
        booking.setPhone(form.getPhone().trim());
        booking.setTotalPrice(service.get().getPrice());
        booking.setStatus(BookingStatus.PENDING);
        bookings.save(booking);
        redirect.addFlashAttribute("success", "Booking created successfully.");
        return "redirect:/customer/confirmation/" + booking.getId();
    }

    @GetMapping("/confirmation/{id}")
    public String confirmation(@PathVariable Long id, Authentication authentication, Model model) {
        var booking = bookings.findByIdAndCustomer_Id(id, currentUser(authentication).getId());
        if (booking.isEmpty()) return "redirect:/customer/bookings";
        model.addAttribute("booking", booking.get());
        return "customer/confirmation";
    }

    @GetMapping("/bookings")
    public String bookingHistory(Authentication authentication, Model model) {
        model.addAttribute("bookings", bookings.findByCustomer_IdOrderByCreatedAtDesc(currentUser(authentication).getId()));
        return "customer/bookings";
    }

    @PostMapping("/bookings/{id}/cancel")
    public String cancelBooking(
        @PathVariable Long id,
        Authentication authentication,
        RedirectAttributes redirect
    ) {
        var booking = bookings.findByIdAndCustomer_Id(id, currentUser(authentication).getId());
        if (booking.isEmpty()) return "redirect:/customer/bookings";
        Booking item = booking.get();
        if (item.getStatus() != BookingStatus.PENDING && item.getStatus() != BookingStatus.ACCEPTED) {
            redirect.addFlashAttribute("error", "This booking can no longer be cancelled.");
            return "redirect:/customer/bookings";
        }
        item.setStatus(BookingStatus.CANCELLED);
        bookings.save(item);
        redirect.addFlashAttribute("success", "Booking cancelled successfully.");
        return "redirect:/customer/bookings";
    }

    @GetMapping("/reviews/{bookingId}")
    public String reviewForm(
        @PathVariable Long bookingId,
        Authentication authentication,
        Model model,
        RedirectAttributes redirect
    ) {
        var booking = bookings.findByIdAndCustomer_Id(bookingId, currentUser(authentication).getId());
        if (booking.isEmpty()) return "redirect:/customer/bookings";
        if (booking.get().getStatus() != BookingStatus.COMPLETED || reviews.existsByBooking_Id(bookingId)) {
            redirect.addFlashAttribute("error", "Only completed bookings without a review can be reviewed.");
            return "redirect:/customer/bookings";
        }
        ReviewForm form = new ReviewForm();
        form.setBookingId(bookingId);
        model.addAttribute("form", form);
        model.addAttribute("booking", booking.get());
        return "customer/review";
    }

    @PostMapping("/reviews")
    public String submitReview(
        @Valid @ModelAttribute("form") ReviewForm form,
        BindingResult result,
        Authentication authentication,
        Model model,
        RedirectAttributes redirect
    ) {
        var booking = form.getBookingId() == null ? java.util.Optional.<Booking>empty()
            : bookings.findByIdAndCustomer_Id(form.getBookingId(), currentUser(authentication).getId());
        if (booking.isEmpty()) return "redirect:/customer/bookings";
        if (booking.get().getStatus() != BookingStatus.COMPLETED || reviews.existsByBooking_Id(form.getBookingId())) {
            redirect.addFlashAttribute("error", "Only completed bookings without a review can be reviewed.");
            return "redirect:/customer/bookings";
        }
        if (result.hasErrors()) {
            model.addAttribute("booking", booking.get());
            return "customer/review";
        }
        Review review = new Review();
        review.setBooking(booking.get());
        review.setUser(booking.get().getCustomer());
        review.setService(booking.get().getService());
        review.setRating(form.getRating());
        review.setComment(form.getComment().trim());
        reviews.save(review);
        redirect.addFlashAttribute("success", "Thank you for sharing your review.");
        return "redirect:/customer/bookings";
    }

    @GetMapping("/profile")
    public String profile(Authentication authentication, Model model) {
        var user = currentUser(authentication);
        model.addAttribute("form", toProfileForm(user));
        return "customer/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(
        @Valid @ModelAttribute("form") ProfileForm form,
        BindingResult result,
        Authentication authentication,
        Model model,
        RedirectAttributes redirect
    ) {
        var user = currentUser(authentication);
        String email = form.getEmail() == null ? "" : form.getEmail().trim().toLowerCase();
        var existing = users.findByEmailIgnoreCase(email);
        if (existing.isPresent() && !existing.get().getId().equals(user.getId())) {
            result.rejectValue("email", "duplicate", "This email is already in use.");
        }
        if (result.hasErrors()) return "customer/profile";
        user.setName(form.getName().trim());
        user.setEmail(email);
        user.setPhone(form.getPhone().trim());
        users.save(user);
        redirect.addFlashAttribute("success", "Profile updated successfully.");
        return "redirect:/customer/profile";
    }

    private com.urbanservices.model.UserAccount currentUser(Authentication authentication) {
        return users.findByEmailIgnoreCase(authentication.getName())
            .orElseThrow(() -> new IllegalStateException("Signed-in account no longer exists."));
    }

    private ProfileForm toProfileForm(com.urbanservices.model.UserAccount user) {
        ProfileForm form = new ProfileForm();
        form.setName(user.getName());
        form.setEmail(user.getEmail());
        form.setPhone(user.getPhone());
        return form;
    }
}
