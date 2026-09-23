package com.crm.platform.upload.dto;

public class UploadErrorDetail {
    private int row;
    private String email;
    private String reason;

    public UploadErrorDetail() {
    }

    public UploadErrorDetail(int row, String email, String reason) {
        this.row = row;
        this.email = email;
        this.reason = reason;
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
