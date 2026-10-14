package com.crm.platform.frontend.components.common;

import com.vaadin.flow.component.html.Span;

/**
 * Enterprise semantic status badge displaying a colored indicator dot and uppercase label.
 */
public class StatusBadge extends Span {

    public enum BadgeVariant {
        SUCCESS,
        WARNING,
        DANGER,
        INFO,
        NEUTRAL
    }

    public StatusBadge(String status) {
        this(status, resolveVariant(status));
    }

    public StatusBadge(String status, BadgeVariant variant) {
        addClassName("crm-status-pill");

        Span dot = new Span();
        dot.addClassName("crm-status-dot");

        Span text = new Span(status != null ? status : "UNKNOWN");

        add(dot, text);

        if (variant == null) {
            variant = resolveVariant(status);
        }

        switch (variant) {
            case SUCCESS -> addClassName("crm-status-success");
            case WARNING -> addClassName("crm-status-warning");
            case DANGER -> addClassName("crm-status-error");
            case INFO -> addClassName("crm-status-info");
            case NEUTRAL -> {
                addClassName("crm-status-info");
                getStyle().set("opacity", "0.75");
            }
        }
    }

    private static BadgeVariant resolveVariant(String status) {
        if (status == null) {
            return BadgeVariant.INFO;
        }
        return switch (status.toUpperCase()) {
            case "ACTIVE", "COMPLETED", "SENT", "SUCCESS" -> BadgeVariant.SUCCESS;
            case "DRAFT", "PARTIAL_SUCCESS", "WARNING", "FALLBACK" -> BadgeVariant.WARNING;
            case "FAILED", "DEACTIVATED", "ERROR" -> BadgeVariant.DANGER;
            case "RUNNING", "PENDING", "PROCESSING", "INFO" -> BadgeVariant.INFO;
            default -> BadgeVariant.INFO;
        };
    }
}
