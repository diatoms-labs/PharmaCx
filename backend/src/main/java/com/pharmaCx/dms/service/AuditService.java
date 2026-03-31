package com.pharmaCx.dms.service;

import com.pharmaCx.dms.domain.enums.AuditAction;
import com.pharmaCx.dms.domain.enums.ResourceType;
import com.pharmaCx.dms.domain.enums.UserRole;
import com.pharmaCx.dms.domain.model.AuditEvent;
import com.pharmaCx.dms.domain.repository.AuditEventRepository;
import com.pharmaCx.dms.domain.enums.DocumentStatus;
import com.pharmaCx.dms.domain.repository.ControlledDocumentRepository;
import com.pharmaCx.dms.security.CurrentUserService;
import com.pharmaCx.dms.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class AuditService {

    private final AuditEventRepository auditRepo;
    private final ControlledDocumentRepository docRepo;
    private final CurrentUserService currentUserService;

    public AuditService(AuditEventRepository auditRepo, 
                        ControlledDocumentRepository docRepo,
                        CurrentUserService currentUserService) {
        this.auditRepo = auditRepo;
        this.docRepo = docRepo;
        this.currentUserService = currentUserService;
    }

    public void log(AuditAction action, ResourceType resourceType, String resourceId, String resourceName, String reason) {
        UserPrincipal user = currentUserService.getCurrentUser();
        AuditEvent event = new AuditEvent();
        event.setTimestamp(Instant.now());
        event.setUserId(user.getUserId());
        event.setUsername(user.getUsername());
        event.setUserRole(UserRole.valueOf(user.getRole()));
        event.setAction(action);
        event.setResourceType(resourceType);
        event.setResourceId(resourceId);
        event.setResourceName(resourceName);
        
        // Tag external knowledge interactions
        if (resourceType == ResourceType.DOCUMENT && resourceId != null) {
            docRepo.findById(resourceId).ifPresent(doc -> {
                if (doc.getStatus() == DocumentStatus.EXTERNAL_KNOWLEDGE) {
                    event.setDetails("[EXTERNAL KNOWLEDGE] This document is not managed and was not processed by this application. " + (reason != null ? reason : ""));
                }
            });
        }
        
        event.setReason(reason);
        auditRepo.save(event);
    }

    public void logSystem(AuditAction action, ResourceType resourceType, String resourceId, String resourceName) {
        AuditEvent event = new AuditEvent();
        event.setTimestamp(Instant.now());
        event.setUserId("SYSTEM");
        event.setUsername("System");
        event.setAction(action);
        event.setResourceType(resourceType);
        event.setResourceId(resourceId);
        event.setResourceName(resourceName);
        auditRepo.save(event);
    }

    public Page<AuditEvent> getAll(Pageable pageable) {
        return auditRepo.findByOrderByTimestampDesc(pageable);
    }

    public List<AuditEvent> getByResource(String resourceId) {
        return auditRepo.findByResourceIdOrderByTimestampDesc(resourceId);
    }

    public List<AuditEvent> getByUser(String userId) {
        return auditRepo.findByUserIdOrderByTimestampDesc(userId);
    }

    public List<AuditEvent> getByDateRange(Instant from, Instant to) {
        return auditRepo.findByTimestampBetweenOrderByTimestampDesc(from, to);
    }

    public Page<AuditEvent> getByAction(AuditAction action, Pageable pageable) {
        return auditRepo.findByActionOrderByTimestampDesc(action, pageable);
    }

    public List<AuditEvent> getRecent() {
        String userId = currentUserService.getCurrentUserId();
        return auditRepo.findTop20ByUserIdOrderByTimestampDesc(userId);
    }
}
