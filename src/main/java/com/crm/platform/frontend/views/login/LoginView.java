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
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * SCR-01: Enterprise-Grade Authentication Screen for the Enterprise AI-CRM Platform (CS-CRM-2026).
 * Features rich ambient aesthetics, 1-click role credential switching, responsive split layout,
 * and double-submission protection.
 */
@Route("login")
@PageTitle("Login | Enterprise AI-CRM Platform")
public class LoginView extends VerticalLayout {

    private final AuthApiService authApiService;
    private final Div errorBanner;
    private final TextField usernameField;
    private final PasswordField passwordField;
    private final Button loginButton;
    private Div adminRoleBtn;
    private Div marketerRoleBtn;

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
        // Left Side: Brand Showcase & Enterprise Architecture
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

        Paragraph heroTagline = new Paragraph("High-throughput customer intelligence, recursive segment synthesis, and transactional delivery telemetry.");
        heroTagline.addClassName("crm-login-hero-tagline");

        heroHeader.add(brandIcon, heroTitle, heroTagline);

        Div featureList = new Div();
        featureList.addClassName("crm-feature-list");
        featureList.add(createFeatureItem(VaadinIcon.BOLT, "Event-Driven AST Engine", "Recursive boolean criteria trees compiled to optimized SQL with Keyset pagination."));
        featureList.add(createFeatureItem(VaadinIcon.CONNECT_O, "Redis 7 Stream Telemetry", "Transactional outbox pattern with at-least-once distributed delivery consumers."));
        featureList.add(createFeatureItem(VaadinIcon.MAGIC, "Gemini AI Copilot", "Natural language segment rule synthesis, automated audits, and campaign analytics."));

        Div heroFooter = new Div();
        Div statusBadge = new Div();
        statusBadge.addClassName("crm-hero-footer-status");
        Span pulseDot = new Span();
        pulseDot.addClassName("crm-pulse-dot");
        Span statusText = new Span("All Core Services Operational");
        statusBadge.add(pulseDot, statusText);
        heroFooter.add(statusBadge);

        heroSide.add(heroHeader, featureList, heroFooter);

        // ----------------------------------------------------
        // Right Side: Authentication Form Card
        // ----------------------------------------------------
        Div formSide = new Div();
        formSide.addClassName("crm-login-form-side");

        Div formHeader = new Div();
        formHeader.addClassName("crm-form-header");
        H2 formTitle = new H2("Corporate Sign In");
        formTitle.addClassName("crm-form-title");
        Paragraph formSub = new Paragraph("Authenticate with role credentials or single sign-on.");
        formSub.addClassName("crm-form-subtitle");
        formHeader.add(formTitle, formSub);

        // 1-Click Role Switcher
        Div roleSwitcherLabel = new Div();
        roleSwitcherLabel.addClassName("crm-role-switcher-label");
        Span roleLabelText = new Span("Quick Credential Presets");
        Span clickHint = new Span("1-Click Select");
        clickHint.getStyle().set("color", "var(--crm-accent-primary)").set("font-size", "10px");
        roleSwitcherLabel.add(roleLabelText, clickHint);

        Div rolePills = new Div();
        rolePills.addClassName("crm-role-pills");

        adminRoleBtn = createRolePill("Administrator", "admin", "AdminPassword123!", true);
        marketerRoleBtn = createRolePill("Marketing Lead", "marketer", "Marketer123!", false);
        rolePills.add(adminRoleBtn, marketerRoleBtn);

        // Error Banner
        errorBanner = new Div();
        errorBanner.addClassName("crm-status-pill");
        errorBanner.addClassName("crm-status-error");
        errorBanner.setWidthFull();
        errorBanner.setVisible(false);
        errorBanner.getStyle().set("margin-bottom", "14px").set("padding", "8px 12px");

        // Input Fields
        usernameField = new TextField("Username or Email");
        usernameField.setWidthFull();
        usernameField.setValue("admin");
        usernameField.setPlaceholder("e.g. admin or marketer");
        usernameField.setPrefixComponent(VaadinIcon.USER.create());
        usernameField.setRequired(true);

        passwordField = new PasswordField("Password");
        passwordField.setWidthFull();
        passwordField.setValue("AdminPassword123!");
        passwordField.setPlaceholder("Enter account password");
        passwordField.setPrefixComponent(VaadinIcon.LOCK.create());
        passwordField.setRequired(true);

        // Submit Button with Double-Submit Prevention
        loginButton = new Button("Sign In to Platform", VaadinIcon.SIGN_IN.create(), e -> handleLogin());
        loginButton.addClassName("crm-btn-primary");
        loginButton.addClassName("crm-login-submit-btn");
        loginButton.setWidthFull();
        loginButton.addClickShortcut(Key.ENTER);

        // Security Compliance Footer
        Div complianceFooter = new Div();
        complianceFooter.addClassName("crm-login-compliance");
        HorizontalLayout socBadge = new HorizontalLayout();
        socBadge.setSpacing(true);
        socBadge.setAlignItems(FlexComponent.Alignment.CENTER);
        Icon shieldIcon = VaadinIcon.SHIELD.create();
        shieldIcon.getStyle().set("font-size", "12px").set("color", "var(--crm-text-muted)");
        Span socText = new Span("SOC-2 Type II Certified");
        socBadge.add(shieldIcon, socText);

        Span tlsText = new Span("TLS 1.3 Strict Transport");
        complianceFooter.add(socBadge, tlsText);

        formSide.add(
                formHeader,
                roleSwitcherLabel,
                rolePills,
                errorBanner,
                usernameField,
                passwordField,
                loginButton,
                complianceFooter
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

    private Div createRolePill(String roleLabel, String username, String password, boolean active) {
        Div pill = new Div();
        pill.addClassName("crm-role-pill-btn");
        if (active) {
            pill.addClassName("active");
        }

        Div pillTitle = new Div();
        pillTitle.addClassName("crm-role-pill-title");
        Icon pillIcon = active ? VaadinIcon.COG.create() : VaadinIcon.BULLHORN.create();
        pillIcon.getStyle().set("font-size", "11px");
        Span titleSpan = new Span(roleLabel);
        pillTitle.add(pillIcon, titleSpan);

        Div pillSub = new Div();
        pillSub.addClassName("crm-role-pill-sub");
        pillSub.setText(username + " / ••••••••");

        pill.add(pillTitle, pillSub);

        pill.addClickListener(e -> {
            usernameField.setValue(username);
            passwordField.setValue(password);
            adminRoleBtn.removeClassName("active");
            marketerRoleBtn.removeClassName("active");
            pill.addClassName("active");
            errorBanner.setVisible(false);
        });

        return pill;
    }

    private void handleLogin() {
        String user = usernameField.getValue();
        String pass = passwordField.getValue();

        if (user == null || user.trim().isEmpty() || pass == null || pass.trim().isEmpty()) {
            showError("Please enter both username and password.");
            return;
        }

        loginButton.setEnabled(false);
        loginButton.setText("Authenticating Session...");

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
        loginButton.setText("Sign In to Platform");
        loginButton.setIcon(VaadinIcon.SIGN_IN.create());
    }

    private void showError(String message) {
        errorBanner.removeAll();
        Icon warnIcon = VaadinIcon.WARNING.create();
        warnIcon.getStyle().set("font-size", "13px").set("margin-right", "6px").set("flex-shrink", "0");
        Span msg = new Span(message);
        errorBanner.add(warnIcon, msg);
        errorBanner.setVisible(true);
    }
}
