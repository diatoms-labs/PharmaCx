package com.pharmaCx.dms.security;

import com.pharmaCx.dms.exception.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    public UserPrincipal getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof UserPrincipal) {
            return (UserPrincipal) auth.getDetails();
        }
        throw new AccessDeniedException("No authenticated user found");
    }

    public String getCurrentUserId() {
        return getCurrentUser().getUserId();
    }

    public String getCurrentUsername() {
        return getCurrentUser().getUsername();
    }
}
