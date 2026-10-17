package com.crm.platform.frontend.views.login;

import com.crm.platform.frontend.client.ApiException;
import com.crm.platform.frontend.service.AuthApiService;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * SCR-01: Enterprise Authentication View for Enterprise AI-CRM Platform (CS-CRM-2026).
 * Follows understated, polished enterprise SaaS styling with real backend JWT authentication,
 * input validation, and double-submission protection.
 */
@Route("login")
@PageTitle("Sign In | Enterprise AI-CRM")
public class LoginView extends VerticalLayout {

    private final AuthApiService authApiService;
    private final Div errorBanner;
    private final TextField usernameField;
    private final PasswordField passwordField;
    private final Button loginButton;

    public LoginView(AuthApiService authApiService) {
        this.authApiService = authApiService;

        setSizeFull();
        setMargin(false);
        setPadding(false);
        setSpacing(false);
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        addClassName("crm-login-wrapper");

        Div loginContainer = new Div();
        loginContainer.addClassName("crm-login-container");

        // ----------------------------------------------------
        // Left Side: Enterprise Platform Architecture Overview
        // ----------------------------------------------------
        Div heroSide = new Div();
        heroSide.addClassName("crm-login-hero");

        Div heroHeader = new Div();
        heroHeader.addClassName("crm-login-hero-header");

        Div brandIcon = new Div();
        brandIcon.addClassName("crm-login-brand-icon");
        brandIcon.setText("AI");

        H1 heroTitle = new H1("Enterprise AI-CRM");
        heroTitle.addClassName("crm-login-hero-title");

        Paragraph heroTagline = new Paragraph("Centralized customer intelligence, recursive segment synthesis, and event-driven campaign delivery.");
        heroTagline.addClassName("crm-login-hero-tagline");

        heroHeader.add(brandIcon, heroTitle, heroTagline);

        Div featureList = new Div();
        featureList.addClassName("crm-feature-list");
        featureList.add(createFeatureItem(VaadinIcon.DATABASE, "Customer Master Directory", "High-throughput ingestion and lifecycle tracking with Keyset pagination."));
        featureList.add(createFeatureItem(VaadinIcon.SPLIT, "Deterministic Segment Studio", "Recursive AST boolean expression engine compiling directly to optimized SQL queries."));
        featureList.add(createFeatureItem(VaadinIcon.PAPERPLANE, "Campaign Delivery Pipeline", "Transactional outbox pattern with Redis Streams for at-least-once message dispatch."));

        Div heroFooter = new Div();
        heroFooter.addClassName("crm-hero-footer-note");
        heroFooter.setText("Enterprise Monolith Architecture • Spring Boot 3 & Vaadin Flow");

        heroSide.add(heroHeader, featureList, heroFooter);

        // ----------------------------------------------------
        // Right Side: Authentication Form
        // ----------------------------------------------------
        Div formSide = new Div();
        formSide.addClassName("crm-login-form-side");

        Div formHeader = new Div();
        formHeader.addClassName("crm-form-header");
        H2 formTitle = new H2("Corporate Sign In");
        formTitle.addClassName("crm-form-title");
        Paragraph formSub = new Paragraph("Enter your account credentials to access the console.");
        formSub.addClassName("crm-form-subtitle");
        formHeader.add(formTitle, formSub);

        // Error Banner
        errorBanner = new Div();
        errorBanner.addClassName("crm-status-pill");
        errorBanner.addClassName("crm-status-error");
        errorBanner.setWidthFull();
        errorBanner.setVisible(false);
        errorBanner.getStyle().set("margin-bottom", "14px").set("padding", "8px 12px");

        // Input Fields (Empty by default, no exposed credentials)
        usernameField = new TextField("Username or Corporate Email");
        usernameField.setWidthFull();
        usernameField.setPlaceholder("Username or corporate email");
        usernameField.setPrefixComponent(VaadinIcon.USER.create());
        usernameField.setRequired(true);
        usernameField.setAutofocus(true);

        passwordField = new PasswordField("Password");
        passwordField.setWidthFull();
        passwordField.setPlaceholder("Account password");
        passwordField.setPrefixComponent(VaadinIcon.LOCK.create());
        passwordField.setRequired(true);

        // Submit Button with Double-Submission Protection
        loginButton = new Button("Sign In", VaadinIcon.SIGN_IN.create(), e -> handleLogin());
        loginButton.addClassName("crm-btn-primary");
        loginButton.addClassName("crm-login-submit-btn");
        loginButton.setWidthFull();
        loginButton.addClickShortcut(Key.ENTER);

        // Understated Footer Notice
        Div footerNotice = new Div();
        footerNotice.addClassName("crm-login-footer-notice");
        footerNotice.setText("Authorized enterprise access only. All sessions and actions are logged.");

        formSide.add(
                formHeader,
                errorBanner,
                usernameField,
                passwordField,
                loginButton,
                footerNotice
        );

        loginContainer.add(heroSide, formSide);
        add(loginContainer);
    }

    private Div createFeatureItem(VaadinIcon icon, String title, String desc) {
        Div item = new Div();
        item.addClassName("crm-feature-item");

        Icon ic = icon.create();
        ic.addClassName("crm-feature-icon");

        Div textBlock = new Div();
        textBlock.addClassName("crm-feature-text");
        Span strongTitle = new Span(title);
        strongTitle.getStyle().set("font-weight", "600").set("color", "var(--crm-text-primary)").set("display", "block");
        Span descSpan = new Span(desc);
        textBlock.add(strongTitle, descSpan);

        item.add(ic, textBlock);
        return item;
    }

    private void handleLogin() {
        String user = usernameField.getValue();
        String pass = passwordField.getValue();

        if (user == null || user.trim().isEmpty() || pass == null || pass.trim().isEmpty()) {
            showError("Please enter both username/email and password.");
            return;
        }

        loginButton.setEnabled(false);
        loginButton.setText("Authenticating...");

        try {
            authApiService.login(user.trim(), pass);
            errorBanner.setVisible(false);
            getUI().ifPresent(ui -> ui.navigate(""));
        } catch (ApiException apiEx) {
            showError(apiEx.getUserFriendlyMessage());
            resetLoginButton();
        } catch (Exception ex) {
            showError("Authentication failed: " + ex.getMessage());
            resetLoginButton();
        }
    }

    private void resetLoginButton() {
        loginButton.setEnabled(true);
        loginButton.setText("Sign In");
        loginButton.setIcon(VaadinIcon.SIGN_IN.create());
    }

    private void showError(String message) {
        errorBanner.removeAll();
        Icon warnIcon = VaadinIcon.EXCLAMATION_CIRCLE.create();
        warnIcon.getStyle().set("font-size", "13px").set("margin-right", "6px").set("flex-shrink", "0");
        Span msg = new Span(message);
        errorBanner.add(warnIcon, msg);
        errorBanner.setVisible(true);
    }
}
