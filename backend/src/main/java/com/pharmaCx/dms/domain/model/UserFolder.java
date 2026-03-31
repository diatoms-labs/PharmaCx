package com.pharmaCx.dms.domain.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Document(collection = "user_folders")
public class UserFolder {

    @Id
    private String id;

    private String name;

    @Indexed
    private String ownerId;

    private String ownerUsername;

    private String parentFolderId; // null for root folders

    // References organizational_units._id (replaces free-text department string)
    @Indexed
    private String ownerUnitId;

    // References folder_policies._id — null for personal folders
    private String policyId;

    // PERSONAL | DEPARTMENT — determines whether policy applies
    private String folderType = "PERSONAL";

    private boolean sharedWithAll; // retained for personal folder quick-share

    private List<String> sharedWithUserIds = new ArrayList<>();

    private List<String> documentIds = new ArrayList<>();

    // Allowed document type IDs in this folder (references document_type_configs._id)
    // Empty list = all types allowed
    private List<String> allowedDocumentTypeIds = new ArrayList<>();

    private Instant createdAt;
    private Instant updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getOwnerUsername() { return ownerUsername; }
    public void setOwnerUsername(String ownerUsername) { this.ownerUsername = ownerUsername; }

    public String getParentFolderId() { return parentFolderId; }
    public void setParentFolderId(String parentFolderId) { this.parentFolderId = parentFolderId; }

    public String getOwnerUnitId() { return ownerUnitId; }
    public void setOwnerUnitId(String ownerUnitId) { this.ownerUnitId = ownerUnitId; }

    public String getPolicyId() { return policyId; }
    public void setPolicyId(String policyId) { this.policyId = policyId; }

    public String getFolderType() { return folderType; }
    public void setFolderType(String folderType) { this.folderType = folderType; }

    public boolean isSharedWithAll() { return sharedWithAll; }
    public void setSharedWithAll(boolean sharedWithAll) { this.sharedWithAll = sharedWithAll; }

    public List<String> getSharedWithUserIds() { return sharedWithUserIds; }
    public void setSharedWithUserIds(List<String> sharedWithUserIds) { this.sharedWithUserIds = sharedWithUserIds; }

    public List<String> getDocumentIds() { return documentIds; }
    public void setDocumentIds(List<String> documentIds) { this.documentIds = documentIds; }

    public List<String> getAllowedDocumentTypeIds() { return allowedDocumentTypeIds; }
    public void setAllowedDocumentTypeIds(List<String> allowedDocumentTypeIds) { this.allowedDocumentTypeIds = allowedDocumentTypeIds; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserFolder that = (UserFolder) o;
        return sharedWithAll == that.sharedWithAll &&
                Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(ownerId, that.ownerId) &&
                Objects.equals(ownerUnitId, that.ownerUnitId) &&
                Objects.equals(folderType, that.folderType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, ownerId, ownerUnitId, folderType);
    }

    @Override
    public String toString() {
        return "UserFolder{id='" + id + "', name='" + name + "', type='" + folderType +
                "', ownerUnitId='" + ownerUnitId + "'}";
    }
}
