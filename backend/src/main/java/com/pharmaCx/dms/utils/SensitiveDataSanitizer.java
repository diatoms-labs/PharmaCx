package com.pharmaCx.dms.utils;

import java.util.regex.Pattern;

/**
 * High-performance utility to redact sensitive data (PII/PHI) before 
 * data leaves the corporate network to cloud AI providers.
 */
public class SensitiveDataSanitizer {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}", Pattern.CASE_INSENSITIVE);
    
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "\\b(\\+\\d{1,2}\\s*)?(\\(\\d{3}\\)|\\d{3})[\\s.-]?\\d{3}[\\s.-]?\\d{4}\\b");

    private static final Pattern SSN_PATTERN = Pattern.compile(
        "\\b\\d{3}-\\d{2}-\\d{4}\\b");

    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile(
        "\\b(?:\\d[ -]?){13,16}\\b");

    /**
     * Redacts sensitive information from the input text.
     * Use this before sending any data to a Cloud AI provider.
     */
    public static String sanitize(String text) {
        if (text == null || text.isBlank()) return text;

        String sanitized = text;
        sanitized = EMAIL_PATTERN.matcher(sanitized).replaceAll("[REDACTED_EMAIL]");
        sanitized = PHONE_PATTERN.matcher(sanitized).replaceAll("[REDACTED_PHONE]");
        sanitized = SSN_PATTERN.matcher(sanitized).replaceAll("[REDACTED_ID]");
        sanitized = CREDIT_CARD_PATTERN.matcher(sanitized).replaceAll("[REDACTED_FINANCIAL]");
        
        return sanitized;
    }
}
