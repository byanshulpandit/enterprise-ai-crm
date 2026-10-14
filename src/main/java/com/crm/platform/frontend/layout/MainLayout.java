package com.crm.platform.frontend.layout;

import com.crm.platform.frontend.components.common.StatusBadge;
import com.crm.platform.frontend.security.FrontendSecurityContext;
import com.crm.platform.frontend.security.UserSession;
import com.crm.platform.frontend.service.AuthApiService;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;

/**
 * Enterprise Application Shell Layout providing the top utility navbar and collapsible role-aware drawer.
 */
public class MainLayout extends AppLayout {

    private final AuthApiService authApiService;

    public MainLayout(AuthApiService authApiService) {
        this.authApiService = authApiService;
        addClassName("crm-app-shell");

        createHeader();
        createDrawer();
    }

    private void createHeader() {
        DrawerToggle toggle = new DrawerToggle();
        toggle.getStyle().set("color", "var(--crm-text-primary)");

        H1 title = new H1("ENTERPRISE AI-CRM");
        title.getStyle()
                .set("font-size", "15px")
                .set("font-weight", "700")
                .set("letter-spacing", "0.02em")
                .set("margin", "0 8px 0 0")
                .set("color", "var(--crm-text-primary)");

        Span badge = new Span("PRO");
        badge.addClassName("crm-brand-badge");

        HorizontalLayout brandLayout = new HorizontalLayout(title, badge);
        brandLayout.setAlignItems(FlexComponent.Alignment.CENTER);

        HorizontalLayout left = new HorizontalLayout(toggle, brandLayout);
        left.setAlignItems(FlexComponent.Alignment.CENTER);

        UserSession session = FrontendSecurityContext.getCurrentSession();
        String username = session != null && session.getUsername() != null ? session.getUsername() : "User";
        String roleStr = session != null && session.getRole() != null ? session.getRole().name() : "ROLE_MARKETER";

        Div userChip = new Div();
        userChip.addClassName("crm-user-chip");

        Span userIcon = new Span(VaadinIcon.USER.create());
        userIcon.getStyle().set("color", "var(--crm-text-secondary)").set("font-size", "14px");

        Span userNameSpan = new Span(username);
        userNameSpan.getStyle().set("font-size", "12px").set("font-weight", "600").set("color", "var(--crm-text-primary)");

        StatusBadge roleBadge = new StatusBadge(roleStr.replace("ROLE_", ""));

        userChip.add(userIcon, userNameSpan, roleBadge);

        Button logoutBtn = new Button("Sign Out", e -> {
            authApiService.logout();
            getUI().ifPresent(ui -> ui.navigate("login"));
        });
        logoutBtn.addClassName("crm-btn-secondary");
        logoutBtn.getStyle().set("font-size", "12px").set("padding", "4px 12px");

        HorizontalLayout right = new HorizontalLayout(userChip, logoutBtn);
        right.setAlignItems(FlexComponent.Alignment.CENTER);
        right.setSpacing(true);

        HorizontalLayout navbar = new HorizontalLayout(left, right);
        navbar.addClassName("crm-navbar");
        navbar.setWidthFull();
        navbar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        navbar.setAlignItems(FlexComponent.Alignment.CENTER);

        addToNavbar(navbar);
    }

    private void createDrawer() {
        VerticalLayout drawerLayout = new VerticalLayout();
        drawerLayout.setPadding(false);
        drawerLayout.setSpacing(false);
        drawerLayout.setWidthFull();

        Div drawerHeader = new Div();
        drawerHeader.addClassName("crm-drawer-header");
        Span drawerTitle = new Span("Platform Navigation");
        drawerTitle.addClassName("crm-brand-title");
        drawerHeader.add(drawerTitle);

        SideNav operationalNav = new SideNav();
        operationalNav.setLabel("OPERATIONAL");
        operationalNav.addItem(new SideNavItem("Dashboard", "dashboard", VaadinIcon.DASHBOARD.create()));
        operationalNav.addItem(new SideNavItem("Customers", "customers", VaadinIcon.USERS.create()));
        operationalNav.addItem(new SideNavItem("Segments", "segments", VaadinIcon.SPLIT.create()));
        operationalNav.addItem(new SideNavItem("Campaigns", "campaigns", VaadinIcon.PAPERPLANE.create()));
        operationalNav.addItem(new SideNavItem("Bulk Upload", "uploads", VaadinIcon.UPLOAD.create()));
        operationalNav.addItem(new SideNavItem("Analytics", "reports", VaadinIcon.CHART.create()));

        drawerLayout.add(drawerHeader, operationalNav);

        if (FrontendSecurityContext.isAdmin()) {
            SideNav governanceNav = new SideNav();
            governanceNav.setLabel("GOVERNANCE");
            governanceNav.addItem(new SideNavItem("User Accounts", "admin/users", VaadinIcon.USER_CHECK.create()));
            governanceNav.addItem(new SideNavItem("AI Audits", "admin/ai-audits", VaadinIcon.FILE_TEXT.create()));
            drawerLayout.add(governanceNav);
        }

        Scroller scroller = new Scroller(drawerLayout);
        scroller.setClassName("crm-drawer");

        addToDrawer(scroller);
    }
}
