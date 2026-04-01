package com.pharmaCx.dms.service;

import com.pharmaCx.dms.config.OnlyOfficeConfig;
import com.pharmaCx.dms.domain.enums.DocumentStatus;
import com.pharmaCx.dms.domain.enums.StepStatus;
import com.pharmaCx.dms.domain.model.AppUser;
import com.pharmaCx.dms.domain.model.ControlledDocument;
import com.pharmaCx.dms.domain.model.WorkflowStep;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.SecretKey;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class OnlyOfficeService {

    private static final Logger log = LoggerFactory.getLogger(OnlyOfficeService.class);

    private final OnlyOfficeConfig config;
    private final RestTemplate restTemplate;

    public OnlyOfficeService(OnlyOfficeConfig config, RestTemplate restTemplate) {
        this.config = config;
        this.restTemplate = restTemplate;
    }

    // ═══════════════════════════════════════════════════════════════
    //  Permission Profile — workflow-state-driven editor permissions.
    //  download and print are always false here — overridden per user
    //  by applyUserEditorPermissions() from user.editorPermissions.
    // ═══════════════════════════════════════════════════════════════

    public enum EditorPermissionProfile {
        AUTHOR_FIRST_DRAFT(
                "edit", true,  true,  false, true,  true,  true,  true,
                false, false,
                false, true,   "markup",  true,  false, true,  true),

        AUTHOR_EDIT(
                "edit", true,  true,  true,  true,  true,  true,  true,
                false, false,
                true,  true,   "markup",  true,  false, true,  true),

        REVIEWER(
                "edit", false, true,  true,  false, false, false, true,
                false, false,
                true,  true,   "markup",  false, false, true,  true),

        APPROVER(
                "edit", false, true,  true,  false, false, false, true,
                false, false,
                false, true,   "markup",  false, false, true,  true),

        PUBLISHED_VIEW(
                "view", false, false, false, false, false, false, true,
                false, false,
                false, false,  "final",   false, true,  false, false),

        VIEW_ONLY(
                "view", false, false, false, false, false, false, true,
                false, false,
                false, false,  "final",   false, false, false, false);

        public final String mode;
        public final boolean edit;
        public final boolean comment;
        public final boolean review;
        public final boolean fillForms;
        public final boolean modifyContentControl;
        public final boolean modifyFilter;
        public final boolean copy;
        public final boolean download;  // always false — set by user.editorPermissions
        public final boolean print;     // always false — set by user.editorPermissions
        public final boolean trackChanges;
        public final boolean showReviewChanges;
        public final String reviewDisplay;
        public final boolean spellCheck;
        public final boolean protect;
        public final boolean deleteCommentAuthorOnly;
        public final boolean editCommentAuthorOnly;

        EditorPermissionProfile(String mode, boolean edit, boolean comment, boolean review,
                                boolean fillForms, boolean modifyContentControl, boolean modifyFilter,
                                boolean copy, boolean download, boolean print,
                                boolean trackChanges, boolean showReviewChanges, String reviewDisplay,
                                boolean spellCheck, boolean protect,
                                boolean deleteCommentAuthorOnly, boolean editCommentAuthorOnly) {
            this.mode = mode;
            this.edit = edit;
            this.comment = comment;
            this.review = review;
            this.fillForms = fillForms;
            this.modifyContentControl = modifyContentControl;
            this.modifyFilter = modifyFilter;
            this.copy = copy;
            this.download = download;
            this.print = print;
            this.trackChanges = trackChanges;
            this.showReviewChanges = showReviewChanges;
            this.reviewDisplay = reviewDisplay;
            this.spellCheck = spellCheck;
            this.protect = protect;
            this.deleteCommentAuthorOnly = deleteCommentAuthorOnly;
            this.editCommentAuthorOnly = editCommentAuthorOnly;
        }
    }

    public static EditorPermissionProfile resolveProfile(DocumentStatus status, boolean canEdit, boolean isRevision) {
        if (canEdit) {
            return switch (status) {
                case AUTHOR_DRAFT -> isRevision
                        ? EditorPermissionProfile.AUTHOR_EDIT
                        : EditorPermissionProfile.AUTHOR_FIRST_DRAFT;
                case PEER_REVIEW, QA_REVIEW -> EditorPermissionProfile.REVIEWER;
                default -> EditorPermissionProfile.VIEW_ONLY;
            };
        }
        return switch (status) {
            case APPROVAL -> EditorPermissionProfile.APPROVER;
            case PUBLISHED -> EditorPermissionProfile.PUBLISHED_VIEW;
            default -> EditorPermissionProfile.VIEW_ONLY;
        };
    }

    public static boolean isRevisionDraft(ControlledDocument doc) {
        if (doc.getStatus() != DocumentStatus.AUTHOR_DRAFT) return false;
        if (doc.getWorkflowSteps().size() <= 3) return false;
        WorkflowStep peerReviewStep = doc.getWorkflowSteps().get(3);
        return peerReviewStep.getCompletedAt() != null
                || peerReviewStep.getStatus() == StepStatus.COMPLETED
                || peerReviewStep.getStatus() == StepStatus.REJECTED;
    }

    public void forceSave(ControlledDocument doc) {
        if (doc.getDocumentFileId() == null) return;

        String documentKey = doc.getDocumentFileId() + "_v" + doc.getVersion()
                + "_" + doc.getUpdatedAt().toEpochMilli() + "_edit";

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("c", "forcesave");
        payload.put("key", documentKey);

        String token = signConfig(payload);
        payload.put("token", token);

        String commandUrl = config.getUrl() + "/coauthoring/CommandService.ashx";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            restTemplate.postForEntity(commandUrl, request, Map.class);
        } catch (Exception e) {
            log.warn("Force-save failed for key={}: {} (editor may not be active)", documentKey, e.getMessage());
        }
    }

    public Map<String, Object> generateEditorConfig(ControlledDocument doc, AppUser user, boolean canEdit, boolean isRevision) {
        String fileId = doc.getDocumentFileId();
        String externalPath = doc.getExternalPath();
        
        String fileExt = "docx";
        if (doc.getStatus() == DocumentStatus.EXTERNAL_KNOWLEDGE && externalPath != null) {
            if (externalPath.toLowerCase().endsWith(".pdf")) fileExt = "pdf";
        }
        
        // Filename is derived from title/number + extension

        EditorPermissionProfile profile = resolveProfile(doc.getStatus(), canEdit, isRevision);
        if (doc.getStatus() == DocumentStatus.EXTERNAL_KNOWLEDGE) {
            profile = EditorPermissionProfile.PUBLISHED_VIEW; // View-only but with more features than VIEW_ONLY
        }

        String documentKey = (fileId != null ? fileId : (externalPath != null ? String.valueOf(externalPath.hashCode()) : "EXTERNAL")) 
                + "_u" + user.getId() 
                + "_v" + doc.getVersion() 
                + "_" + doc.getUpdatedAt().toEpochMilli()
                + "_" + profile.mode;
        
        String fileUrl;
        if (doc.getStatus() == DocumentStatus.EXTERNAL_KNOWLEDGE && externalPath != null) {
            String encodedPath = URLEncoder.encode(externalPath, StandardCharsets.UTF_8);
            fileUrl = config.getBackendUrl() + "/api/v1/files/external?relativePath=" + encodedPath;
        } else {
            fileUrl = config.getBackendUrl() + "/api/v1/files/" + fileId;
        }
        
        log.info("Generating OnlyOffice config for doc={} status={} fileUrl={}", doc.getId(), doc.getStatus(), fileUrl);
        
        String callbackUrl = config.getBackendUrl() + "/api/v1/files/callback?fileId=" + (fileId != null ? fileId : "EXTERNAL");

        Map<String, Object> document = new LinkedHashMap<>();
        document.put("fileType", fileExt);
        document.put("key", documentKey);
        document.put("title", " ");
        document.put("url", fileUrl);

        // Build permissions from profile, then override download/print from user.editorPermissions
        Map<String, Object> permissions = new LinkedHashMap<>();
        permissions.put("edit", profile.edit);
        permissions.put("comment", profile.comment);
        permissions.put("review", profile.review);
        permissions.put("fillForms", profile.fillForms);
        permissions.put("modifyContentControl", profile.modifyContentControl);
        permissions.put("modifyFilter", profile.modifyFilter);
        permissions.put("copy", profile.copy);
        permissions.put("deleteCommentAuthorOnly", profile.deleteCommentAuthorOnly);
        permissions.put("editCommentAuthorOnly", profile.editCommentAuthorOnly);

        // User-level permission overrides — default false, granted by admin only
        applyUserEditorPermissions(permissions, user);
        document.put("permissions", permissions);

        Map<String, Object> userMap = new LinkedHashMap<>();
        userMap.put("id", user.getId());
        userMap.put("name", user.getFullName());

        Map<String, Object> editorConfig = new LinkedHashMap<>();
        editorConfig.put("callbackUrl", callbackUrl);
        editorConfig.put("user", userMap);
        editorConfig.put("lang", "en");
        editorConfig.put("mode", profile.mode);

        Map<String, Object> customization = new LinkedHashMap<>();
        customization.put("uiTheme", "theme-contrast-light");
        customization.put("autosave", true);
        customization.put("forcesave", true);
        customization.put("compactHeader", true);
        customization.put("compactToolbar", false);
        customization.put("toolbarNoTabs", false);
        customization.put("toolbarHideFileName", true);
        customization.put("hideRightMenu", false);
        customization.put("hideRulers", false);
        customization.put("chat", false);

        Map<String, Object> logo = new LinkedHashMap<>();
        logo.put("image", "");
        logo.put("visible", false);
        customization.put("logo", logo);

        customization.put("goback", false);

        Map<String, Object> anonymous = new LinkedHashMap<>();
        anonymous.put("request", false);
        customization.put("anonymous", anonymous);

        Map<String, Object> reviewSettings = new LinkedHashMap<>();
        reviewSettings.put("reviewDisplay", profile.reviewDisplay);
        reviewSettings.put("trackChanges", profile.trackChanges);
        reviewSettings.put("showReviewChanges", profile.showReviewChanges);
        customization.put("review", reviewSettings);

        Map<String, Object> features = new LinkedHashMap<>();
        features.put("spellcheck", profile.spellCheck);

        // Disable the OnlyOffice native AI plugin menu across all profiles —
        // AI assistance is provided by the custom helix-ai panel instead.
        Map<String, Object> aiFeature = new LinkedHashMap<>();
        aiFeature.put("smartDocument", false);
        aiFeature.put("reviewChanges", false);
        features.put("ai", aiFeature);

        customization.put("features", features);
        editorConfig.put("customization", customization);

        Map<String, Object> editorCfg = new LinkedHashMap<>();
        editorCfg.put("document", document);
        editorCfg.put("documentType", "word");
        editorCfg.put("type", "desktop");
        editorCfg.put("editorConfig", editorConfig);
        editorCfg.put("height", "100%");
        editorCfg.put("width", "100%");

        String token = signConfig(editorCfg);
        editorCfg.put("token", token);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("documentServerUrl", config.getExternalUrl());
        result.put("config", editorCfg);
        result.put("mode", profile.mode);
        result.put("features", buildFeatureFlags(profile, user, doc.getStatus()));

        return result;
    }

    /**
     * Applies user-level editor permissions (download, print, upload) to the
     * OnlyOffice permissions map. These are disabled by default for all users
     * and only enabled when admin explicitly grants them on the user record.
     */
    private void applyUserEditorPermissions(Map<String, Object> permissions, AppUser user) {
        AppUser.EditorPermissions ep = user.getEditorPermissions();
        if (ep == null) ep = new AppUser.EditorPermissions();
        permissions.put("download", ep.isCanDownload());
        permissions.put("print", ep.isCanPrint());
        // canUpload controls "insert from file" in OnlyOffice
        permissions.put("changeLayout", ep.isCanUpload());
    }

    public Map<String, Object> generateTemplateViewConfig(String fileStorageId, String templateName, AppUser user) {
        String fileExt = "docx";
        String documentKey = fileStorageId + "_v_temp_" + user.getId() + "_" + System.currentTimeMillis();
        String fileUrl = config.getBackendUrl() + "/api/v1/files/" + fileStorageId;

        Map<String, Object> document = new LinkedHashMap<>();
        document.put("fileType", fileExt);
        document.put("key", documentKey);
        document.put("title", " ");
        document.put("url", fileUrl);

        EditorPermissionProfile profile = EditorPermissionProfile.VIEW_ONLY;
        Map<String, Object> permissions = new LinkedHashMap<>();
        permissions.put("edit", profile.edit);
        permissions.put("comment", profile.comment);
        permissions.put("review", profile.review);
        permissions.put("copy", profile.copy);
        // Template viewer: apply user permissions for download/print
        applyUserEditorPermissions(permissions, user);
        document.put("permissions", permissions);

        Map<String, Object> userMap = new LinkedHashMap<>();
        userMap.put("id", user.getId());
        userMap.put("name", user.getFullName());

        Map<String, Object> editorConfig = new LinkedHashMap<>();
        editorConfig.put("user", userMap);
        editorConfig.put("lang", "en");
        editorConfig.put("mode", profile.mode);

        Map<String, Object> customization = new LinkedHashMap<>();
        customization.put("uiTheme", "theme-contrast-light");
        customization.put("compactHeader", true);
        customization.put("toolbarHideFileName", true);
        customization.put("chat", false);
        customization.put("goback", false);
        Map<String, Object> logo = new LinkedHashMap<>();
        logo.put("image", "");
        logo.put("visible", false);
        customization.put("logo", logo);
        editorConfig.put("customization", customization);

        Map<String, Object> editorCfg = new LinkedHashMap<>();
        editorCfg.put("document", document);
        editorCfg.put("documentType", "word");
        editorCfg.put("type", "desktop");
        editorCfg.put("editorConfig", editorConfig);
        editorCfg.put("height", "100%");
        editorCfg.put("width", "100%");

        String token = signConfig(editorCfg);
        editorCfg.put("token", token);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("documentServerUrl", config.getExternalUrl());
        result.put("config", editorCfg);
        result.put("mode", "view");
        result.put("features", buildFeatureFlags(profile, user, DocumentStatus.PUBLISHED));
        return result;
    }

    private Map<String, Object> buildFeatureFlags(EditorPermissionProfile profile, AppUser user, DocumentStatus status) {
        AppUser.EditorPermissions ep = user.getEditorPermissions() != null
                ? user.getEditorPermissions() : new AppUser.EditorPermissions();
        Map<String, Object> flags = new LinkedHashMap<>();
        flags.put("canEdit", profile.edit);
        flags.put("canReview", profile.review);
        flags.put("canComment", profile.comment);
        flags.put("trackChanges", profile.trackChanges);
        flags.put("canAcceptRejectChanges", profile.edit || profile.review);
        flags.put("canDownload", ep.isCanDownload());
        flags.put("canPrint", ep.isCanPrint());
        flags.put("canUpload", ep.isCanUpload());
        flags.put("versionHistory", true);
        flags.put("protection", profile.protect);
        flags.put("aiEnabled", profile == EditorPermissionProfile.AUTHOR_FIRST_DRAFT
                || profile == EditorPermissionProfile.AUTHOR_EDIT);
        return flags;
    }

    private String signConfig(Map<String, Object> payload) {
        SecretKey key = Keys.hmacShaKeyFor(config.getJwtSecret().getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .claims(payload)
                .signWith(key)
                .compact();
    }
}
