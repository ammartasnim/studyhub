package com.dsi.studyhub.services;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface FileStorageService {
    String storeFile(MultipartFile file, String subfolder) throws IOException;
    void deleteFile(String filename) throws IOException;
}
