package com.urbanservices.web.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public class BookingForm {
    @NotNull
    private Long serviceId;

    @NotNull
    private LocalDate bookingDate = LocalDate.now().plusDays(1);

    @NotBlank
    private String bookingTime = "";

    @NotBlank @Size(min = 8, max = 300)
    private String address = "";

    @NotBlank @Size(min = 7, max = 24)
    private String phone = "";

    public Long getServiceId() { return serviceId; }
    public void setServiceId(Long serviceId) { this.serviceId = serviceId; }
    public LocalDate getBookingDate() { return bookingDate; }
    public void setBookingDate(LocalDate bookingDate) { this.bookingDate = bookingDate; }
    public String getBookingTime() { return bookingTime; }
    public void setBookingTime(String bookingTime) { this.bookingTime = bookingTime; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
