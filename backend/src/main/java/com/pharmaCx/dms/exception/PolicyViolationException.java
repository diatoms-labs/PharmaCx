package com.pharmaCx.dms.exception;

import com.pharmaCx.dms.api.dto.ContactInfo;

/**
 * Thrown when a policy check fails. Carries structured contact info
 * so the caller can surface a helpful error to the user.
 */
public class PolicyViolationException extends AppException {

    private final String code;
    private final String detail;
    private final ContactInfo contact;

    public PolicyViolationException(String code, String message, String detail, ContactInfo contact) {
        super(message);
        this.code = code;
        this.detail = detail;
        this.contact = contact;
    }

    public PolicyViolationException(String code, String message, String detail) {
        this(code, message, detail, null);
    }

    public String getCode()         { return code; }
    public String getDetail()       { return detail; }
    public ContactInfo getContact() { return contact; }
}
