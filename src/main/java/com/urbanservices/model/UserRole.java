package com.urbanservices.model;

public enum UserRole {
    CUSTOMER("Customer"),
    PROVIDER("Service Provider"),
    ADMIN("Admin");

    private final String label;

    UserRole(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
