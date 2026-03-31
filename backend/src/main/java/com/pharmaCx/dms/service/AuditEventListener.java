package com.pharmaCx.dms.service;

import com.pharmaCx.dms.domain.model.AuditEvent;
import com.pharmaCx.dms.domain.repository.AuditEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listens for AuditEvent Spring application events and persists them to MongoDB.
 * Runs asynchronously so audit logging never blocks the request thread.
 */
@Component
public class AuditEventListener {

    private static final Logger log = LoggerFactory.getLogger(AuditEventListener.class);

    private final AuditEventRepository auditRepo;

    public AuditEventListener(AuditEventRepository auditRepo) {
        this.auditRepo = auditRepo;
    }

    @Async
    @EventListener
    public void onAuditEvent(AuditEvent event) {
        try {
            auditRepo.save(event);
        } catch (Exception e) {
            log.error("Failed to persist audit event [{}] for resource {}: {}",
                    event.getAction(), event.getResourceId(), e.getMessage(), e);
        }
    }
}
