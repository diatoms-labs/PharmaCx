package com.pharmaCx.dms.api.dto;

import jakarta.validation.constraints.NotBlank;

public class DocumentRequestDto {

    @NotBlank
    private String title;

    @NotBlank
    private String documentTypeId; // references document_type_configs._id

    @NotBlank
    private String unitId; // references organizational_units._id

    private String justification;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDocumentTypeId() { return documentTypeId; }
    public void setDocumentTypeId(String documentTypeId) { this.documentTypeId = documentTypeId; }

    public String getUnitId() { return unitId; }
    public void setUnitId(String unitId) { this.unitId = unitId; }

    public String getJustification() { return justification; }
    public void setJustification(String justification) { this.justification = justification; }
}
