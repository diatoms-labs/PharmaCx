package com.pharmaCx.dms.domain.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "user_ip_policies")
public class UserIpPolicy {

    @Id
    private String id;

    @Indexed
    private String userId;

    // Exact IPs or CIDR blocks allowed (e.g. "203.0.113.5", "198.51.100.0/24")
    private List<String> allowedIps = new ArrayList<>();

    // When true: user can only access if their request IP is in allowedIps
    // When false: no IP restriction applies
    private boolean restrictToAllowedIps = false;

    // What the user can do when accessing from a matched allowed IP
    // More restrictive than their normal permissions (e.g. view-only from outside)
    private IpPermissions permissionsFromIp = new IpPermissions();

    private String grantedBy; // userId of admin who set this policy
    private Instant grantedAt;
    private Instant expiresAt; // null = no expiry

    public static class IpPermissions {
        private boolean canView = true;
        private boolean canUpload = false;
        private boolean canDownload = false;
        private boolean canPrint = false;

        public boolean isCanView() { return canView; }
        public void setCanView(boolean canView) { this.canView = canView; }

        public boolean isCanUpload() { return canUpload; }
        public void setCanUpload(boolean canUpload) { this.canUpload = canUpload; }

        public boolean isCanDownload() { return canDownload; }
        public void setCanDownload(boolean canDownload) { this.canDownload = canDownload; }

        public boolean isCanPrint() { return canPrint; }
        public void setCanPrint(boolean canPrint) { this.canPrint = canPrint; }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public List<String> getAllowedIps() { return allowedIps; }
    public void setAllowedIps(List<String> allowedIps) { this.allowedIps = allowedIps; }

    public boolean isRestrictToAllowedIps() { return restrictToAllowedIps; }
    public void setRestrictToAllowedIps(boolean restrictToAllowedIps) { this.restrictToAllowedIps = restrictToAllowedIps; }

    public IpPermissions getPermissionsFromIp() { return permissionsFromIp; }
    public void setPermissionsFromIp(IpPermissions permissionsFromIp) { this.permissionsFromIp = permissionsFromIp; }

    public String getGrantedBy() { return grantedBy; }
    public void setGrantedBy(String grantedBy) { this.grantedBy = grantedBy; }

    public Instant getGrantedAt() { return grantedAt; }
    public void setGrantedAt(Instant grantedAt) { this.grantedAt = grantedAt; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}
