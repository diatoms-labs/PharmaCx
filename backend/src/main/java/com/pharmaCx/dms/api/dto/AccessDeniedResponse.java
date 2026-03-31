package com.pharmaCx.dms.api.dto;

public class AccessDeniedResponse {

    private final String error = "ACCESS_DENIED";
    private String code;
    private String message;
    private String detail;
    private ContactInfo contact;

    public AccessDeniedResponse() {}

    public AccessDeniedResponse(String code, String message, String detail, ContactInfo contact) {
        this.code = code;
        this.message = message;
        this.detail = detail;
        this.contact = contact;
    }

    public String getError() { return error; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }

    public ContactInfo getContact() { return contact; }
    public void setContact(ContactInfo contact) { this.contact = contact; }
}
