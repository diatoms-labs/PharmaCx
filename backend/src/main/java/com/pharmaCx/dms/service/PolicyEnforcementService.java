package com.pharmaCx.dms.service;

import com.pharmaCx.dms.api.dto.ContactInfo;
import com.pharmaCx.dms.api.dto.PolicyDecision;
import com.pharmaCx.dms.config.RoleHierarchyConfig;
import com.pharmaCx.dms.domain.enums.UserRole;
import com.pharmaCx.dms.domain.model.*;
import com.pharmaCx.dms.domain.repository.*;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Central policy enforcement service.
 * All permission checks should flow through here to ensure consistency.
 */
@Service
public class PolicyEnforcementService {

    private final UserIpPolicyRepository ipPolicyRepo;
    private final FolderPolicyRepository folderPolicyRepo;
    private final AppUserRepository userRepo;
    private final OrganizationalUnitRepository orgUnitRepo;
    private final RoleHierarchyConfig roleHierarchy;

    public PolicyEnforcementService(UserIpPolicyRepository ipPolicyRepo,
                                    FolderPolicyRepository folderPolicyRepo,
                                    AppUserRepository userRepo,
                                    OrganizationalUnitRepository orgUnitRepo,
                                    RoleHierarchyConfig roleHierarchy) {
        this.ipPolicyRepo = ipPolicyRepo;
        this.folderPolicyRepo = folderPolicyRepo;
        this.userRepo = userRepo;
        this.orgUnitRepo = orgUnitRepo;
        this.roleHierarchy = roleHierarchy;
    }

    // ── IP Access ──────────────────────────────────────────────────────────────

    /**
     * Check if the given IP address is allowed for this user.
     * If the user has no IP policy or restrictToAllowedIps=false → ALLOW.
     */
    public PolicyDecision checkIpAccess(String userId, String ipAddress) {
        return ipPolicyRepo.findByUserId(userId).map(policy -> {
            if (!policy.isRestrictToAllowedIps()) return PolicyDecision.allow();
            if (policy.getAllowedIps() == null || policy.getAllowedIps().isEmpty()) {
                return PolicyDecision.allow(); // empty allowlist = no restriction
            }
            boolean allowed = policy.getAllowedIps().stream()
                    .anyMatch(ip -> ip.equals(ipAddress) || matchesCidr(ipAddress, ip));
            if (allowed) return PolicyDecision.allow();

            ContactInfo contact = resolveSystemAdminContact(
                    "Your IP address is not in the allowed list. Contact the system administrator to request access.");
            return PolicyDecision.deny("IP_NOT_ALLOWED",
                    "Access denied: your IP address (" + ipAddress + ") is not permitted.",
                    contact);
        }).orElse(PolicyDecision.allow()); // no policy = allow
    }

    // ── Editor Permissions ─────────────────────────────────────────────────────

    public PolicyDecision checkDownloadPermission(AppUser user) {
        if (user.getEditorPermissions() != null && user.getEditorPermissions().isCanDownload()) {
            return PolicyDecision.allow();
        }
        ContactInfo contact = resolveSystemAdminContact(
                "Download is not enabled for your account. Contact the system administrator.");
        return PolicyDecision.deny("DOWNLOAD_NOT_PERMITTED",
                "You do not have permission to download documents.", contact);
    }

    public PolicyDecision checkPrintPermission(AppUser user) {
        if (user.getEditorPermissions() != null && user.getEditorPermissions().isCanPrint()) {
            return PolicyDecision.allow();
        }
        ContactInfo contact = resolveSystemAdminContact(
                "Print is not enabled for your account. Contact the system administrator.");
        return PolicyDecision.deny("PRINT_NOT_PERMITTED",
                "You do not have permission to print documents.", contact);
    }

    public PolicyDecision checkUploadPermission(AppUser user) {
        if (user.getEditorPermissions() != null && user.getEditorPermissions().isCanUpload()) {
            return PolicyDecision.allow();
        }
        ContactInfo contact = resolveSystemAdminContact(
                "Upload is not enabled for your account. Contact the system administrator.");
        return PolicyDecision.deny("UPLOAD_NOT_PERMITTED",
                "You do not have permission to upload documents.", contact);
    }

    // ── Folder Access ──────────────────────────────────────────────────────────

    /**
     * Check if a user can access the given folder based on its FolderPolicy.
     * Access scope: ORGANIZATION (all), CROSS_DEPARTMENT (allowed units), DEPARTMENT_ONLY (same unit)
     */
    public PolicyDecision checkFolderAccess(AppUser user, UserFolder folder) {
        FolderPolicy policy = folder.getPolicyId() != null
                ? folderPolicyRepo.findById(folder.getPolicyId()).orElse(null)
                : folderPolicyRepo.findByFolderId(folder.getId()).orElse(null);

        if (policy == null) {
            // No policy = owner and explicitly shared users only
            if (folder.getOwnerId().equals(user.getId())) return PolicyDecision.allow();
            if (folder.getSharedWithUserIds() != null && folder.getSharedWithUserIds().contains(user.getId())) {
                return PolicyDecision.allow();
            }
            if (Boolean.TRUE.equals(folder.isSharedWithAll())) return PolicyDecision.allow();
            return PolicyDecision.deny("FOLDER_ACCESS_DENIED", "You do not have access to this folder.",
                    resolveFolderOwnerContact(folder));
        }

        // Check minimum role threshold
        if (policy.getMinimumRole() != null) {
            try {
                UserRole minRole = UserRole.valueOf(policy.getMinimumRole());
                if (!roleHierarchy.meetsMinimum(user.getRole(), minRole)) {
                    return PolicyDecision.deny("INSUFFICIENT_ROLE",
                            "Your role (" + user.getRole() + ") does not meet the minimum required role for this folder.",
                            resolveFolderOwnerContact(folder));
                }
            } catch (IllegalArgumentException ignored) {}
        }

        String scope = policy.getAccessScope();
        if (scope == null || "ORGANIZATION".equals(scope)) {
            return PolicyDecision.allow();
        }

        if ("DEPARTMENT_ONLY".equals(scope)) {
            if (user.getUnitId() != null && user.getUnitId().equals(policy.getOwnerUnitId())) {
                return PolicyDecision.allow();
            }
            return PolicyDecision.deny("DEPARTMENT_RESTRICTED",
                    "This folder is restricted to the owning department only.",
                    resolveFolderOwnerContact(folder));
        }

        if ("CROSS_DEPARTMENT".equals(scope)) {
            List<String> allowedUnits = policy.getAllowedUnitIds();
            if (allowedUnits != null && (allowedUnits.contains(user.getUnitId()) || user.getUnitId().equals(policy.getOwnerUnitId()))) {
                return PolicyDecision.allow();
            }
            return PolicyDecision.deny("UNIT_NOT_ALLOWED",
                    "Your organizational unit does not have access to this folder.",
                    resolveFolderOwnerContact(folder));
        }

        return PolicyDecision.allow();
    }

    public boolean userHasActiveIpPolicy(String userId) {
        return ipPolicyRepo.findByUserId(userId)
                .map(p -> p.isRestrictToAllowedIps() && p.getAllowedIps() != null && !p.getAllowedIps().isEmpty())
                .orElse(false);
    }

    // ── Contact Info Resolution ─────────────────────────────────────────────────

    public ContactInfo resolveSystemAdminContact(String reason) {
        return userRepo.findByRoleAndActiveTrue(UserRole.SYSTEM_ADMIN).stream()
                .findFirst()
                .map(admin -> new ContactInfo(reason, admin.getFullName(), "SYSTEM_ADMIN", admin.getEmail()))
                .orElse(new ContactInfo(reason, "System Administrator", "SYSTEM_ADMIN", "admin@pharmaCx.com"));
    }

    private ContactInfo resolveFolderOwnerContact(UserFolder folder) {
        String reason = "Contact the folder owner to request access to this folder.";
        if (folder.getOwnerId() != null) {
            return userRepo.findById(folder.getOwnerId())
                    .map(owner -> new ContactInfo(reason, owner.getFullName(), owner.getRole().name(), owner.getEmail()))
                    .orElse(resolveSystemAdminContact(reason));
        }
        return resolveSystemAdminContact(reason);
    }

    // ── IP CIDR matching (simple prefix match) ──────────────────────────────────

    private boolean matchesCidr(String ipAddress, String cidr) {
        if (!cidr.contains("/")) return false;
        try {
            String[] parts = cidr.split("/");
            String network = parts[0];
            int prefixLen = Integer.parseInt(parts[1]);
            long ipLong = ipToLong(ipAddress);
            long netLong = ipToLong(network);
            long mask = prefixLen == 0 ? 0L : (~0L << (32 - prefixLen)) & 0xFFFFFFFFL;
            return (ipLong & mask) == (netLong & mask);
        } catch (Exception e) {
            return false;
        }
    }

    private long ipToLong(String ip) {
        String[] octets = ip.split("\\.");
        long result = 0;
        for (String o : octets) result = result * 256 + Integer.parseInt(o.trim());
        return result;
    }
}
