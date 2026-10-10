package com.urbanservices.web.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ProfileForm {
    @NotBlank @Size(min = 2, max = 80)
    private String name = "";

    @NotBlank @Email @Size(max = 180)
    private String email = "";

    @NotBlank @Size(min = 7, max = 24)
    private String phone = "";

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
