package com.crm.platform.frontend.components.charts;

import com.crm.platform.delivery.dto.DeliverySummaryResponse;
import com.crm.platform.frontend.util.FormattingUtils;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;

/**
 * Visual linear progress gauge for campaign delivery execution telemetry.
 */
public class DeliveryProgressGauge extends Div {

    private final Span percentageSpan;
    private final ProgressBar progressBar;
    private final Span sentSpan;
    private final Span failedSpan;
    private final Span pendingSpan;
    private final Span targetSpan;

    public DeliveryProgressGauge() {
        addClassName("crm-panel");
        setWidthFull();

        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.BASELINE);

        Span title = new Span("Delivery Telemetry");
        title.getStyle().set("font-size", "14px").set("font-weight", "600").set("color", "var(--crm-text-primary)");

        percentageSpan = new Span("0.0%");
        percentageSpan.getStyle().set("font-size", "18px").set("font-weight", "700").set("font-family", "var(--lumo-font-family-monospace)").set("color", "var(--crm-accent-primary)");

        header.add(title, percentageSpan);

        progressBar = new ProgressBar(0.0, 100.0, 0.0);
        progressBar.setWidthFull();
        progressBar.getStyle().set("margin", "12px 0");

        HorizontalLayout stats = new HorizontalLayout();
        stats.setWidthFull();
        stats.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        stats.getStyle().set("font-size", "12px").set("font-family", "var(--lumo-font-family-monospace)");

        sentSpan = createStatChip("Sent: 0", "var(--crm-status-success-base)");
        failedSpan = createStatChip("Failed: 0", "var(--crm-status-error-base)");
        pendingSpan = createStatChip("Pending: 0", "var(--crm-status-info-base)");
        targetSpan = createStatChip("Target: 0", "var(--crm-text-secondary)");

        stats.add(sentSpan, failedSpan, pendingSpan, targetSpan);

        add(header, progressBar, stats);
    }

    public DeliveryProgressGauge(long targetAudienceSize, long sentCount, long failedCount) {
        this();
        long pending = Math.max(0, targetAudienceSize - (sentCount + failedCount));
        double pct = targetAudienceSize > 0 ? (((double) (sentCount + failedCount) / targetAudienceSize) * 100.0) : 0.0;

        percentageSpan.setText(FormattingUtils.formatPercentage(pct));
        progressBar.setValue(pct);

        sentSpan.setText("Sent: " + FormattingUtils.formatInteger(sentCount));
        failedSpan.setText("Failed: " + FormattingUtils.formatInteger(failedCount));
        pendingSpan.setText("Pending: " + FormattingUtils.formatInteger(pending));
        targetSpan.setText("Target: " + FormattingUtils.formatInteger(targetAudienceSize));
    }

    private Span createStatChip(String text, String color) {
        Span span = new Span(text);
        span.getStyle().set("color", color).set("font-weight", "600");
        return span;
    }

    public void update(DeliverySummaryResponse summary) {
        if (summary == null) {
            return;
        }

        double pct = summary.getCompletionPercentage();
        percentageSpan.setText(FormattingUtils.formatPercentage(pct));
        progressBar.setValue(pct);

        sentSpan.setText("Sent: " + FormattingUtils.formatInteger(summary.getSentCount()));
        failedSpan.setText("Failed: " + FormattingUtils.formatInteger(summary.getFailedCount()));
        pendingSpan.setText("Pending: " + FormattingUtils.formatInteger(summary.getPendingCount()));
        targetSpan.setText("Target: " + FormattingUtils.formatInteger(summary.getTargetAudienceSize()));
    }
}
