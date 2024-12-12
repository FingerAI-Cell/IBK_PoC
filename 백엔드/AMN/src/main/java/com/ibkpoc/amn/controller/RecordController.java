package com.ibkpoc.amn.controller;

import com.ibkpoc.amn.dto.CommonResponse;
import com.ibkpoc.amn.service.RecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

// RecordController.java
@RestController
@RequestMapping("/api/meeting")
@RequiredArgsConstructor
public class RecordController {

    private final RecordService recordService;

    @PostMapping("/upload-record")
    public ResponseEntity<CommonResponse<Void>> uploadRecord(
            @RequestParam("file") MultipartFile file
    ) {
        try {
            recordService.saveRecordFile(file);
            return ResponseEntity.ok(new CommonResponse<>("SUCCESS", "File uploaded successfully", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new CommonResponse<>("ERROR", "Failed to upload file: " + e.getMessage(), null));
        }
    }
}