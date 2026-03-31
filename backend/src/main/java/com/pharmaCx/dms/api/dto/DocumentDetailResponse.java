package com.pharmaCx.dms.api.dto;

import com.pharmaCx.dms.domain.model.ControlledDocument;

import java.util.Map;

public class DocumentDetailResponse {

    private ControlledDocument document;
    private EditorConfigResponse editorConfig;
    private Map<String, Boolean> features;

    public DocumentDetailResponse() {
    }

    public DocumentDetailResponse(ControlledDocument document, EditorConfigResponse editorConfig, Map<String, Boolean> features) {
        this.document = document;
        this.editorConfig = editorConfig;
        this.features = features;
    }

    public ControlledDocument getDocument() {
        return document;
    }

    public void setDocument(ControlledDocument document) {
        this.document = document;
    }

    public EditorConfigResponse getEditorConfig() {
        return editorConfig;
    }

    public void setEditorConfig(EditorConfigResponse editorConfig) {
        this.editorConfig = editorConfig;
    }

    public Map<String, Boolean> getFeatures() {
        return features;
    }

    public void setFeatures(Map<String, Boolean> features) {
        this.features = features;
    }
}
