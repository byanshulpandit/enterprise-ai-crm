package com.crm.platform.frontend.components.common;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Accessible confirmation dialog for consequential and destructive operations.
 */
public class ConfirmDialog extends Dialog {

    public ConfirmDialog(String title, String message, String confirmText, Runnable onConfirm) {
        this(title, message, confirmText, true, onConfirm);
    }

    public ConfirmDialog(String title, String message, String confirmText, boolean isDestructive, Runnable onConfirm) {
        setCloseOnEsc(true);
        setCloseOnOutsideClick(false);
        setWidth("420px");

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(true);
        layout.setSpacing(true);

        H3 titleHeader = new H3(title);
        titleHeader.getStyle().set("margin", "0 0 8px 0");
        titleHeader.getStyle().set("font-size", "16px");
        titleHeader.getStyle().set("color", isDestructive ? "var(--crm-status-error-base)" : "var(--crm-text-primary)");

        Paragraph messageParagraph = new Paragraph(message);
        messageParagraph.getStyle().set("color", "var(--crm-text-secondary)");
        messageParagraph.getStyle().set("font-size", "13px");
        messageParagraph.getStyle().set("margin", "0 0 16px 0");

        Button cancelButton = new Button("Cancel", e -> close());
        cancelButton.addClassName("crm-btn-secondary");

        Button confirmButton = new Button(confirmText != null ? confirmText : "Confirm", e -> {
            close();
            if (onConfirm != null) {
                onConfirm.run();
            }
        });
        if (isDestructive) {
            confirmButton.addClassName("crm-btn-danger");
        } else {
            confirmButton.addClassName("crm-btn-primary");
        }

        HorizontalLayout actionButtons = new HorizontalLayout(cancelButton, confirmButton);
        actionButtons.setWidthFull();
        actionButtons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        layout.add(titleHeader, messageParagraph, actionButtons);
        add(layout);
    }
}
