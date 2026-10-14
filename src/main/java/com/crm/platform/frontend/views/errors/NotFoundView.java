package com.crm.platform.frontend.views.errors;

import com.crm.platform.frontend.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.ParentLayout;
import jakarta.servlet.http.HttpServletResponse;

/**
 * SCR-ERR-02: 404 Resource Not Found View.
 */
@ParentLayout(MainLayout.class)
@PageTitle("Page Not Found | Enterprise AI-CRM")
public class NotFoundView extends VerticalLayout implements HasErrorParameter<NotFoundException> {

    private final Paragraph pathParagraph;

    public NotFoundView() {
        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        H2 title = new H2("404 — Page Not Found");
        title.getStyle().set("color", "var(--crm-text-primary)").set("margin-bottom", "8px");

        pathParagraph = new Paragraph("The requested route could not be found.");
        pathParagraph.getStyle().set("color", "var(--crm-text-secondary)").set("font-size", "14px");

        Button returnBtn = new Button("Return to Dashboard", e -> getUI().ifPresent(ui -> ui.navigate("")));
        returnBtn.addClassName("crm-btn-primary");
        returnBtn.getStyle().set("margin-top", "16px");

        add(title, pathParagraph, returnBtn);
    }

    @Override
    public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<NotFoundException> parameter) {
        pathParagraph.setText("The requested URL path '/" + event.getLocation().getPath() + "' does not exist.");
        return HttpServletResponse.SC_NOT_FOUND;
    }
}
