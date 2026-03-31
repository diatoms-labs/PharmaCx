package com.pharmaCx.dms.api.controller;

import com.pharmaCx.dms.domain.model.OrganizationalUnit;
import com.pharmaCx.dms.domain.repository.OrganizationalUnitRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/org-units")
public class OrgUnitController {

    private final OrganizationalUnitRepository orgUnitRepo;

    public OrgUnitController(OrganizationalUnitRepository orgUnitRepo) {
        this.orgUnitRepo = orgUnitRepo;
    }

    @GetMapping
    public ResponseEntity<List<OrganizationalUnit>> listActive() {
        return ResponseEntity.ok(orgUnitRepo.findByActiveTrue());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrganizationalUnit> getById(@PathVariable String id) {
        return orgUnitRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<OrganizationalUnit> create(@RequestBody OrganizationalUnit unit) {
        unit.setId(null); // ensure MongoDB assigns the ID
        return ResponseEntity.ok(orgUnitRepo.save(unit));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<OrganizationalUnit> update(@PathVariable String id, @RequestBody OrganizationalUnit unit) {
        return orgUnitRepo.findById(id).map(existing -> {
            existing.setCode(unit.getCode());
            existing.setDisplayName(unit.getDisplayName());
            existing.setType(unit.getType());
            existing.setActive(unit.isActive());
            return ResponseEntity.ok(orgUnitRepo.save(existing));
        }).orElse(ResponseEntity.notFound().build());
    }
}
