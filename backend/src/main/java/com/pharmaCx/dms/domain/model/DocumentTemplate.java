package com.pharmaCx.dms.domain.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Document(collection = "document_templates")
public class DocumentTemplate {

    @Id
    private String id;

    private String name;

    // References document_type_configs._id (replaces DocumentType enum)
    @Indexed
    private String documentTypeId;

    private String description;
    private String fileStorageId;
    private List<String> sections;
    private boolean companyLogoIncluded = true;
    private int version = 1;
    private boolean latest = true;
    private boolean active = true;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDocumentTypeId() { return documentTypeId; }
    public void setDocumentTypeId(String documentTypeId) { this.documentTypeId = documentTypeId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getFileStorageId() { return fileStorageId; }
    public void setFileStorageId(String fileStorageId) { this.fileStorageId = fileStorageId; }

    public List<String> getSections() { return sections; }
    public void setSections(List<String> sections) { this.sections = sections; }

    public boolean isCompanyLogoIncluded() { return companyLogoIncluded; }
    public void setCompanyLogoIncluded(boolean companyLogoIncluded) { this.companyLogoIncluded = companyLogoIncluded; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public boolean isLatest() { return latest; }
    public void setLatest(boolean latest) { this.latest = latest; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DocumentTemplate that = (DocumentTemplate) o;
        return companyLogoIncluded == that.companyLogoIncluded &&
                version == that.version && latest == that.latest && active == that.active &&
                Objects.equals(id, that.id) && Objects.equals(name, that.name) &&
                Objects.equals(documentTypeId, that.documentTypeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, documentTypeId, version, latest, active);
    }

    @Override
    public String toString() {
        return "DocumentTemplate{id='" + id + "', name='" + name +
                "', documentTypeId='" + documentTypeId + "', version=" + version + "}";
    }
}
