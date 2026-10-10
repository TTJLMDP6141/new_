# Urban Services — Spring Boot edition

Urban Services is a home-service booking app built with Spring Boot MVC, Thymeleaf, Spring Security, Spring Data JPA, and an embedded H2 database. The supplied archive contains this Spring Boot project; it does not include the original .NET source for a direct feature-by-feature comparison.

## Requirements

- JDK 17 or newer
- Apache Maven 3.8+

## Run from VS Code

1. Open the `UrbanServicesSpringBoot` folder in VS Code.
2. Install the **Extension Pack for Java** if VS Code asks for Java support.
3. Open **Terminal → New Terminal**.
4. Run:

   ```bash
   mvn spring-boot:run
   ```

5. Open [http://localhost:8080](http://localhost:8080).

The app creates the H2 database file `urbanservices.mv.db` in this folder and inserts the sample service catalog on first startup. The database is local and persists when the app restarts.

## Run in Replit

The project workflow starts this Spring Boot app with `mvn spring-boot:run`. Its preview listens on port 8080. To run commands manually, open a shell in this folder and use:

```bash
mvn test
mvn spring-boot:run
```

To build a runnable JAR:

```bash
mvn package
java -jar target/urban-services-1.0.0.jar
```

## Demo accounts without checked-in passwords

Demo users are optional. Their emails and shared password are read from local environment variables; none of those values are stored in this repository or displayed on the sign-in page.

PowerShell:

```powershell
$env:URBAN_SERVICES_DEMO_PASSWORD = "choose-a-local-demo-password"
$env:URBAN_SERVICES_DEMO_ADMIN_EMAIL = "admin@example.test"
$env:URBAN_SERVICES_DEMO_CUSTOMER_EMAIL = "customer@example.test"
$env:URBAN_SERVICES_DEMO_PROVIDER_EMAIL = "provider@example.test"
mvn spring-boot:run
```

macOS/Linux:

```bash
export URBAN_SERVICES_DEMO_PASSWORD="choose-a-local-demo-password"
export URBAN_SERVICES_DEMO_ADMIN_EMAIL="admin@example.test"
export URBAN_SERVICES_DEMO_CUSTOMER_EMAIL="customer@example.test"
export URBAN_SERVICES_DEMO_PROVIDER_EMAIL="provider@example.test"
mvn spring-boot:run
```

When all four values are set, the app creates three accounts if they do not already exist, using the email addresses supplied in those variables. Use a password you choose for local testing; do not commit it or reuse a personal password. Without these variables, customers and providers can still register, and sample services are still seeded. A new admin account is not created automatically.

## H2 database console

To enable the H2 console for local development, set `H2_CONSOLE_ENABLED=true` before starting the app. It is disabled by default. When enabled, open [http://localhost:8080/h2-console](http://localhost:8080/h2-console).

- JDBC URL: `jdbc:h2:file:./urbanservices`
- User: `sa`
- Password: leave blank

The console is intended for local development only. Keep it disabled on a public server.

## Features

- Public service catalog, search by name/category/description, service details, and reviews.
- Customer and provider self-registration, BCrypt password hashing, sign-in/sign-out, and role-protected pages.
- Customer appointment booking, history, cancellation, profile edits, and one review after a completed booking.
- Provider profile, assigned-job dashboard, accept/reject, start-work, and complete-work actions.
- Admin dashboard, customer/provider lists, provider assignment, booking status management, and service create/edit/delete.
- H2 persistence with JPA relationships and sample data loaded on first startup.

## Main folders

- `src/main/java/com/urbanservices/model/` — JPA entities and booking/role enums.
- `src/main/java/com/urbanservices/repository/` — Spring Data repositories.
- `src/main/java/com/urbanservices/security/` — Spring Security, login lookup, and password encoder.
- `src/main/java/com/urbanservices/data/` — initial sample services and optional demo users.
- `src/main/java/com/urbanservices/web/` — MVC controllers.
- `src/main/java/com/urbanservices/web/form/` — validated form objects.
- `src/main/resources/templates/` — Thymeleaf pages.
- `src/main/resources/static/css/` — application styles.
- `src/main/resources/application.properties` — H2, port, and local demo seeding configuration.

## Booking lifecycle

1. A customer selects an active service and requests a date, time, contact number, and address. The new booking starts as **Pending**.
2. An admin assigns a provider.
3. The provider can accept or reject a pending booking. Accepted work can move to **In Progress**, then **Completed**.
4. A customer can cancel a **Pending** or **Accepted** booking, or review a completed booking once.

This is a demonstration app, not a production-ready public service. Before using real customers, add production account recovery, email verification, login rate limiting, privacy protections, backups, and monitoring. Do not expose the development H2 console publicly.
