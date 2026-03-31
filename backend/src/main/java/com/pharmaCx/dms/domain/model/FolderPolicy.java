package com.pharmaCx.dms.domain.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "folder_policies")
public class FolderPolicy {

    @Id
    private String id;

    @Indexed
    private String folderId;

    // ORGANIZATION | CROSS_DEPARTMENT | DEPARTMENT_ONLY
    private String accessScope = "DEPARTMENT_ONLY";

    // Minimum UserRole required to access — stored as role name string
    // OPERATOR | ENGINEER | MANAGER | HEAD_OF_DEPARTMENT | DIRECTOR | SYSTEM_ADMIN
    private String minimumRole = "OPERATOR";

    // For CROSS_DEPARTMENT scope: which unit IDs can access
    private List<String> allowedUnitIds = new ArrayList<>();

    private String ownerId;     // user who owns / manages this folder
    private String ownerUnitId; // org unit this folder belongs to

    private FolderPermissions permissions = new FolderPermissions();

    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;

    public static class FolderPermissions {
        private boolean canRead = true;
        private boolean canWrite = false;
        private boolean canShare = false;
        private boolean canManageSubFolders = false;

        public boolean isCanRead() { return canRead; }
        public void setCanRead(boolean canRead) { this.canRead = canRead; }

        public boolean isCanWrite() { return canWrite; }
        public void setCanWrite(boolean canWrite) { this.canWrite = canWrite; }

        public boolean isCanShare() { return canShare; }
        public void setCanShare(boolean canShare) { this.canShare = canShare; }

        public boolean isCanManageSubFolders() { return canManageSubFolders; }
        public void setCanManageSubFolders(boolean canManageSubFolders) { this.canManageSubFolders = canManageSubFolders; }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFolderId() { return folderId; }
    public void setFolderId(String folderId) { this.folderId = folderId; }

    public String getAccessScope() { return accessScope; }
    public void setAccessScope(String accessScope) { this.accessScope = accessScope; }

    public String getMinimumRole() { return minimumRole; }
    public void setMinimumRole(String minimumRole) { this.minimumRole = minimumRole; }

    public List<String> getAllowedUnitIds() { return allowedUnitIds; }
    public void setAllowedUnitIds(List<String> allowedUnitIds) { this.allowedUnitIds = allowedUnitIds; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getOwnerUnitId() { return ownerUnitId; }
    public void setOwnerUnitId(String ownerUnitId) { this.ownerUnitId = ownerUnitId; }

    public FolderPermissions getPermissions() { return permissions; }
    public void setPermissions(FolderPermissions permissions) { this.permissions = permissions; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
