package com.crm.platform.segment.dto;

import java.time.Instant;

public class SegmentPreviewResponse {

    private Long segmentId;
    private String segmentName;
    private Long matchedAudienceCount;
    private Instant evaluatedAt;

    public SegmentPreviewResponse() {
    }

    public SegmentPreviewResponse(Long segmentId, String segmentName, Long matchedAudienceCount, Instant evaluatedAt) {
        this.segmentId = segmentId;
        this.segmentName = segmentName;
        this.matchedAudienceCount = matchedAudienceCount;
        this.evaluatedAt = evaluatedAt;
    }

    public Long getSegmentId() {
        return segmentId;
    }

    public void setSegmentId(Long segmentId) {
        this.segmentId = segmentId;
    }

    public String getSegmentName() {
        return segmentName;
    }

    public void setSegmentName(String segmentName) {
        this.segmentName = segmentName;
    }

    public Long getMatchedAudienceCount() {
        return matchedAudienceCount;
    }

    public void setMatchedAudienceCount(Long matchedAudienceCount) {
        this.matchedAudienceCount = matchedAudienceCount;
    }

    public Instant getEvaluatedAt() {
        return evaluatedAt;
    }

    public void setEvaluatedAt(Instant evaluatedAt) {
        this.evaluatedAt = evaluatedAt;
    }
}
