package com.pharmaCx.dms.api.dto;

/**
 * Result of a policy enforcement check.
 */
public class PolicyDecision {

    public enum Result { ALLOW, DENY }

    private final Result result;
    private final String code;       // e.g. "IP_NOT_ALLOWED", "DOWNLOAD_NOT_PERMITTED"
    private final String reason;     // human-readable reason
    private final ContactInfo contact;

    private PolicyDecision(Result result, String code, String reason, ContactInfo contact) {
        this.result = result;
        this.code = code;
        this.reason = reason;
        this.contact = contact;
    }

    public static PolicyDecision allow() {
        return new PolicyDecision(Result.ALLOW, null, null, null);
    }

    public static PolicyDecision deny(String code, String reason) {
        return new PolicyDecision(Result.DENY, code, reason, null);
    }

    public static PolicyDecision deny(String code, String reason, ContactInfo contact) {
        return new PolicyDecision(Result.DENY, code, reason, contact);
    }

    public boolean isAllowed() { return result == Result.ALLOW; }
    public boolean isDenied()  { return result == Result.DENY; }

    public Result getResult()       { return result; }
    public String getCode()         { return code; }
    public String getReason()       { return reason; }
    public ContactInfo getContact() { return contact; }
}
