package com.pharmaCx.dms.service;

import com.pharmaCx.dms.domain.model.DocumentTypeConfig;
import com.pharmaCx.dms.domain.model.OrganizationalUnit;
import com.pharmaCx.dms.domain.model.DocumentNumberSequence;
import com.pharmaCx.dms.domain.repository.DocumentTypeConfigRepository;
import com.pharmaCx.dms.domain.repository.OrganizationalUnitRepository;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

@Service
public class DocumentNumberService {

    private final MongoTemplate mongoTemplate;
    private final DocumentTypeConfigRepository docTypeConfigRepo;
    private final OrganizationalUnitRepository orgUnitRepo;

    public DocumentNumberService(MongoTemplate mongoTemplate,
                                 DocumentTypeConfigRepository docTypeConfigRepo,
                                 OrganizationalUnitRepository orgUnitRepo) {
        this.mongoTemplate = mongoTemplate;
        this.docTypeConfigRepo = docTypeConfigRepo;
        this.orgUnitRepo = orgUnitRepo;
    }

    /**
     * Preview the next document number without consuming the sequence.
     */
    public String previewNextNumber(String documentTypeId, String unitId) {
        String typePrefix = resolveTypePrefix(documentTypeId);
        String deptCode = resolveUnitCode(unitId);
        String key = typePrefix + "-" + deptCode;

        Query query = new Query(Criteria.where("key").is(key));
        DocumentNumberSequence seq = mongoTemplate.findOne(query, DocumentNumberSequence.class);
        long nextNumber = (seq != null ? seq.getCurrentNumber() : 0) + 1;
        return String.format("%s-%s-%03d", typePrefix, deptCode, nextNumber);
    }

    /**
     * Atomically generates the next document number.
     * Format: {TYPE_PREFIX}-{UNIT_CODE}-{SEQ:3}, e.g. SOP-QA-001
     */
    public String generateNumber(String documentTypeId, String unitId) {
        String typePrefix = resolveTypePrefix(documentTypeId);
        String deptCode = resolveUnitCode(unitId);
        String key = typePrefix + "-" + deptCode;

        Query query = new Query(Criteria.where("key").is(key));
        Update update = new Update().inc("currentNumber", 1);
        FindAndModifyOptions options = FindAndModifyOptions.options()
                .returnNew(true)
                .upsert(true);

        DocumentNumberSequence seq = mongoTemplate.findAndModify(
                query, update, options, DocumentNumberSequence.class);

        long number = seq != null ? seq.getCurrentNumber() : 1;
        return String.format("%s-%s-%03d", typePrefix, deptCode, number);
    }

    private String resolveTypePrefix(String documentTypeId) {
        if (documentTypeId == null) return "DOC";
        return docTypeConfigRepo.findById(documentTypeId)
                .map(DocumentTypeConfig::getNumberingPrefix)
                .filter(p -> p != null && !p.isBlank())
                .orElse(documentTypeId.toUpperCase());
    }

    private String resolveUnitCode(String unitId) {
        if (unitId == null) return "GEN";
        return orgUnitRepo.findById(unitId)
                .map(OrganizationalUnit::getCode)
                .filter(c -> c != null && !c.isBlank())
                .orElse(unitId.toUpperCase());
    }
}
