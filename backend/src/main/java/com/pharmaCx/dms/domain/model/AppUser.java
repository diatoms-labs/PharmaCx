package com.pharmaCx.dms.domain.model;

import com.pharmaCx.dms.domain.enums.UserRole;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Objects;

@Document(collection = "users")
public class AppUser {

    @Id
    private String id;

    @Indexed(unique = true)
    private String username;

    @Indexed(unique = true)
    private String email;

    private String passwordHash;
    private String fullName;
    private UserRole role;

    // References organizational_units._id (replaces Department enum)
    @Indexed
    private String unitId;

    private boolean mustChangePassword;
    private boolean active = true;

    // OnlyOffice editor permissions — all disabled by default, granted by admin per user
    private EditorPermissions editorPermissions = new EditorPermissions();

    private Instant createdAt;
    private Instant updatedAt;

    public static class EditorPermissions {
        private boolean canDownload = false;
        private boolean canPrint = false;
        private boolean canUpload = false;

        public boolean isCanDownload() { return canDownload; }
        public void setCanDownload(boolean canDownload) { this.canDownload = canDownload; }

        public boolean isCanPrint() { return canPrint; }
        public void setCanPrint(boolean canPrint) { this.canPrint = canPrint; }

        public boolean isCanUpload() { return canUpload; }
        public void setCanUpload(boolean canUpload) { this.canUpload = canUpload; }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public String getUnitId() { return unitId; }
    public void setUnitId(String unitId) { this.unitId = unitId; }

    public boolean isMustChangePassword() { return mustChangePassword; }
    public void setMustChangePassword(boolean mustChangePassword) { this.mustChangePassword = mustChangePassword; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public EditorPermissions getEditorPermissions() { return editorPermissions; }
    public void setEditorPermissions(EditorPermissions editorPermissions) { this.editorPermissions = editorPermissions; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AppUser appUser = (AppUser) o;
        return mustChangePassword == appUser.mustChangePassword &&
                active == appUser.active &&
                Objects.equals(id, appUser.id) &&
                Objects.equals(username, appUser.username) &&
                Objects.equals(email, appUser.email) &&
                Objects.equals(fullName, appUser.fullName) &&
                Objects.equals(role, appUser.role) &&
                Objects.equals(unitId, appUser.unitId) &&
                Objects.equals(createdAt, appUser.createdAt) &&
                Objects.equals(updatedAt, appUser.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username, email, fullName, role, unitId,
                mustChangePassword, active, createdAt, updatedAt);
    }

    @Override
    public String toString() {
        return "AppUser{id='" + id + "', username='" + username + "', role=" + role +
                ", unitId='" + unitId + "', active=" + active + "}";
    }
}
