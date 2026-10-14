package com.crm.platform.frontend.components.charts;

import com.crm.platform.frontend.util.FormattingUtils;
import com.crm.platform.reporting.dto.CustomerOverviewReportResponse.LocationMetricDto;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.List;

/**
 * Pure CSS/Java horizontal bar chart displaying customer location distributions.
 */
public class TopLocationsBarChart extends Div {

    private final VerticalLayout listContainer;

    public TopLocationsBarChart() {
        addClassName("crm-panel");
        setWidthFull();

        Span title = new Span("Top Customer Locations");
        title.getStyle().set("font-size", "14px").set("font-weight", "600").set("color", "var(--crm-text-primary)").set("display", "block").set("margin-bottom", "14px");

        listContainer = new VerticalLayout();
        listContainer.setPadding(false);
        listContainer.setSpacing(true);
        listContainer.setWidthFull();

        add(title, listContainer);
    }

    public void setData(List<LocationMetricDto> locations) {
        listContainer.removeAll();
        if (locations == null || locations.isEmpty()) {
            Span empty = new Span("No location data recorded");
            empty.getStyle().set("color", "var(--crm-text-muted)").set("font-size", "12px");
            listContainer.add(empty);
            return;
        }

        long maxCount = locations.stream().mapToLong(LocationMetricDto::getCustomerCount).max().orElse(1L);
        if (maxCount == 0) maxCount = 1;

        for (LocationMetricDto loc : locations) {
            HorizontalLayout row = new HorizontalLayout();
            row.setWidthFull();
            row.setAlignItems(FlexComponent.Alignment.CENTER);
            row.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

            Span citySpan = new Span(loc.getCity() != null ? loc.getCity() : "Unknown");
            citySpan.setWidth("110px");
            citySpan.getStyle().set("font-size", "12px").set("font-weight", "500").set("color", "var(--crm-text-secondary)");

            Div barWrapper = new Div();
            barWrapper.getStyle()
                    .set("flex-grow", "1")
                    .set("background-color", "var(--crm-surface-panel)")
                    .set("border-radius", "3px")
                    .set("height", "14px")
                    .set("overflow", "hidden")
                    .set("margin", "0 12px");

            Div bar = new Div();
            double pct = ((double) loc.getCustomerCount() / maxCount) * 100.0;
            bar.getStyle()
                    .set("background-color", "var(--crm-accent-primary)")
                    .set("height", "100%")
                    .set("width", Math.max(pct, 2.0) + "%")
                    .set("border-radius", "3px");
            barWrapper.add(bar);

            Span countSpan = new Span(FormattingUtils.formatInteger(loc.getCustomerCount()));
            countSpan.setWidth("60px");
            countSpan.getStyle()
                    .set("font-size", "12px")
                    .set("text-align", "right")
                    .set("font-family", "var(--lumo-font-family-monospace)")
                    .set("color", "var(--crm-text-primary)");

            row.add(citySpan, barWrapper, countSpan);
            listContainer.add(row);
        }
    }
}
