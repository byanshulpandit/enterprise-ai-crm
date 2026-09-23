package com.crm.platform.reporting.dto;

import com.crm.platform.campaign.entity.CampaignStatus;

import java.time.Instant;

public class CampaignReportResponse {

    private Long campaignId;
    private String campaignName;
    private CampaignStatus status;
    private long targetAudienceSize;
    private MetricsDto metrics;
    private TimelineDto timeline;

    public CampaignReportResponse() {
    }

    public CampaignReportResponse(Long campaignId, String campaignName, CampaignStatus status,
                                  long targetAudienceSize, MetricsDto metrics, TimelineDto timeline) {
        this.campaignId = campaignId;
        this.campaignName = campaignName;
        this.status = status;
        this.targetAudienceSize = targetAudienceSize;
        this.metrics = metrics;
        this.timeline = timeline;
    }

    public static class MetricsDto {
        private long sent;
        private long failed;
        private double deliveryRatePercentage;

        public MetricsDto() {}
        public MetricsDto(long sent, long failed, double deliveryRatePercentage) {
            this.sent = sent;
            this.failed = failed;
            this.deliveryRatePercentage = deliveryRatePercentage;
        }
        public long getSent() { return sent; }
        public void setSent(long sent) { this.sent = sent; }
        public long getFailed() { return failed; }
        public void setFailed(long failed) { this.failed = failed; }
        public double getDeliveryRatePercentage() { return deliveryRatePercentage; }
        public void setDeliveryRatePercentage(double deliveryRatePercentage) { this.deliveryRatePercentage = deliveryRatePercentage; }
    }

    public static class TimelineDto {
        private Instant launchedAt;
        private Instant completedAt;
        private Long durationSeconds;

        public TimelineDto() {}
        public TimelineDto(Instant launchedAt, Instant completedAt, Long durationSeconds) {
            this.launchedAt = launchedAt;
            this.completedAt = completedAt;
            this.durationSeconds = durationSeconds;
        }
        public Instant getLaunchedAt() { return launchedAt; }
        public void setLaunchedAt(Instant launchedAt) { this.launchedAt = launchedAt; }
        public Instant getCompletedAt() { return completedAt; }
        public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
        public Long getDurationSeconds() { return durationSeconds; }
        public void setDurationSeconds(Long durationSeconds) { this.durationSeconds = durationSeconds; }
    }

    public Long getCampaignId() { return campaignId; }
    public void setCampaignId(Long campaignId) { this.campaignId = campaignId; }
    public String getCampaignName() { return campaignName; }
    public void setCampaignName(String campaignName) { this.campaignName = campaignName; }
    public CampaignStatus getStatus() { return status; }
    public void setStatus(CampaignStatus status) { this.status = status; }
    public long getTargetAudienceSize() { return targetAudienceSize; }
    public void setTargetAudienceSize(long targetAudienceSize) { this.targetAudienceSize = targetAudienceSize; }
    public MetricsDto getMetrics() { return metrics; }
    public void setMetrics(MetricsDto metrics) { this.metrics = metrics; }
    public TimelineDto getTimeline() { return timeline; }
    public void setTimeline(TimelineDto timeline) { this.timeline = timeline; }
}
