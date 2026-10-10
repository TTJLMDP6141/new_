package com.urbanservices.web.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterForm {
    @NotBlank @Size(min = 2, max = 80)
    private String name = "";

    @NotBlank @Email @Size(max = 180)
    private String email = "";

    @NotBlank @Size(min = 7, max = 24)
    private String phone = "";

    @NotBlank @Size(min = 8, max = 72)
    private String password = "";

    @NotBlank
    private String confirmPassword = "";

    @NotBlank
    private String role = "CUSTOMER";

    @Min(0) @Max(60)
    private int experience;

    @Size(max = 200)
    private String availability = "Monday to Saturday";

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public int getExperience() { return experience; }
    public void setExperience(int experience) { this.experience = experience; }
    public String getAvailability() { return availability; }
    public void setAvailability(String availability) { this.availability = availability; }
}
