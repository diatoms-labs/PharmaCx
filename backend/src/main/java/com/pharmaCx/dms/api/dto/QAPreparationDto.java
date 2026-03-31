package com.pharmaCx.dms.api.dto;

public class QAPreparationDto {

    private String documentNumber; // optional -- auto-generated if blank
    private String templateId;     // optional -- latest template for doc type used if blank

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }
}
