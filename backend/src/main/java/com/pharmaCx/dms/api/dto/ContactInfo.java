package com.pharmaCx.dms.api.dto;

public class ContactInfo {

    private String reason;
    private String name;
    private String role;
    private String email;

    public ContactInfo() {}

    public ContactInfo(String reason, String name, String role, String email) {
        this.reason = reason;
        this.name = name;
        this.role = role;
        this.email = email;
    }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
