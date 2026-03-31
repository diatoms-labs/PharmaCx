package com.pharmaCx.dms.api.controller;

import com.pharmaCx.dms.domain.model.DocumentTypeConfig;
import com.pharmaCx.dms.domain.repository.DocumentTypeConfigRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/document-types")
public class DocumentTypeConfigController {

    private final DocumentTypeConfigRepository docTypeRepo;

    public DocumentTypeConfigController(DocumentTypeConfigRepository docTypeRepo) {
        this.docTypeRepo = docTypeRepo;
    }

    @GetMapping
    public ResponseEntity<List<DocumentTypeConfig>> listActive() {
        return ResponseEntity.ok(docTypeRepo.findByActiveTrue());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentTypeConfig> getById(@PathVariable String id) {
        return docTypeRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<DocumentTypeConfig> create(@RequestBody DocumentTypeConfig config) {
        config.setId(null);
        return ResponseEntity.ok(docTypeRepo.save(config));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<DocumentTypeConfig> update(@PathVariable String id, @RequestBody DocumentTypeConfig config) {
        return docTypeRepo.findById(id).map(existing -> {
            existing.setCode(config.getCode());
            existing.setDisplayName(config.getDisplayName());
            existing.setNumberingPrefix(config.getNumberingPrefix());
            existing.setActive(config.isActive());
            return ResponseEntity.ok(docTypeRepo.save(existing));
        }).orElse(ResponseEntity.notFound().build());
    }
}
