package com.pharmaCx.dms.domain.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "document_type_configs")
public class DocumentTypeConfig {

    @Id
    private String id;

    @Indexed(unique = true)
    private String code; // e.g. "SOP", "MBR", "VALIDATION"

    private String displayName; // e.g. "Standard Operating Procedure"

    private String description;

    private String ownerUnitId; // organizational unit that owns/manages this type

    private List<String> allowedUnitIds = new ArrayList<>(); // empty = all units can use it

    private String numberingPrefix; // e.g. "SOP", "MBR" — used in document number generation

    private boolean active = true;

    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getOwnerUnitId() { return ownerUnitId; }
    public void setOwnerUnitId(String ownerUnitId) { this.ownerUnitId = ownerUnitId; }

    public List<String> getAllowedUnitIds() { return allowedUnitIds; }
    public void setAllowedUnitIds(List<String> allowedUnitIds) { this.allowedUnitIds = allowedUnitIds; }

    public String getNumberingPrefix() { return numberingPrefix; }
    public void setNumberingPrefix(String numberingPrefix) { this.numberingPrefix = numberingPrefix; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
