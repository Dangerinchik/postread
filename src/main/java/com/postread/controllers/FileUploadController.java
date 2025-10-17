package com.postread.controllers;

import com.postread.services.FileStorageService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileStorageService fileStorageService;

    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> uploadFile(@RequestParam(value = "file", required = false) MultipartFile file) {
        try {
            System.out.println("Upload endpoint called");
            System.out.println("File received: " + (file != null ? file.getOriginalFilename() : "null"));
            System.out.println("File size: " + (file != null ? file.getSize() : "0"));

            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(new UploadResponse(null, "Файл не предоставлен"));
            }

            String fileName = fileStorageService.storeFile(file);
            System.out.println("File stored with name: " + fileName);
            return ResponseEntity.ok(new UploadResponse(fileName, "Файл успешно загружен"));
        } catch (Exception e) {
            System.out.println("Error uploading file: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(new UploadResponse(null, "Ошибка загрузки файла: " + e.getMessage()));
        }
    }

    @Data
    public static class UploadResponse {
        private final String fileName;
        private final String message;

        // Добавляем конструктор по умолчанию для Jackson
        public UploadResponse() {
            this.fileName = null;
            this.message = null;
        }

        public UploadResponse(String fileName, String message) {
            this.fileName = fileName;
            this.message = message;
        }
    }
}