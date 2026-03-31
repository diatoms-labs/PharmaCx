package com.pharmaCx.dms.api.controller;

import com.pharmaCx.dms.domain.model.AppUser;
import com.pharmaCx.dms.domain.repository.AppUserRepository;
import com.pharmaCx.dms.exception.ResourceNotFoundException;
import com.pharmaCx.dms.service.TrainingEligibilityService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final AppUserRepository userRepo;
    private final TrainingEligibilityService eligibilityService;

    public UserController(AppUserRepository userRepo, TrainingEligibilityService eligibilityService) {
        this.userRepo = userRepo;
        this.eligibilityService = eligibilityService;
    }

    @GetMapping
    public ResponseEntity<List<AppUser>> getAll() {
        return ResponseEntity.ok(userRepo.findAll());
    }

    // Filter by organizational unit ID
    @GetMapping("/unit/{unitId}")
    public ResponseEntity<List<AppUser>> getByUnit(@PathVariable String unitId) {
        return ResponseEntity.ok(userRepo.findByUnitIdAndActiveTrue(unitId));
    }

    // Eligible reviewers for a document type (by document type config ID)
    @GetMapping("/eligible/{documentTypeId}")
    public ResponseEntity<List<AppUser>> getEligible(@PathVariable String documentTypeId) {
        return ResponseEntity.ok(eligibilityService.getEligibleUsers(documentTypeId));
    }

    /**
     * PATCH /users/{id}/editor-permissions
     * SYSTEM_ADMIN sets per-user download/print/upload capabilities.
     */
    @PatchMapping("/{id}/editor-permissions")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<AppUser> updateEditorPermissions(@PathVariable String id,
                                                           @RequestBody EditorPermissionsRequest req) {
        AppUser user = userRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        AppUser.EditorPermissions ep = user.getEditorPermissions() != null
                ? user.getEditorPermissions() : new AppUser.EditorPermissions();
        ep.setCanDownload(req.canDownload());
        ep.setCanPrint(req.canPrint());
        ep.setCanUpload(req.canUpload());
        user.setEditorPermissions(ep);
        return ResponseEntity.ok(userRepo.save(user));
    }

    public record EditorPermissionsRequest(boolean canDownload, boolean canPrint, boolean canUpload) {}
}
