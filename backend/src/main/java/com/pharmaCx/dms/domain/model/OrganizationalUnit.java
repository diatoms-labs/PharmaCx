package com.pharmaCx.dms.domain.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "organizational_units")
public class OrganizationalUnit {

    @Id
    private String id;

    @Indexed(unique = true)
    private String code; // e.g. "QA", "PROD", "RD"

    private String displayName; // e.g. "Quality Assurance"

    private String description;

    private String parentUnitId; // null for top-level units

    // DEPARTMENT | DIVISION | TEAM | VIRTUAL
    private String type = "DEPARTMENT";

    private String headUserId; // user who is the head/owner of this unit (contact for access errors)

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

    public String getParentUnitId() { return parentUnitId; }
    public void setParentUnitId(String parentUnitId) { this.parentUnitId = parentUnitId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getHeadUserId() { return headUserId; }
    public void setHeadUserId(String headUserId) { this.headUserId = headUserId; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
