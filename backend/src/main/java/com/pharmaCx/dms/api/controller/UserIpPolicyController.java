package com.pharmaCx.dms.api.controller;

import com.pharmaCx.dms.domain.model.UserIpPolicy;
import com.pharmaCx.dms.domain.repository.UserIpPolicyRepository;
import com.pharmaCx.dms.exception.ResourceNotFoundException;
import com.pharmaCx.dms.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * Admin API for managing per-user IP allowlist policies.
 * Only SYSTEM_ADMIN can read/write these policies.
 */
@RestController
@RequestMapping("/api/v1/ip-policies")
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class UserIpPolicyController {

    private final UserIpPolicyRepository ipPolicyRepo;

    public UserIpPolicyController(UserIpPolicyRepository ipPolicyRepo) {
        this.ipPolicyRepo = ipPolicyRepo;
    }

    /** List all IP policies */
    @GetMapping
    public ResponseEntity<List<UserIpPolicy>> listAll() {
        return ResponseEntity.ok(ipPolicyRepo.findAll());
    }

    /** Get IP policy for a specific user */
    @GetMapping("/user/{userId}")
    public ResponseEntity<UserIpPolicy> getForUser(@PathVariable String userId) {
        UserIpPolicy policy = ipPolicyRepo.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("IP Policy", userId));
        return ResponseEntity.ok(policy);
    }

    /** Upsert (create or replace) an IP policy for a user */
    @PutMapping("/user/{userId}")
    public ResponseEntity<UserIpPolicy> upsert(@PathVariable String userId,
                                               @RequestBody UpsertRequest req,
                                               @AuthenticationPrincipal UserPrincipal admin) {
        UserIpPolicy policy = ipPolicyRepo.findByUserId(userId).orElseGet(UserIpPolicy::new);
        policy.setUserId(userId);
        policy.setAllowedIps(req.allowedIps() != null ? req.allowedIps() : List.of());
        policy.setRestrictToAllowedIps(req.restrictToAllowedIps());
        policy.setExpiresAt(req.expiresAt());
        policy.setGrantedBy(admin.getUserId());
        policy.setGrantedAt(Instant.now());
        return ResponseEntity.ok(ipPolicyRepo.save(policy));
    }

    /** Delete IP policy for a user (removes all restrictions) */
    @DeleteMapping("/user/{userId}")
    public ResponseEntity<Void> deleteForUser(@PathVariable String userId) {
        ipPolicyRepo.findByUserId(userId).ifPresent(ipPolicyRepo::delete);
        return ResponseEntity.noContent().build();
    }

    public record UpsertRequest(
            List<String> allowedIps,
            boolean restrictToAllowedIps,
            Instant expiresAt
    ) {}
}
