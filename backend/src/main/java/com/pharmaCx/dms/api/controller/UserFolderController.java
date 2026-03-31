package com.pharmaCx.dms.api.controller;

import com.pharmaCx.dms.api.dto.AccessDeniedResponse;
import com.pharmaCx.dms.api.dto.PolicyDecision;
import com.pharmaCx.dms.domain.model.AppUser;
import com.pharmaCx.dms.domain.model.FolderPolicy;
import com.pharmaCx.dms.domain.model.UserFolder;
import com.pharmaCx.dms.domain.repository.AppUserRepository;
import com.pharmaCx.dms.domain.repository.FolderPolicyRepository;
import com.pharmaCx.dms.domain.repository.UserFolderRepository;
import com.pharmaCx.dms.exception.ResourceNotFoundException;
import com.pharmaCx.dms.security.CurrentUserService;
import com.pharmaCx.dms.service.PolicyEnforcementService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/folders")
public class UserFolderController {

    private final UserFolderRepository folderRepo;
    private final FolderPolicyRepository folderPolicyRepo;
    private final AppUserRepository userRepo;
    private final CurrentUserService currentUserService;
    private final PolicyEnforcementService policyService;

    public UserFolderController(UserFolderRepository folderRepo,
                                FolderPolicyRepository folderPolicyRepo,
                                AppUserRepository userRepo,
                                CurrentUserService currentUserService,
                                PolicyEnforcementService policyService) {
        this.folderRepo = folderRepo;
        this.folderPolicyRepo = folderPolicyRepo;
        this.userRepo = userRepo;
        this.currentUserService = currentUserService;
        this.policyService = policyService;
    }

    // ── Read ────────────────────────────────────────────────────────────────────

    @GetMapping("/my")
    public ResponseEntity<List<UserFolder>> getMyFolders() {
        String userId = currentUserService.getCurrentUserId();
        return ResponseEntity.ok(folderRepo.findByOwnerIdAndParentFolderIdIsNullOrderByCreatedAtDesc(userId));
    }

    @GetMapping("/{id}/children")
    public ResponseEntity<?> getSubfolders(@PathVariable String id) {
        UserFolder folder = folderRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Folder", id));
        ResponseEntity<?> denied = checkAccess(folder);
        if (denied != null) return denied;
        String userId = currentUserService.getCurrentUserId();
        return ResponseEntity.ok(folderRepo.findByOwnerIdAndParentFolderIdOrderByCreatedAtDesc(userId, id));
    }

    @GetMapping("/shared")
    public ResponseEntity<List<UserFolder>> getSharedFolders() {
        String userId = currentUserService.getCurrentUserId();
        return ResponseEntity.ok(folderRepo.findBySharedWithUserIdsContainingOrderByCreatedAtDesc(userId));
    }

    @GetMapping("/unit/{unitId}")
    public ResponseEntity<List<UserFolder>> getUnitFolders(@PathVariable String unitId) {
        return ResponseEntity.ok(folderRepo.findByOwnerUnitIdAndSharedWithAllTrueOrderByCreatedAtDesc(unitId));
    }

    @GetMapping("/shared-all")
    public ResponseEntity<List<UserFolder>> getAllSharedFolders() {
        return ResponseEntity.ok(folderRepo.findBySharedWithAllTrueOrderByCreatedAtDesc());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getFolder(@PathVariable String id) {
        UserFolder folder = folderRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Folder", id));
        ResponseEntity<?> denied = checkAccess(folder);
        if (denied != null) return denied;
        return ResponseEntity.ok(folder);
    }

    // ── Write ───────────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<UserFolder> createFolder(@RequestBody Map<String, String> body) {
        String userId = currentUserService.getCurrentUserId();
        String username = currentUserService.getCurrentUsername();

        UserFolder folder = new UserFolder();
        folder.setName(body.get("name"));
        folder.setOwnerId(userId);
        folder.setOwnerUsername(username);
        folder.setParentFolderId(body.get("parentFolderId"));
        if (body.get("ownerUnitId") != null) {
            folder.setOwnerUnitId(body.get("ownerUnitId"));
            folder.setSharedWithAll("true".equals(body.get("sharedWithAll")));
        }
        folder.setCreatedAt(Instant.now());
        folder.setUpdatedAt(Instant.now());
        return ResponseEntity.ok(folderRepo.save(folder));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> renameFolder(@PathVariable String id, @RequestBody Map<String, String> body) {
        String userId = currentUserService.getCurrentUserId();
        UserFolder folder = folderRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Folder", id));
        if (!folder.getOwnerId().equals(userId)) return ResponseEntity.status(403).build();
        folder.setName(body.get("name"));
        folder.setUpdatedAt(Instant.now());
        return ResponseEntity.ok(folderRepo.save(folder));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFolder(@PathVariable String id) {
        String userId = currentUserService.getCurrentUserId();
        UserFolder folder = folderRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Folder", id));
        if (!folder.getOwnerId().equals(userId)) return ResponseEntity.status(403).build();
        folderRepo.delete(folder);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/share")
    public ResponseEntity<?> shareFolder(@PathVariable String id, @RequestBody Map<String, List<String>> body) {
        String userId = currentUserService.getCurrentUserId();
        UserFolder folder = folderRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Folder", id));
        if (!folder.getOwnerId().equals(userId)) return ResponseEntity.status(403).build();
        List<String> userIds = body.get("userIds");
        if (userIds != null) folder.setSharedWithUserIds(userIds);
        folder.setUpdatedAt(Instant.now());
        return ResponseEntity.ok(folderRepo.save(folder));
    }

    @PostMapping("/{id}/documents")
    public ResponseEntity<?> addDocument(@PathVariable String id, @RequestBody Map<String, String> body) {
        String userId = currentUserService.getCurrentUserId();
        UserFolder folder = folderRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Folder", id));
        ResponseEntity<?> denied = checkAccess(folder);
        if (denied != null) return denied;
        String documentId = body.get("documentId");
        if (documentId != null && !folder.getDocumentIds().contains(documentId)) {
            folder.getDocumentIds().add(documentId);
            folder.setUpdatedAt(Instant.now());
        }
        return ResponseEntity.ok(folderRepo.save(folder));
    }

    @DeleteMapping("/{id}/documents/{documentId}")
    public ResponseEntity<?> removeDocument(@PathVariable String id, @PathVariable String documentId) {
        String userId = currentUserService.getCurrentUserId();
        UserFolder folder = folderRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Folder", id));
        if (!folder.getOwnerId().equals(userId)) return ResponseEntity.status(403).build();
        folder.getDocumentIds().remove(documentId);
        folder.setUpdatedAt(Instant.now());
        return ResponseEntity.ok(folderRepo.save(folder));
    }

    // ── Folder Policy (admin) ───────────────────────────────────────────────────

    /** Get the policy attached to a folder */
    @GetMapping("/{id}/policy")
    public ResponseEntity<FolderPolicy> getFolderPolicy(@PathVariable String id) {
        UserFolder folder = folderRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Folder", id));
        FolderPolicy policy = folder.getPolicyId() != null
                ? folderPolicyRepo.findById(folder.getPolicyId()).orElse(null)
                : folderPolicyRepo.findByFolderId(id).orElse(null);
        if (policy == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(policy);
    }

    /** Create or replace the policy for a folder (admin or folder owner) */
    @PutMapping("/{id}/policy")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','MANAGER','HEAD_OF_DEPARTMENT','DIRECTOR')")
    public ResponseEntity<FolderPolicy> upsertFolderPolicy(@PathVariable String id,
                                                           @RequestBody PolicyRequest req) {
        UserFolder folder = folderRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Folder", id));
        String userId = currentUserService.getCurrentUserId();

        FolderPolicy policy = folderPolicyRepo.findByFolderId(id).orElseGet(FolderPolicy::new);
        policy.setFolderId(id);
        policy.setOwnerId(folder.getOwnerId());
        policy.setOwnerUnitId(req.ownerUnitId() != null ? req.ownerUnitId() : folder.getOwnerUnitId());
        policy.setAccessScope(req.accessScope() != null ? req.accessScope() : "DEPARTMENT_ONLY");
        policy.setMinimumRole(req.minimumRole() != null ? req.minimumRole() : "OPERATOR");
        policy.setAllowedUnitIds(req.allowedUnitIds() != null ? req.allowedUnitIds() : List.of());
        policy.setCreatedBy(userId);
        policy.setUpdatedAt(Instant.now());
        if (policy.getCreatedAt() == null) policy.setCreatedAt(Instant.now());

        FolderPolicy saved = folderPolicyRepo.save(policy);

        // Link policyId on the folder
        folder.setPolicyId(saved.getId());
        folder.setUpdatedAt(Instant.now());
        folderRepo.save(folder);

        return ResponseEntity.ok(saved);
    }

    /** Remove folder policy (revert to owner-only access) */
    @DeleteMapping("/{id}/policy")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','MANAGER','HEAD_OF_DEPARTMENT','DIRECTOR')")
    public ResponseEntity<Void> deleteFolderPolicy(@PathVariable String id) {
        UserFolder folder = folderRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Folder", id));
        if (folder.getPolicyId() != null) {
            folderPolicyRepo.deleteById(folder.getPolicyId());
            folder.setPolicyId(null);
            folder.setUpdatedAt(Instant.now());
            folderRepo.save(folder);
        } else {
            folderPolicyRepo.findByFolderId(id).ifPresent(folderPolicyRepo::delete);
        }
        return ResponseEntity.noContent().build();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    /**
     * Checks folder access via PolicyEnforcementService.
     * Returns a 403 ResponseEntity if denied, null if allowed.
     */
    private ResponseEntity<AccessDeniedResponse> checkAccess(UserFolder folder) {
        String userId = currentUserService.getCurrentUserId();
        AppUser user = userRepo.findById(userId).orElse(null);
        if (user == null) return ResponseEntity.status(403).build();

        PolicyDecision decision = policyService.checkFolderAccess(user, folder);
        if (decision.isDenied()) {
            return ResponseEntity.status(403).body(
                    new AccessDeniedResponse(decision.getCode(), "Folder Access Denied",
                            decision.getReason(), decision.getContact()));
        }
        return null;
    }

    public record PolicyRequest(
            String accessScope,
            String minimumRole,
            String ownerUnitId,
            List<String> allowedUnitIds
    ) {}
}
