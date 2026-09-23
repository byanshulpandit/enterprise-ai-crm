package com.crm.platform.upload.dto;

import com.crm.platform.upload.entity.UploadFileType;
import com.crm.platform.upload.entity.UploadStatus;

import java.time.Instant;

public class UploadHistoryDto {
    private Long id;
    private Long uploadedBy;
    private String fileName;
    private UploadFileType fileType;
    private Integer totalRows;
    private Integer successCount;
    private Integer failureCount;
    private UploadStatus status;
    private Instant createdAt;

    public UploadHistoryDto() {
    }

    public UploadHistoryDto(Long id, Long uploadedBy, String fileName, UploadFileType fileType,
                            Integer totalRows, Integer successCount, Integer failureCount,
                            UploadStatus status, Instant createdAt) {
        this.id = id;
        this.uploadedBy = uploadedBy;
        this.fileName = fileName;
        this.fileType = fileType;
        this.totalRows = totalRows;
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(Long uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public UploadFileType getFileType() {
        return fileType;
    }

    public void setFileType(UploadFileType fileType) {
        this.fileType = fileType;
    }

    public Integer getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(Integer totalRows) {
        this.totalRows = totalRows;
    }

    public Integer getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(Integer successCount) {
        this.successCount = successCount;
    }

    public Integer getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(Integer failureCount) {
        this.failureCount = failureCount;
    }

    public UploadStatus getStatus() {
        return status;
    }

    public void setStatus(UploadStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
