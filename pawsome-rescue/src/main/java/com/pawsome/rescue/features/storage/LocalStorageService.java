package com.pawsome.rescue.features.storage;

import com.pawsome.rescue.features.storage.exception.StorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class LocalStorageService {

    private static final Logger logger = LoggerFactory.getLogger(LocalStorageService.class);
    private final Path fileStorageLocation;

    @Autowired
    public LocalStorageService(@Value("${file.upload-dir}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new StorageException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public String storeFile(MultipartFile file) {
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = "";
        try {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        } catch (Exception e) {
            // No extension
        }
        String fileName = UUID.randomUUID().toString() + fileExtension;

        try {
            if(fileName.contains("..")) {
                throw new StorageException("Sorry! Filename contains invalid path sequence " + originalFileName);
            }

            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return fileName;
        } catch (IOException ex) {
            throw new StorageException("Could not store file " + fileName + ". Please try again!", ex);
        }
    }

    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new StorageException("File not found " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new StorageException("File not found " + fileName, ex);
        }
    }

    // Optional: Add a delete method if you need it
    public void deleteFile(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }
        } catch (IOException ex) {
            throw new StorageException("Could not delete file " + fileName, ex);
        }
    }

    public void deleteAllFiles() {
        logger.warn("Attempting to delete all files in upload directory: {}", fileStorageLocation);
        try {
            // Iterate and delete files first
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(fileStorageLocation)) {
                for (Path entry : stream) {
                    if (Files.isRegularFile(entry)) {
                        Files.delete(entry);
                    } else if (Files.isDirectory(entry)) {
                        FileSystemUtils.deleteRecursively(entry); // Delete subdirectories if any
                    }
                }
            }
            logger.info("Successfully deleted contents of upload directory.");
        } catch (IOException e) {
            logger.error("Could not delete all files in upload directory: {}", fileStorageLocation, e);
            // Decide if this should throw an exception or just log
            throw new StorageException("Could not delete all files in upload directory.", e);
        }
    }
}