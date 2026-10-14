package com.crm.platform.frontend.views.login;

import com.crm.platform.frontend.client.ApiException;
import com.crm.platform.frontend.service.AuthApiService;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * SCR-01: Public Authentication Screen for the Enterprise AI-CRM Platform.
 */
@Route("login")
@PageTitle("Login | Enterprise AI-CRM Platform")
public class LoginView extends VerticalLayout {

    private final AuthApiService authApiService;
    private final Div errorBanner;

    public LoginView(AuthApiService authApiService) {
        this.authApiService = authApiService;

        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        getStyle().set("background-color", "var(--crm-canvas-background)");

        Div loginCard = new Div();
        loginCard.addClassName("crm-panel");
        loginCard.setWidth("380px");
        loginCard.getStyle()
                .set("background-color", "var(--crm-surface-panel)")
                .set("border", "1px solid var(--crm-border-contrast)")
                .set("border-radius", "var(--crm-radius-md)")
                .set("box-shadow", "var(--crm-shadow-lg)");

        VerticalLayout cardContent = new VerticalLayout();
        cardContent.setPadding(true);
        cardContent.setSpacing(true);

        H2 brandTitle = new H2("Enterprise AI-CRM");
        brandTitle.getStyle()
                .set("font-size", "20px")
                .set("font-weight", "700")
                .set("margin", "0")
                .set("color", "var(--crm-text-primary)");

        Paragraph subtitle = new Paragraph("Sign in with your corporate credentials");
        subtitle.getStyle()
                .set("font-size", "13px")
                .set("color", "var(--crm-text-secondary)")
                .set("margin", "0 0 12px 0");

        errorBanner = new Div();
        errorBanner.addClassName("crm-status-pill");
        errorBanner.addClassName("crm-status-error");
        errorBanner.setWidthFull();
        errorBanner.setVisible(false);
        errorBanner.getStyle().set("margin-bottom", "12px");

        TextField usernameField = new TextField("Username or Email");
        usernameField.setWidthFull();
        usernameField.setPlaceholder("e.g. admin or marketer");
        usernameField.setRequired(true);

        PasswordField passwordField = new PasswordField("Password");
        passwordField.setWidthFull();
        passwordField.setPlaceholder("Enter account password");
        passwordField.setRequired(true);

        Button loginButton = new Button("Sign In", e -> {
            String user = usernameField.getValue();
            String pass = passwordField.getValue();

            if (user == null || user.trim().isEmpty() || pass == null || pass.trim().isEmpty()) {
                showError("Please enter both username and password.");
                return;
            }

            try {
                authApiService.login(user.trim(), pass);
                errorBanner.setVisible(false);
                getUI().ifPresent(ui -> ui.navigate(""));
            } catch (ApiException apiEx) {
                showError(apiEx.getUserFriendlyMessage());
            } catch (Exception ex) {
                showError("Authentication failed: " + ex.getMessage());
            }
        });

        loginButton.addClassName("crm-btn-primary");
        loginButton.setWidthFull();
        loginButton.getStyle().set("margin-top", "16px").set("height", "40px");
        loginButton.addClickShortcut(Key.ENTER);

        cardContent.add(brandTitle, subtitle, errorBanner, usernameField, passwordField, loginButton);
        loginCard.add(cardContent);

        add(loginCard);
    }

    private void showError(String message) {
        errorBanner.removeAll();
        Span dot = new Span();
        dot.addClassName("crm-status-dot");
        Span msg = new Span(message);
        errorBanner.add(dot, msg);
        errorBanner.setVisible(true);
    }
}
