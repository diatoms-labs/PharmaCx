package com.pharmaCx.dms.service;

import com.pharmaCx.dms.domain.enums.AuditAction;
import com.pharmaCx.dms.domain.enums.ResourceType;
import com.pharmaCx.dms.domain.enums.UserRole;
import com.pharmaCx.dms.domain.model.AuditEvent;
import com.pharmaCx.dms.security.CurrentUserService;
import com.pharmaCx.dms.security.UserPrincipal;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Publishes audit events as Spring application events.
 * Decouples audit logging from service logic — services fire events
 * and the AuditEventListener persists them asynchronously.
 */
@Component
public class AuditEventPublisher {

    private final ApplicationEventPublisher publisher;
    private final CurrentUserService currentUserService;

    public AuditEventPublisher(ApplicationEventPublisher publisher,
                               CurrentUserService currentUserService) {
        this.publisher = publisher;
        this.currentUserService = currentUserService;
    }

    /** Publish audit event for the currently authenticated user. */
    public void publish(AuditAction action, ResourceType resourceType,
                        String resourceId, String resourceName) {
        publish(action, resourceType, resourceId, resourceName, null);
    }

    /** Publish audit event with reason for the currently authenticated user. */
    public void publish(AuditAction action, ResourceType resourceType,
                        String resourceId, String resourceName, String reason) {
        AuditEvent event = buildEvent(action, resourceType, resourceId, resourceName, reason);
        try {
            UserPrincipal user = currentUserService.getCurrentUser();
            event.setUserId(user.getUserId());
            event.setUsername(user.getUsername());
            event.setUserRole(UserRole.valueOf(user.getRole()));
        } catch (Exception ignored) {
            // Not authenticated — system event
        }
        publisher.publishEvent(event);
    }

    /** Publish a system-level audit event (no authenticated user). */
    public void publishSystem(AuditAction action, ResourceType resourceType,
                              String resourceId, String resourceName) {
        AuditEvent event = buildEvent(action, resourceType, resourceId, resourceName, null);
        event.setUserId("SYSTEM");
        event.setUsername("System");
        publisher.publishEvent(event);
    }

    /** Publish an IP-related audit event with explicit user info. */
    public void publishIpEvent(AuditAction action, String userId, String username,
                               UserRole role, String unitId, String ipAddress,
                               ResourceType resourceType, String resourceId, String resourceName) {
        AuditEvent event = buildEvent(action, resourceType, resourceId, resourceName, null);
        event.setUserId(userId);
        event.setUsername(username);
        event.setUserRole(role);
        event.setUserUnitId(unitId);
        event.setIpAddress(ipAddress);
        publisher.publishEvent(event);
    }

    private AuditEvent buildEvent(AuditAction action, ResourceType resourceType,
                                  String resourceId, String resourceName, String reason) {
        AuditEvent event = new AuditEvent();
        event.setTimestamp(Instant.now());
        event.setAction(action);
        event.setResourceType(resourceType);
        event.setResourceId(resourceId);
        event.setResourceName(resourceName);
        event.setReason(reason);
        return event;
    }
}
