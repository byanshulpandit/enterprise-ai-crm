package com.crm.platform.upload.controller;

import com.crm.platform.common.dto.ApiResponse;
import com.crm.platform.common.dto.PageMetadata;
import com.crm.platform.upload.dto.UploadHistoryDto;
import com.crm.platform.upload.dto.UploadResultResponse;
import com.crm.platform.upload.service.UploadService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/uploads")
public class UploadController {

    private final UploadService uploadService;

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping(value = "/bulk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETER')")
    public ResponseEntity<ApiResponse<UploadResultResponse>> bulkUpload(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) throws HttpMediaTypeNotSupportedException {
        String callerUsername = authentication != null ? authentication.getName() : null;
        UploadResultResponse result = uploadService.processBulkUpload(file, callerUsername);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETER')")
    public ResponseEntity<ApiResponse<List<UploadHistoryDto>>> getUploadHistory(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<UploadHistoryDto> page = uploadService.getUploadHistory(pageable);
        PageMetadata metadata = PageMetadata.fromPage(page);
        return ResponseEntity.ok(ApiResponse.success(page.getContent(), metadata));
    }
}
