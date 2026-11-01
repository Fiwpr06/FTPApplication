package com.fiwpr06.ftpapplication.common.model;

/**
 * Trang thai truyen tai tap tin
 */
public enum TransferStatus {
    QUEUED("Dang cho"),
    TRANSFERRING("Dang truyen"),
    COMPLETED("Thanh cong"),
    FAILED("That bai"),
    CANCELLED("Da huy");

    private final String description;

    TransferStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
