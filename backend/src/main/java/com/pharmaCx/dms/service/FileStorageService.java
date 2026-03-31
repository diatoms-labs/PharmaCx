package com.pharmaCx.dms.service;

import com.pharmaCx.dms.config.OnlyOfficeConfig;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private final OnlyOfficeConfig config;
    private Path storagePath;

    public FileStorageService(OnlyOfficeConfig config) {
        this.config = config;
    }

    @PostConstruct
    public void init() {
        storagePath = Paths.get(config.getFileStoragePath());
        try {
            Files.createDirectories(storagePath);
            log.info("File storage initialized at: {}", storagePath.toAbsolutePath());
        } catch (IOException e) {
            throw new IllegalStateException("Could not create file storage directory", e);
        }
    }

    public String storeFile(InputStream inputStream, String extension) throws IOException {
        String fileId = UUID.randomUUID().toString();
        Path filePath = storagePath.resolve(fileId + "." + extension);
        Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        log.info("Stored file: {}", filePath);
        return fileId;
    }

    public String storeFileWithId(String fileId, InputStream inputStream, String extension) throws IOException {
        Path filePath = storagePath.resolve(fileId + "." + extension);
        Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        log.info("Stored file with id {}: {}", fileId, filePath);
        return fileId;
    }

    public Resource loadFile(String fileId) {
        // Try common extensions
        for (String ext : new String[]{"docx", "xlsx", "pptx", "pdf"}) {
            Path filePath = storagePath.resolve(fileId + "." + ext);
            if (Files.exists(filePath)) {
                return new FileSystemResource(filePath);
            }
        }
        return null;
    }

    public Path getFilePath(String fileId) {
        for (String ext : new String[]{"docx", "xlsx", "pptx", "pdf"}) {
            Path filePath = storagePath.resolve(fileId + "." + ext);
            if (Files.exists(filePath)) {
                return filePath;
            }
        }
        return null;
    }

    public String getFileExtension(String fileId) {
        for (String ext : new String[]{"docx", "xlsx", "pptx", "pdf"}) {
            Path filePath = storagePath.resolve(fileId + "." + ext);
            if (Files.exists(filePath)) {
                return ext;
            }
        }
        return "docx";
    }

    public boolean fileExists(String fileId) {
        return getFilePath(fileId) != null;
    }

    public void copyFileToExternal(String fileId, String targetPath, String newName) throws IOException {
        Path sourcePath = getFilePath(fileId);
        if (sourcePath == null) throw new IOException("Source file not found: " + fileId);
        
        Path targetDir = Paths.get(targetPath);
        Files.createDirectories(targetDir);
        
        Path targetFile = targetDir.resolve(newName);
        Files.copy(sourcePath, targetFile, StandardCopyOption.REPLACE_EXISTING);
        log.info("Exported file for RAG: {}", targetFile);
    }
}
