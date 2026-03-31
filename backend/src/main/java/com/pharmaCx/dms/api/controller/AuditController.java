package com.pharmaCx.dms.api.controller;

import com.pharmaCx.dms.domain.enums.AuditAction;
import com.pharmaCx.dms.domain.model.AuditEvent;
import com.pharmaCx.dms.service.AuditService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public ResponseEntity<Page<AuditEvent>> getAll(Pageable pageable) {
        return ResponseEntity.ok(auditService.getAll(pageable));
    }

    @GetMapping("/resource/{resourceId}")
    public ResponseEntity<List<AuditEvent>> getByResource(@PathVariable String resourceId) {
        return ResponseEntity.ok(auditService.getByResource(resourceId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AuditEvent>> getByUser(@PathVariable String userId) {
        return ResponseEntity.ok(auditService.getByUser(userId));
    }

    @GetMapping("/action/{action}")
    public ResponseEntity<Page<AuditEvent>> getByAction(@PathVariable AuditAction action, Pageable pageable) {
        return ResponseEntity.ok(auditService.getByAction(action, pageable));
    }

    @GetMapping("/recent")
    public ResponseEntity<List<AuditEvent>> getRecent() {
        return ResponseEntity.ok(auditService.getRecent());
    }

    @GetMapping("/range")
    public ResponseEntity<List<AuditEvent>> getByDateRange(
            @RequestParam Instant from, @RequestParam Instant to) {
        return ResponseEntity.ok(auditService.getByDateRange(from, to));
    }
}
