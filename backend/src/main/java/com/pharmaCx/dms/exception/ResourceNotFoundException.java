package com.pharmaCx.dms.exception;

public class ResourceNotFoundException extends AppException {

    public ResourceNotFoundException(String resourceType, String identifier) {
        super("The requested " + resourceType.toLowerCase() + " could not be found (ID: " + identifier + "). It may have been deleted or you may not have access.");
    }
}
