package com.urbanservices.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.urbanservices.model.UserAccount;
import com.urbanservices.model.UserRole;
import com.urbanservices.repository.BookingRepository;
import com.urbanservices.repository.ProviderProfileRepository;
import com.urbanservices.repository.ServiceOfferingRepository;
import com.urbanservices.repository.UserAccountRepository;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:urban-test;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.h2.console.enabled=false",
    "app.demo-seed-password="
})
class CustomerFlowTest {
    @Autowired private MockMvc mvc;
    @Autowired private ServiceOfferingRepository services;
    @Autowired private UserAccountRepository users;
    @Autowired private ProviderProfileRepository providers;
    @Autowired private BookingRepository bookings;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void publicPagesAndCatalogRender() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Make room for the things you love")));
        mvc.perform(get("/services")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Home Cleaning")));
        mvc.perform(get("/login")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Sign in to your account")));
        mvc.perform(get("/register")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Create your account")));
    }

    @Test
    void customerCanRegisterLoginBookAndCancel() throws Exception {
        String email = "customer-" + UUID.randomUUID() + "@example.test";
        mvc.perform(post("/register")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .param("name", "Test Customer")
                .param("email", email)
                .param("phone", "9000000011")
                .param("role", "CUSTOMER")
                .param("password", "LocalPass123!")
                .param("confirmPassword", "LocalPass123!"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));

        MvcResult login = mvc.perform(post("/login")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .param("username", email)
                .param("password", "LocalPass123!"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/after-login"))
            .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

        mvc.perform(get("/after-login").session(session))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/customer"));
        mvc.perform(get("/customer").session(session))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Customer space")));

        Long serviceId = services.findByActiveTrueOrderByNameAsc().get(0).getId();
        MvcResult booking = mvc.perform(post("/customer/book")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .session(session)
                .param("serviceId", serviceId.toString())
                .param("bookingDate", LocalDate.now().plusDays(1).toString())
                .param("bookingTime", "09:00 AM")
                .param("address", "123 Test Street, Sample City")
                .param("phone", "9000000011"))
            .andExpect(status().is3xxRedirection())
            .andReturn();
        String confirmationUrl = booking.getResponse().getRedirectedUrl();
        org.junit.jupiter.api.Assertions.assertNotNull(confirmationUrl);
        mvc.perform(get(confirmationUrl).session(session))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Booking submitted")));

        String bookingId = confirmationUrl.substring(confirmationUrl.lastIndexOf('/') + 1);
        mvc.perform(post("/customer/bookings/{id}/cancel", bookingId)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .session(session))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/customer/bookings"));
        mvc.perform(get("/customer/bookings").session(session))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Cancelled")));
    }

    @Test
    void customerCannotOpenAdminPages() throws Exception {
        String email = "customer-" + UUID.randomUUID() + "@example.test";
        mvc.perform(post("/register")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .param("name", "Another Customer")
                .param("email", email)
                .param("phone", "9000000012")
                .param("role", "CUSTOMER")
                .param("password", "LocalPass123!")
                .param("confirmPassword", "LocalPass123!"))
            .andExpect(status().is3xxRedirection());
        MvcResult login = mvc.perform(post("/login")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .param("username", email)
                .param("password", "LocalPass123!"))
            .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        mvc.perform(get("/admin").session(session))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminCanAssignProviderAndProviderCanCompleteWorkForReview() throws Exception {
        String customerEmail = "customer-" + UUID.randomUUID() + "@example.test";
        String providerEmail = "provider-" + UUID.randomUUID() + "@example.test";
        MockHttpSession customer = registerAndLogin(customerEmail, "CUSTOMER");
        MockHttpSession provider = registerAndLogin(providerEmail, "PROVIDER");
        Long providerId = providers.findByUser_Id(
            users.findByEmailIgnoreCase(providerEmail).orElseThrow().getId()
        ).orElseThrow().getId();

        UserAccount adminAccount = new UserAccount();
        adminAccount.setName("Test Administrator");
        adminAccount.setEmail("admin-" + UUID.randomUUID() + "@example.test");
        adminAccount.setPhone("9000000013");
        adminAccount.setPasswordHash(passwordEncoder.encode("LocalPass123!"));
        adminAccount.setRole(UserRole.ADMIN);
        users.save(adminAccount);
        MockHttpSession admin = login(adminAccount.getEmail(), "LocalPass123!");

        Long serviceId = services.findByActiveTrueOrderByNameAsc().get(0).getId();
        MvcResult bookingResult = mvc.perform(post("/customer/book")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .session(customer)
                .param("serviceId", serviceId.toString())
                .param("bookingDate", LocalDate.now().plusDays(1).toString())
                .param("bookingTime", "11:00 AM")
                .param("address", "456 Sample Road, Sample City")
                .param("phone", "9000000014"))
            .andExpect(status().is3xxRedirection())
            .andReturn();
        String confirmationUrl = bookingResult.getResponse().getRedirectedUrl();
        String bookingId = confirmationUrl.substring(confirmationUrl.lastIndexOf('/') + 1);

        mvc.perform(get("/admin").session(admin))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Administration")));
        mvc.perform(get("/admin/bookings").session(admin))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Bookings")));
        mvc.perform(post("/admin/bookings/{id}/assign", bookingId)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .session(admin)
                .param("providerId", providerId.toString()))
            .andExpect(status().is3xxRedirection());

        mvc.perform(get("/provider").session(provider))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Your assigned work")));
        updateProviderStatus(bookingId, "ACCEPTED", provider);
        updateProviderStatus(bookingId, "IN_PROGRESS", provider);
        updateProviderStatus(bookingId, "COMPLETED", provider);

        mvc.perform(get("/customer/reviews/{id}", bookingId).session(customer))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Share your experience")));
        mvc.perform(post("/customer/reviews")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .session(customer)
                .param("bookingId", bookingId)
                .param("rating", "5")
                .param("comment", "The service was excellent."))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/customer/bookings"));
        mvc.perform(get("/services/{id}", serviceId))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("The service was excellent.")));
        mvc.perform(get("/customer/reviews/{id}", bookingId).session(customer))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/customer/bookings"));
    }

    @Test
    void adminCanCreateEditAndDeleteAnUnbookedService() throws Exception {
        UserAccount adminAccount = new UserAccount();
        adminAccount.setName("Service Admin");
        adminAccount.setEmail("service-admin-" + UUID.randomUUID() + "@example.test");
        adminAccount.setPhone("9000000015");
        adminAccount.setPasswordHash(passwordEncoder.encode("LocalPass123!"));
        adminAccount.setRole(UserRole.ADMIN);
        users.save(adminAccount);
        MockHttpSession admin = login(adminAccount.getEmail(), "LocalPass123!");

        mvc.perform(get("/admin/services/new").session(admin))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Add a service")));
        mvc.perform(post("/admin/services")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .session(admin)
                .param("name", "Test Window Cleaning")
                .param("category", "Cleaning")
                .param("description", "A test service for CRUD verification.")
                .param("price", "250.00")
                .param("durationMinutes", "45")
                .param("active", "true"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/services"));

        var created = services.findAllByOrderByNameAsc().stream()
            .filter(item -> item.getName().equals("Test Window Cleaning"))
            .findFirst().orElseThrow();
        mvc.perform(get("/admin/services/{id}/edit", created.getId()).session(admin))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Edit service")));
        mvc.perform(post("/admin/services/{id}", created.getId())
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .session(admin)
                .param("name", "Updated Window Cleaning")
                .param("category", "Cleaning")
                .param("description", "Updated test service description.")
                .param("price", "275.00")
                .param("durationMinutes", "50")
                .param("active", "true"))
            .andExpect(status().is3xxRedirection());
        org.junit.jupiter.api.Assertions.assertEquals(
            "Updated Window Cleaning", services.findById(created.getId()).orElseThrow().getName()
        );
        mvc.perform(post("/admin/services/{id}/delete", created.getId())
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .session(admin))
            .andExpect(status().is3xxRedirection());
        org.junit.jupiter.api.Assertions.assertFalse(services.existsById(created.getId()));
    }

    private MockHttpSession registerAndLogin(String email, String role) throws Exception {
        mvc.perform(post("/register")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .param("name", role.equals("PROVIDER") ? "Test Provider" : "Flow Customer")
                .param("email", email)
                .param("phone", "9000000020")
                .param("role", role)
                .param("experience", "3")
                .param("availability", "Weekdays")
                .param("password", "LocalPass123!")
                .param("confirmPassword", "LocalPass123!"))
            .andExpect(status().is3xxRedirection());
        return login(email, "LocalPass123!");
    }

    private MockHttpSession login(String email, String password) throws Exception {
        MvcResult result = mvc.perform(post("/login")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .param("username", email)
                .param("password", password))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/after-login"))
            .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private void updateProviderStatus(String bookingId, String status, MockHttpSession session) throws Exception {
        mvc.perform(post("/provider/bookings/{id}/status", bookingId)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .session(session)
                .param("status", status))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/provider"));
    }
}
