package com.pharmaCx.dms.security;

import com.pharmaCx.dms.api.dto.AccessDeniedResponse;
import com.pharmaCx.dms.api.dto.PolicyDecision;
import com.pharmaCx.dms.domain.enums.AuditAction;
import com.pharmaCx.dms.domain.enums.ResourceType;
import com.pharmaCx.dms.domain.enums.UserRole;
import com.pharmaCx.dms.domain.model.AppUser;
import com.pharmaCx.dms.domain.repository.AppUserRepository;
import com.pharmaCx.dms.service.AuditEventPublisher;
import com.pharmaCx.dms.service.PolicyEnforcementService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Runs after JwtAuthFilter. For authenticated requests, checks whether
 * the request IP is in the user's allowed IP list.
 * If blocked, writes a structured 403 JSON response and halts the chain.
 */
@Component
public class IpPolicyFilter extends OncePerRequestFilter {

    private final PolicyEnforcementService policyService;
    private final AuditEventPublisher auditPublisher;
    private final AppUserRepository userRepo;
    private final ObjectMapper objectMapper;

    public IpPolicyFilter(PolicyEnforcementService policyService,
                          AuditEventPublisher auditPublisher,
                          AppUserRepository userRepo,
                          ObjectMapper objectMapper) {
        this.policyService = policyService;
        this.auditPublisher = auditPublisher;
        this.userRepo = userRepo;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Only check authenticated users
        if (auth == null || !auth.isAuthenticated() || !(auth.getDetails() instanceof UserPrincipal principal)) {
            filterChain.doFilter(request, response);
            return;
        }

        String userId = principal.getUserId();
        String ipAddress = extractIp(request);

        PolicyDecision decision = policyService.checkIpAccess(userId, ipAddress);

        AppUser user = userRepo.findById(userId).orElse(null);
        UserRole userRole = user != null ? user.getRole() : UserRole.valueOf(principal.getRole());
        String userUnitId = user != null ? user.getUnitId() : null;

        if (decision.isDenied()) {
            auditPublisher.publishIpEvent(
                    AuditAction.IP_ACCESS_BLOCKED, userId, principal.getUsername(),
                    userRole, userUnitId, ipAddress,
                    ResourceType.SYSTEM, null,
                    "IP blocked for " + principal.getUsername() + " from " + ipAddress
            );
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            AccessDeniedResponse body = new AccessDeniedResponse(
                    decision.getCode(), "IP address not permitted",
                    decision.getReason(), decision.getContact()
            );
            response.getWriter().write(objectMapper.writeValueAsString(body));
            return;
        }

        filterChain.doFilter(request, response);
    }

    /** Extract the real client IP, respecting X-Forwarded-For proxies. */
    private String extractIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) return realIp.trim();
        return request.getRemoteAddr();
    }
}
