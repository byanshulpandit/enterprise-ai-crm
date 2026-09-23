package com.crm.platform.upload.dto;

import com.crm.platform.upload.entity.UploadStatus;
import java.util.ArrayList;
import java.util.List;

public class UploadResultResponse {
    private Long uploadId;
    private String fileName;
    private UploadStatus status;
    private int totalRecords;
    private int successfulRecords;
    private int failedRecords;
    private List<UploadErrorDetail> errors = new ArrayList<>();

    public UploadResultResponse() {
    }

    public UploadResultResponse(Long uploadId, String fileName, UploadStatus status,
                                int totalRecords, int successfulRecords, int failedRecords,
                                List<UploadErrorDetail> errors) {
        this.uploadId = uploadId;
        this.fileName = fileName;
        this.status = status;
        this.totalRecords = totalRecords;
        this.successfulRecords = successfulRecords;
        this.failedRecords = failedRecords;
        this.errors = errors != null ? errors : new ArrayList<>();
    }

    public Long getUploadId() {
        return uploadId;
    }

    public void setUploadId(Long uploadId) {
        this.uploadId = uploadId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public UploadStatus getStatus() {
        return status;
    }

    public void setStatus(UploadStatus status) {
        this.status = status;
    }

    public int getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(int totalRecords) {
        this.totalRecords = totalRecords;
    }

    public int getSuccessfulRecords() {
        return successfulRecords;
    }

    public void setSuccessfulRecords(int successfulRecords) {
        this.successfulRecords = successfulRecords;
    }

    public int getFailedRecords() {
        return failedRecords;
    }

    public void setFailedRecords(int failedRecords) {
        this.failedRecords = failedRecords;
    }

    public List<UploadErrorDetail> getErrors() {
        return errors;
    }

    public void setErrors(List<UploadErrorDetail> errors) {
        this.errors = errors;
    }
}
