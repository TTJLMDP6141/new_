package com.urbanservices.web.form;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ProviderProfileForm {
    @NotBlank @Size(min = 2, max = 80)
    private String name = "";

    @NotBlank @Size(min = 7, max = 24)
    private String phone = "";

    @Min(0) @Max(60)
    private int experience;

    @NotBlank @Size(max = 200)
    private String availability = "";

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public int getExperience() { return experience; }
    public void setExperience(int experience) { this.experience = experience; }
    public String getAvailability() { return availability; }
    public void setAvailability(String availability) { this.availability = availability; }
}
