package com.pharmaCx.dms.domain.model;

import com.pharmaCx.dms.domain.enums.AuditAction;
import com.pharmaCx.dms.domain.enums.ResourceType;
import com.pharmaCx.dms.domain.enums.UserRole;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Document(collection = "audit_events")
public class AuditEvent {

    @Id
    private String id;

    @Indexed
    private Instant timestamp;

    @Indexed
    private String userId;
    private String username;
    private UserRole userRole;
    private String userUnitId;
    private String ipAddress;
    private String sessionId;

    @Indexed
    private AuditAction action;
    private ResourceType resourceType;

    @Indexed
    private String resourceId;
    private String resourceName;

    // Change details
    private ChangeDetails changeDetails;

    // Why (required for certain actions per 21 CFR Part 11)
    private String reason;
    private String details;

    // Electronic signature
    private String signatureData;
    private boolean signatureVerified;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public UserRole getUserRole() {
        return userRole;
    }

    public void setUserRole(UserRole userRole) {
        this.userRole = userRole;
    }

    public String getUserUnitId() {
        return userUnitId;
    }

    public void setUserUnitId(String userUnitId) {
        this.userUnitId = userUnitId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public AuditAction getAction() {
        return action;
    }

    public void setAction(AuditAction action) {
        this.action = action;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public ChangeDetails getChangeDetails() {
        return changeDetails;
    }

    public void setChangeDetails(ChangeDetails changeDetails) {
        this.changeDetails = changeDetails;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getSignatureData() {
        return signatureData;
    }

    public void setSignatureData(String signatureData) {
        this.signatureData = signatureData;
    }

    public boolean isSignatureVerified() {
        return signatureVerified;
    }

    public void setSignatureVerified(boolean signatureVerified) {
        this.signatureVerified = signatureVerified;
    }

    @Override
    public String toString() {
        return "AuditEvent{" +
                "id='" + id + '\'' +
                ", timestamp=" + timestamp +
                ", userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                ", userRole=" + userRole +
                ", userUnitId=" + userUnitId +
                ", ipAddress='" + ipAddress + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", action=" + action +
                ", resourceType=" + resourceType +
                ", resourceId='" + resourceId + '\'' +
                ", resourceName='" + resourceName + '\'' +
                ", changeDetails=" + changeDetails +
                ", reason='" + reason + '\'' +
                ", signatureData='" + signatureData + '\'' +
                ", signatureVerified=" + signatureVerified +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuditEvent that = (AuditEvent) o;
        return signatureVerified == that.signatureVerified &&
                Objects.equals(id, that.id) &&
                Objects.equals(timestamp, that.timestamp) &&
                Objects.equals(userId, that.userId) &&
                Objects.equals(username, that.username) &&
                Objects.equals(userRole, that.userRole) &&
                Objects.equals(userUnitId, that.userUnitId) &&
                Objects.equals(ipAddress, that.ipAddress) &&
                Objects.equals(sessionId, that.sessionId) &&
                Objects.equals(action, that.action) &&
                Objects.equals(resourceType, that.resourceType) &&
                Objects.equals(resourceId, that.resourceId) &&
                Objects.equals(resourceName, that.resourceName) &&
                Objects.equals(changeDetails, that.changeDetails) &&
                Objects.equals(reason, that.reason) &&
                Objects.equals(signatureData, that.signatureData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, timestamp, userId, username, userRole, userUnitId, ipAddress,
                sessionId, action, resourceType, resourceId, resourceName, changeDetails,
                reason, signatureData, signatureVerified);
    }

    public static class ChangeDetails {
        private Integer documentVersion;
        private Integer previousVersion;
        private List<Integer> pagesModified;
        private List<String> sectionsModified;
        private String changeType;
        private String changeSummary;

        private String fromStep;
        private String toStep;
        private String fromStatus;
        private String toStatus;

        private List<FieldChange> fieldChanges;

        public Integer getDocumentVersion() {
            return documentVersion;
        }

        public void setDocumentVersion(Integer documentVersion) {
            this.documentVersion = documentVersion;
        }

        public Integer getPreviousVersion() {
            return previousVersion;
        }

        public void setPreviousVersion(Integer previousVersion) {
            this.previousVersion = previousVersion;
        }

        public List<Integer> getPagesModified() {
            return pagesModified;
        }

        public void setPagesModified(List<Integer> pagesModified) {
            this.pagesModified = pagesModified;
        }

        public List<String> getSectionsModified() {
            return sectionsModified;
        }

        public void setSectionsModified(List<String> sectionsModified) {
            this.sectionsModified = sectionsModified;
        }

        public String getChangeType() {
            return changeType;
        }

        public void setChangeType(String changeType) {
            this.changeType = changeType;
        }

        public String getChangeSummary() {
            return changeSummary;
        }

        public void setChangeSummary(String changeSummary) {
            this.changeSummary = changeSummary;
        }

        public String getFromStep() {
            return fromStep;
        }

        public void setFromStep(String fromStep) {
            this.fromStep = fromStep;
        }

        public String getToStep() {
            return toStep;
        }

        public void setToStep(String toStep) {
            this.toStep = toStep;
        }

        public String getFromStatus() {
            return fromStatus;
        }

        public void setFromStatus(String fromStatus) {
            this.fromStatus = fromStatus;
        }

        public String getToStatus() {
            return toStatus;
        }

        public void setToStatus(String toStatus) {
            this.toStatus = toStatus;
        }

        public List<FieldChange> getFieldChanges() {
            return fieldChanges;
        }

        public void setFieldChanges(List<FieldChange> fieldChanges) {
            this.fieldChanges = fieldChanges;
        }

        @Override
        public String toString() {
            return "ChangeDetails{" +
                    "documentVersion=" + documentVersion +
                    ", previousVersion=" + previousVersion +
                    ", pagesModified=" + pagesModified +
                    ", sectionsModified=" + sectionsModified +
                    ", changeType='" + changeType + '\'' +
                    ", changeSummary='" + changeSummary + '\'' +
                    ", fromStep='" + fromStep + '\'' +
                    ", toStep='" + toStep + '\'' +
                    ", fromStatus='" + fromStatus + '\'' +
                    ", toStatus='" + toStatus + '\'' +
                    ", fieldChanges=" + fieldChanges +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ChangeDetails that = (ChangeDetails) o;
            return Objects.equals(documentVersion, that.documentVersion) &&
                    Objects.equals(previousVersion, that.previousVersion) &&
                    Objects.equals(pagesModified, that.pagesModified) &&
                    Objects.equals(sectionsModified, that.sectionsModified) &&
                    Objects.equals(changeType, that.changeType) &&
                    Objects.equals(changeSummary, that.changeSummary) &&
                    Objects.equals(fromStep, that.fromStep) &&
                    Objects.equals(toStep, that.toStep) &&
                    Objects.equals(fromStatus, that.fromStatus) &&
                    Objects.equals(toStatus, that.toStatus) &&
                    Objects.equals(fieldChanges, that.fieldChanges);
        }

        @Override
        public int hashCode() {
            return Objects.hash(documentVersion, previousVersion, pagesModified, sectionsModified,
                    changeType, changeSummary, fromStep, toStep, fromStatus, toStatus, fieldChanges);
        }
    }

    public static class FieldChange {
        private String fieldName;
        private String oldValue;
        private String newValue;

        public String getFieldName() {
            return fieldName;
        }

        public void setFieldName(String fieldName) {
            this.fieldName = fieldName;
        }

        public String getOldValue() {
            return oldValue;
        }

        public void setOldValue(String oldValue) {
            this.oldValue = oldValue;
        }

        public String getNewValue() {
            return newValue;
        }

        public void setNewValue(String newValue) {
            this.newValue = newValue;
        }

        @Override
        public String toString() {
            return "FieldChange{" +
                    "fieldName='" + fieldName + '\'' +
                    ", oldValue='" + oldValue + '\'' +
                    ", newValue='" + newValue + '\'' +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FieldChange that = (FieldChange) o;
            return Objects.equals(fieldName, that.fieldName) &&
                    Objects.equals(oldValue, that.oldValue) &&
                    Objects.equals(newValue, that.newValue);
        }

        @Override
        public int hashCode() {
            return Objects.hash(fieldName, oldValue, newValue);
        }
    }
}
