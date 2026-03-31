package com.pharmaCx.dms.api.controller;

import com.pharmaCx.dms.api.dto.EditorConfigResponse;
import com.pharmaCx.dms.domain.model.DocumentTemplate;
import com.pharmaCx.dms.domain.repository.DocumentTemplateRepository;
import com.pharmaCx.dms.exception.ResourceNotFoundException;
import com.pharmaCx.dms.security.CurrentUserService;
import com.pharmaCx.dms.service.AuthService;
import com.pharmaCx.dms.service.FileStorageService;
import com.pharmaCx.dms.service.OnlyOfficeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/templates")
public class TemplateController {

    private final DocumentTemplateRepository templateRepo;
    private final FileStorageService fileStorageService;
    private final CurrentUserService currentUserService;
    private final OnlyOfficeService onlyOfficeService;
    private final AuthService authService;

    public TemplateController(DocumentTemplateRepository templateRepo, FileStorageService fileStorageService,
                              CurrentUserService currentUserService, OnlyOfficeService onlyOfficeService,
                              AuthService authService) {
        this.templateRepo = templateRepo;
        this.fileStorageService = fileStorageService;
        this.currentUserService = currentUserService;
        this.onlyOfficeService = onlyOfficeService;
        this.authService = authService;
    }

    @PostMapping
    public ResponseEntity<DocumentTemplate> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("name") String name,
            @RequestParam("documentTypeId") String documentTypeId,
            @RequestParam(value = "description", required = false) String description) throws Exception {

        String username = currentUserService.getCurrentUsername();

        String ext = getExtension(file.getOriginalFilename());
        String fileStorageId = fileStorageService.storeFile(file.getInputStream(), ext);

        int nextVersion = 1;
        List<DocumentTemplate> existing = templateRepo.findByNameAndDocumentTypeIdOrderByVersionDesc(name, documentTypeId);
        if (!existing.isEmpty()) {
            nextVersion = existing.get(0).getVersion() + 1;
            for (DocumentTemplate old : existing) {
                if (old.isLatest()) {
                    old.setLatest(false);
                    templateRepo.save(old);
                }
            }
        }

        DocumentTemplate template = new DocumentTemplate();
        template.setName(name);
        template.setDocumentTypeId(documentTypeId);
        template.setDescription(description);
        template.setFileStorageId(fileStorageId);
        template.setVersion(nextVersion);
        template.setLatest(true);
        template.setActive(true);
        template.setCreatedBy(username);
        template.setCreatedAt(Instant.now());
        template.setUpdatedAt(Instant.now());

        return ResponseEntity.ok(templateRepo.save(template));
    }

    @GetMapping
    public ResponseEntity<List<DocumentTemplate>> listActive(
            @RequestParam(value = "documentTypeId", required = false) String documentTypeId) {
        if (documentTypeId != null) {
            return ResponseEntity.ok(templateRepo.findByDocumentTypeIdAndLatestTrueAndActiveTrue(documentTypeId));
        }
        return ResponseEntity.ok(templateRepo.findByLatestTrueAndActiveTrue());
    }

    @GetMapping("/all")
    public ResponseEntity<List<DocumentTemplate>> listAll(
            @RequestParam(value = "documentTypeId", required = false) String documentTypeId) {
        if (documentTypeId != null) {
            return ResponseEntity.ok(templateRepo.findByDocumentTypeIdOrderByNameAscVersionDesc(documentTypeId));
        }
        return ResponseEntity.ok(templateRepo.findAllByOrderByNameAscVersionDesc());
    }

    /**
     * Returns template metadata + read-only OnlyOffice editor config in a single response.
     */
    @SuppressWarnings("unchecked")
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getTemplateDetail(@PathVariable String id) {
        DocumentTemplate template = templateRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template", id));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("template", template);

        if (template.getFileStorageId() != null) {
            String userId = currentUserService.getCurrentUserId();
            var user = authService.getUserById(userId);
            Map<String, Object> rawConfig = onlyOfficeService.generateTemplateViewConfig(
                    template.getFileStorageId(), template.getName(), user);
            response.put("editorConfig", new EditorConfigResponse(
                    (String) rawConfig.get("documentServerUrl"),
                    (Map<String, Object>) rawConfig.get("config"),
                    (String) rawConfig.get("mode")));
        }

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable String id) {
        DocumentTemplate template = templateRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template", id));
        template.setActive(false);
        template.setUpdatedAt(Instant.now());
        templateRepo.save(template);
        return ResponseEntity.ok().build();
    }

    private String getExtension(String filename) {
        if (filename == null) return "docx";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1) : "docx";
    }
}
