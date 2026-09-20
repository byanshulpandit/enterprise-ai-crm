package com.crm.platform.common.dto;

import org.springframework.data.domain.Page;

public class PageMetadata {

    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    @com.fasterxml.jackson.annotation.JsonProperty("isFirst")
    private boolean isFirst;
    @com.fasterxml.jackson.annotation.JsonProperty("isLast")
    private boolean isLast;

    public PageMetadata() {
    }

    public PageMetadata(int page, int size, long totalElements, int totalPages, boolean isFirst, boolean isLast) {
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.isFirst = isFirst;
        this.isLast = isLast;
    }

    public static PageMetadata fromPage(Page<?> page) {
        return new PageMetadata(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("isFirst")
    public boolean isFirst() {
        return isFirst;
    }

    public void setFirst(boolean first) {
        isFirst = first;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("isLast")
    public boolean isLast() {
        return isLast;
    }

    public void setLast(boolean last) {
        isLast = last;
    }
}
