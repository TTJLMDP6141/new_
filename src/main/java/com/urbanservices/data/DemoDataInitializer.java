package com.urbanservices.data;

import com.urbanservices.model.ProviderProfile;
import com.urbanservices.model.ServiceOffering;
import com.urbanservices.model.UserAccount;
import com.urbanservices.model.UserRole;
import com.urbanservices.repository.ProviderProfileRepository;
import com.urbanservices.repository.ServiceOfferingRepository;
import com.urbanservices.repository.UserAccountRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DemoDataInitializer implements CommandLineRunner {
    private final ServiceOfferingRepository services;
    private final UserAccountRepository users;
    private final ProviderProfileRepository providers;
    private final PasswordEncoder passwordEncoder;
    private final String demoPassword;
    private final String adminEmail;
    private final String customerEmail;
    private final String providerEmail;

    public DemoDataInitializer(
        ServiceOfferingRepository services,
        UserAccountRepository users,
        ProviderProfileRepository providers,
        PasswordEncoder passwordEncoder,
        @Value("${app.demo-seed-password:}") String demoPassword,
        @Value("${app.demo-admin-email:}") String adminEmail,
        @Value("${app.demo-customer-email:}") String customerEmail,
        @Value("${app.demo-provider-email:}") String providerEmail
    ) {
        this.services = services;
        this.users = users;
        this.providers = providers;
        this.passwordEncoder = passwordEncoder;
        this.demoPassword = demoPassword;
        this.adminEmail = adminEmail;
        this.customerEmail = customerEmail;
        this.providerEmail = providerEmail;
    }

    @Override
    public void run(String... args) {
        if (services.count() == 0) {
            services.saveAll(List.of(
                service("Home Cleaning", "Cleaning", "A careful top-to-bottom clean for your living spaces, bedrooms and kitchen.", "499", 120),
                service("Bathroom Cleaning", "Cleaning", "Deep scrubbing and sanitizing for tiles, fixtures and hard-to-reach corners.", "299", 60),
                service("AC Repair", "Repair", "Diagnosis and repair for common air-conditioner cooling and electrical issues.", "399", 90),
                service("Plumbing", "Repair", "Help with leaking taps, blocked drains and everyday plumbing repairs.", "349", 60),
                service("Electrician", "Repair", "Safe help with switches, lights, fans and other household electrical needs.", "299", 60),
                service("Salon at Home", "Personal Care", "Book a convenient grooming appointment in the comfort of your home.", "599", 90),
                service("Pest Control", "Home Care", "A home inspection and targeted treatment to help keep common pests away.", "699", 120),
                service("Car Cleaning", "Cleaning", "A thorough exterior wash and interior refresh at your doorstep.", "399", 60),
                service("Appliance Repair", "Repair", "Troubleshooting for common problems with home appliances.", "449", 90),
                service("Painting", "Home Care", "Get help with touch-ups and small interior painting jobs.", "899", 180)
            ));
        }

        // Demo account identities and password come from local environment variables,
        // not from the source repository.
        if (isBlank(demoPassword) || isBlank(adminEmail) || isBlank(customerEmail) || isBlank(providerEmail)) return;
        addDemoUser("Demo Administrator", adminEmail, "9000000001", UserRole.ADMIN, 0);
        addDemoUser("Demo Customer", customerEmail, "9000000002", UserRole.CUSTOMER, 0);
        addDemoUser("Demo Provider", providerEmail, "9000000003", UserRole.PROVIDER, 4);
    }

    private ServiceOffering service(String name, String category, String description, String price, int minutes) {
        ServiceOffering item = new ServiceOffering();
        item.setName(name);
        item.setCategory(category);
        item.setDescription(description);
        item.setPrice(new BigDecimal(price));
        item.setDurationMinutes(minutes);
        item.setActive(true);
        return item;
    }

    private void addDemoUser(String name, String email, String phone, UserRole role, int experience) {
        if (users.existsByEmailIgnoreCase(email)) return;
        UserAccount user = new UserAccount();
        user.setName(name);
        user.setEmail(email.toLowerCase());
        user.setPhone(phone);
        user.setRole(role);
        user.setPasswordHash(passwordEncoder.encode(demoPassword));
        users.save(user);

        if (role == UserRole.PROVIDER) {
            ProviderProfile profile = new ProviderProfile();
            profile.setUser(user);
            profile.setExperience(experience);
            profile.setAvailability("Monday to Saturday, 9:00 AM to 6:00 PM");
            providers.save(profile);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
