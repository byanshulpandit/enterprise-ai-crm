package com.crm.platform.frontend.components.common;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;

/**
 * State switcher supporting deliberate Loading, Empty, Error, and Content view states.
 */
public class StateViewContainer extends Div {

    private Component contentComponent;

    public StateViewContainer() {
        setWidthFull();
    }

    public StateViewContainer(Component initialContent) {
        this();
        setContent(initialContent);
    }

    public void setContent(Component content) {
        this.contentComponent = content;
        showContent();
    }

    public void showContent() {
        removeAll();
        if (contentComponent != null) {
            add(contentComponent);
        }
    }

    public void showContent(Component content) {
        setContent(content);
    }

    public void showLoading(String message) {
        removeAll();
        VerticalLayout layout = new VerticalLayout();
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        layout.getStyle().set("padding", "var(--crm-space-2xl) 0");

        ProgressBar spinner = new ProgressBar();
        spinner.setIndeterminate(true);
        spinner.setWidth("180px");

        Paragraph text = new Paragraph(message != null ? message : "Loading data...");
        text.getStyle().set("color", "var(--crm-text-secondary)");
        text.getStyle().set("font-size", "13px");

        layout.add(spinner, text);
        add(layout);
    }

    public void showEmpty(String title, String message) {
        removeAll();
        VerticalLayout layout = new VerticalLayout();
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        layout.getStyle().set("padding", "var(--crm-space-2xl) 0");

        H3 heading = new H3(title != null ? title : "No Data Found");
        heading.getStyle().set("color", "var(--crm-text-secondary)");
        heading.getStyle().set("margin-bottom", "4px");

        Paragraph desc = new Paragraph(message != null ? message : "There are no records to display at this time.");
        desc.getStyle().set("color", "var(--crm-text-muted)");
        desc.getStyle().set("font-size", "13px");

        layout.add(heading, desc);
        add(layout);
    }

    public void showError(String errorMessage, Runnable onRetry) {
        removeAll();
        VerticalLayout layout = new VerticalLayout();
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        layout.getStyle().set("padding", "var(--crm-space-2xl) 0");

        H3 heading = new H3("Unable to Load Data");
        heading.getStyle().set("color", "var(--crm-status-error-base)");
        heading.getStyle().set("margin-bottom", "4px");

        Paragraph desc = new Paragraph(errorMessage != null ? errorMessage : "An unexpected server error occurred.");
        desc.getStyle().set("color", "var(--crm-text-secondary)");
        desc.getStyle().set("font-size", "13px");

        layout.add(heading, desc);

        if (onRetry != null) {
            Button retryBtn = new Button("Retry", e -> onRetry.run());
            retryBtn.addClassName("crm-btn-secondary");
            layout.add(retryBtn);
        }

        add(layout);
    }
}
