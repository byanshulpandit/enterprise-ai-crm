package com.crm.platform.frontend.components.common;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

/**
 * Enterprise KPI stat widget displaying high-density operational metrics.
 */
public class KpiStatCard extends Div {

    private final Span valueSpan;
    private final Span subtextSpan;

    public KpiStatCard(String label, String initialValue, String subtext) {
        this(label, initialValue, subtext, null);
    }

    public KpiStatCard(String label, String initialValue, String subtext, String borderClass) {
        addClassName("crm-kpi-card");
        if (borderClass != null && !borderClass.isBlank()) {
            addClassName(borderClass);
        }

        Span labelSpan = new Span(label);
        labelSpan.addClassName("crm-kpi-label");

        valueSpan = new Span(initialValue != null ? initialValue : "—");
        valueSpan.addClassName("crm-kpi-value");

        subtextSpan = new Span(subtext != null ? subtext : "");
        subtextSpan.addClassName("crm-kpi-subtext");

        add(labelSpan, valueSpan, subtextSpan);
    }

    public void setValue(String value) {
        valueSpan.setText(value != null ? value : "—");
    }

    public void setSubtext(String subtext) {
        subtextSpan.setText(subtext != null ? subtext : "");
    }
}
