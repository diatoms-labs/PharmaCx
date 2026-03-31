package com.pharmaCx.dms.config;

import com.pharmaCx.dms.ai.service.DocumentIndexService;
import com.pharmaCx.dms.domain.enums.DocumentStatus;
import com.pharmaCx.dms.domain.model.ControlledDocument;
import com.pharmaCx.dms.domain.model.OrganizationalUnit;
import com.pharmaCx.dms.domain.model.UserFolder;
import com.pharmaCx.dms.domain.repository.ControlledDocumentRepository;
import com.pharmaCx.dms.domain.repository.OrganizationalUnitRepository;
import com.pharmaCx.dms.domain.repository.UserFolderRepository;
import com.pharmaCx.dms.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@Order(2) // Run after DataSeeder
public class SampleDocumentSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SampleDocumentSeeder.class);

    private final ControlledDocumentRepository documentRepo;
    private final UserFolderRepository folderRepo;
    private final OrganizationalUnitRepository orgUnitRepo;
    private final FileStorageService fileStorageService;
    private final DocumentIndexService documentIndexService;

    @Value("${app.sample-docs-path:/app/sample-documents}")
    private String sampleDocsPath;

    public SampleDocumentSeeder(ControlledDocumentRepository documentRepo,
                                UserFolderRepository folderRepo,
                                OrganizationalUnitRepository orgUnitRepo,
                                FileStorageService fileStorageService,
                                DocumentIndexService documentIndexService) {
        this.documentRepo = documentRepo;
        this.folderRepo = folderRepo;
        this.orgUnitRepo = orgUnitRepo;
        this.fileStorageService = fileStorageService;
        this.documentIndexService = documentIndexService;
    }

    @Override
    public void run(String... args) {
        if (documentRepo.count() > 0) {
            log.info("[Sample Seeder] Documents already exist, skipping sample document seed.");
            return;
        }

        File sampleDir = new File(sampleDocsPath);
        if (!sampleDir.exists() || !sampleDir.isDirectory()) {
            log.warn("[Sample Seeder] Sample directory not found: {}", sampleDocsPath);
            return;
        }

        File[] files = sampleDir.listFiles((dir, name) -> name.endsWith(".docx"));
        if (files == null || files.length == 0) {
            log.warn("[Sample Seeder] No sample .docx files found in {}", sampleDocsPath);
            return;
        }

        log.info("[Sample Seeder] Found {} sample documents. Starting seed...", files.length);

        String qaUnitId = orgUnitRepo.findByCode("QA").map(OrganizationalUnit::getId).orElse("QA");
        String adminUserId = "admin"; // Should match seeded admin if possible, else generic

        // Ensure target folders exist
        UserFolder aiFolder = getOrCreateSystemFolder("AI Documents", qaUnitId);
        UserFolder sharedFolder = getOrCreateSystemFolder("Shared Documents", qaUnitId);

        int count = 0;
        for (File file : files) {
            try {
                String title = file.getName().replace(".docx", "").replace("_", " ");
                String fileId = fileStorageService.storeFile(new FileInputStream(file), "docx");

                ControlledDocument doc = new ControlledDocument();
                doc.setTitle(title);
                doc.setDocumentNumber("SOP-SEED-" + (count + 1));
                doc.setDocumentTypeId("SOP");
                doc.setUnitId(qaUnitId);
                doc.setStatus(DocumentStatus.PUBLISHED);
                doc.setDocumentFileId(fileId);
                doc.setAuthorId(adminUserId);
                doc.setRequestedBy(adminUserId);
                doc.setEffectiveDate(LocalDate.now());
                doc.setNextReviewDate(LocalDate.now().plusYears(2));
                doc.setCreatedAt(Instant.now());
                doc.setUpdatedAt(Instant.now());
                doc.setCurrentStepIndex(6); // PUBLISHED step

                doc = documentRepo.save(doc);

                // Add to folders
                aiFolder.getDocumentIds().add(doc.getId());
                sharedFolder.getDocumentIds().add(doc.getId());

                count++;
            } catch (IOException e) {
                log.error("[Sample Seeder] Failed to import document {}: {}", file.getName(), e.getMessage());
            }
        }

        folderRepo.save(aiFolder);
        folderRepo.save(sharedFolder);

        log.info("[Sample Seeder] Successfully seeded {} documents into AI and Shared folders.", count);

        // Trigger indexing after seeding
        documentIndexService.indexAllPublishedAsync();
    }

    private UserFolder getOrCreateSystemFolder(String name, String unitId) {
        return folderRepo.findByOwnerUnitIdAndFolderTypeOrderByCreatedAtDesc(unitId, "DEPARTMENT")
                .stream()
                .filter(f -> f.getName().equals(name))
                .findFirst()
                .orElseGet(() -> {
                    UserFolder f = new UserFolder();
                    f.setName(name);
                    f.setOwnerId("SYSTEM");
                    f.setOwnerUsername("System");
                    f.setOwnerUnitId(unitId);
                    f.setFolderType("DEPARTMENT");
                    f.setSharedWithAll(true);
                    f.setDocumentIds(new ArrayList<>());
                    f.setCreatedAt(Instant.now());
                    f.setUpdatedAt(Instant.now());
                    return folderRepo.save(f);
                });
    }
}
