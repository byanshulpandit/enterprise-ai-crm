package com.crm.platform.frontend.views.errors;

import com.crm.platform.frontend.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * SCR-ERR-01: 403 Access Denied View rendered when a user attempts an unauthorized navigation.
 */
@Route(value = "access-denied", layout = MainLayout.class)
@PageTitle("Access Denied | Enterprise AI-CRM")
public class AccessDeniedView extends VerticalLayout {

    public AccessDeniedView() {
        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        H2 title = new H2("403 — Access Denied");
        title.getStyle().set("color", "var(--crm-status-error-base)").set("margin-bottom", "8px");

        Paragraph message = new Paragraph("You do not have the necessary administrative authority (ROLE_ADMIN) to access this console.");
        message.getStyle().set("color", "var(--crm-text-secondary)").set("font-size", "14px");

        Button returnBtn = new Button("Return to Dashboard", e -> getUI().ifPresent(ui -> ui.navigate("")));
        returnBtn.addClassName("crm-btn-primary");
        returnBtn.getStyle().set("margin-top", "16px");

        add(title, message, returnBtn);
    }
}
