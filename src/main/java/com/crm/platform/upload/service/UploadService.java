package com.crm.platform.upload.service;

import com.crm.platform.upload.dto.UploadHistoryDto;
import com.crm.platform.upload.dto.UploadResultResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.multipart.MultipartFile;

public interface UploadService {
    UploadResultResponse processBulkUpload(MultipartFile file, String callerUsername) throws HttpMediaTypeNotSupportedException;
    Page<UploadHistoryDto> getUploadHistory(Pageable pageable);
}
