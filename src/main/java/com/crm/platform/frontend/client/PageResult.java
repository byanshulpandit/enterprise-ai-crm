package com.crm.platform.frontend.client;

import com.crm.platform.common.dto.PageMetadata;

import java.util.Collections;
import java.util.List;

/**
 * Encapsulates paginated results returned from backend REST APIs.
 */
public class PageResult<T> {

    private final List<T> content;
    private final PageMetadata metadata;

    public PageResult(List<T> content, PageMetadata metadata) {
        this.content = content != null ? content : Collections.emptyList();
        this.metadata = metadata;
    }

    public List<T> getContent() {
        return content;
    }

    public PageMetadata getMetadata() {
        return metadata;
    }

    public long getTotalElements() {
        return metadata != null ? metadata.getTotalElements() : content.size();
    }

    public int getTotalPages() {
        return metadata != null ? metadata.getTotalPages() : 1;
    }

    public int getCurrentPage() {
        return metadata != null ? metadata.getPage() : 0;
    }

    public int getPageSize() {
        return metadata != null ? metadata.getSize() : content.size();
    }
}
