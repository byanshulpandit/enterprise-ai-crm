package com.crm.platform.frontend.views.admin;

import com.crm.platform.frontend.client.ApiException;
import com.crm.platform.frontend.client.PageResult;
import com.crm.platform.frontend.components.common.ConfirmDialog;
import com.crm.platform.frontend.components.common.StateViewContainer;
import com.crm.platform.frontend.components.common.StatusBadge;
import com.crm.platform.frontend.layout.MainLayout;
import com.crm.platform.frontend.security.FrontendSecurityContext;
import com.crm.platform.frontend.service.UserApiService;
import com.crm.platform.frontend.util.FormattingUtils;
import com.crm.platform.frontend.util.NotificationUtils;
import com.crm.platform.user.dto.UserResponse;
import com.crm.platform.user.entity.RoleEnum;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;
import java.time.ZoneId;

/**
 * SCR-14: Administrative User Governance & Security Directory View.
 * Restricted strictly to users with ROLE_ADMIN.
 */
@Route(value = "admin/users", layout = MainLayout.class)
@RolesAllowed({"ADMIN", "ROLE_ADMIN"})
@PageTitle("User Administration | Enterprise AI-CRM")
public class UserManagementView extends VerticalLayout {

    private final UserApiService userApiService;
    private final StateViewContainer stateContainer;

    private final Grid<UserResponse> grid = new Grid<>(UserResponse.class, false);
    private final Button prevBtn = new Button("Previous");
    private final Button nextBtn = new Button("Next");
    private final Span pageLabel = new Span("Page 1");

    private int currentPage = 0;
    private final int pageSize = 15;
    private int totalPages = 1;

    public UserManagementView(UserApiService userApiService) {
        this.userApiService = userApiService;

        addClassName("crm-content-container");
        setWidthFull();
        setPadding(true);
        setSpacing(true);

        stateContainer = new StateViewContainer();
        add(stateContainer);

        initView();
        loadUsers();
    }

    private void initView() {
        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(true);
        content.setWidthFull();

        // Header Row
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        VerticalLayout titleBox = new VerticalLayout();
        titleBox.setPadding(false);
        titleBox.setSpacing(false);

        H2 title = new H2("Enterprise User Administration");
        title.getStyle().set("margin", "0 0 4px 0");
        title.getStyle().set("color", "#f0f6fc");

        Span subtitle = new Span("Manage staff credentials, authorization tiers, and account active states.");
        subtitle.getStyle().set("color", "#8b949e").set("font-size", "14px");
        titleBox.add(title, subtitle);

        HorizontalLayout actionControls = new HorizontalLayout();
        actionControls.setSpacing(true);

        Button refreshBtn = new Button("Refresh", VaadinIcon.REFRESH.create(), e -> loadUsers());
        refreshBtn.addClassName("crm-btn-secondary");

        Button addUserBtn = new Button("Provision User", VaadinIcon.PLUS.create(), e -> openUserFormDialog());
        addUserBtn.addClassName("crm-btn-primary");

        actionControls.add(refreshBtn, addUserBtn);
        header.add(titleBox, actionControls);
        content.add(header);

        // Configure Grid
        configureGrid();
        content.add(grid);

        // Pagination Bar
        HorizontalLayout paginationBar = createPaginationBar();
        content.add(paginationBar);

        stateContainer.showContent(content);
    }

    private void configureGrid() {
        grid.addClassName("crm-grid");
        grid.setWidthFull();
        grid.setAllRowsVisible(true);

        grid.addColumn(UserResponse::getId)
                .setHeader("ID")
                .setWidth("70px")
                .setFlexGrow(0);

        grid.addColumn(UserResponse::getUsername)
                .setHeader("Username")
                .setWidth("160px")
                .setFlexGrow(1);

        grid.addColumn(UserResponse::getEmail)
                .setHeader("Email")
                .setWidth("220px")
                .setFlexGrow(1);

        grid.addComponentColumn(user -> {
            RoleEnum role = user.getRole();
            if (role == RoleEnum.ROLE_ADMIN) {
                return new StatusBadge("ADMIN", StatusBadge.BadgeVariant.WARNING);
            } else {
                return new StatusBadge("MARKETER", StatusBadge.BadgeVariant.INFO);
            }
        }).setHeader("Role").setWidth("120px").setFlexGrow(0);

        grid.addComponentColumn(user -> {
            boolean active = Boolean.TRUE.equals(user.getIsActive());
            if (active) {
                return new StatusBadge("ACTIVE", StatusBadge.BadgeVariant.SUCCESS);
            } else {
                return new StatusBadge("DEACTIVATED", StatusBadge.BadgeVariant.NEUTRAL);
            }
        }).setHeader("Status").setWidth("130px").setFlexGrow(0);

        grid.addColumn(user -> FormattingUtils.formatInstant(user.getCreatedAt(), ZoneId.systemDefault()))
                .setHeader("Provisioned")
                .setWidth("160px")
                .setFlexGrow(0);

        // Action Menu Column
        grid.addComponentColumn(this::buildActionButtons)
                .setHeader("Actions")
                .setWidth("280px")
                .setFlexGrow(0);
    }

    private HorizontalLayout buildActionButtons(UserResponse user) {
        HorizontalLayout actions = new HorizontalLayout();
        actions.setSpacing(true);

        // Change Role button
        Button roleBtn = new Button("Role", VaadinIcon.USER_CHECK.create(), e -> openRoleChangeDialog(user));
        roleBtn.addClassName("crm-btn-secondary");
        roleBtn.getStyle().set("padding", "2px 8px").set("font-size", "12px");

        // Reset Password button
        Button pwdBtn = new Button("Password", VaadinIcon.KEY.create(), e -> openPasswordResetDialog(user));
        pwdBtn.addClassName("crm-btn-secondary");
        pwdBtn.getStyle().set("padding", "2px 8px").set("font-size", "12px");

        // Deactivate button
        boolean isActive = Boolean.TRUE.equals(user.getIsActive());
        boolean isCurrentUser = FrontendSecurityContext.getCurrentUsername()
                .map(curr -> curr.equalsIgnoreCase(user.getUsername()))
                .orElse(false);

        Button deactivateBtn = new Button("Deactivate", VaadinIcon.BAN.create());
        deactivateBtn.addClassName("crm-btn-danger");
        deactivateBtn.getStyle().set("padding", "2px 8px").set("font-size", "12px");
        deactivateBtn.setEnabled(isActive && !isCurrentUser);

        if (isCurrentUser) {
            deactivateBtn.setTooltipText("Cannot deactivate your own active session account.");
        } else if (!isActive) {
            deactivateBtn.setText("Inactive");
            deactivateBtn.setEnabled(false);
        } else {
            deactivateBtn.addClickListener(e -> confirmDeactivation(user));
        }

        actions.add(roleBtn, pwdBtn, deactivateBtn);
        return actions;
    }

    private void openRoleChangeDialog(UserResponse user) {
        Dialog roleDialog = new Dialog();
        roleDialog.setWidth("380px");
        roleDialog.addClassName("crm-dialog");

        VerticalLayout container = new VerticalLayout();
        container.setPadding(false);
        container.setSpacing(true);

        H3 title = new H3("Modify Role: @" + user.getUsername());
        title.getStyle().set("margin", "0").set("color", "#f0f6fc");

        ComboBox<RoleEnum> roleSelect = new ComboBox<>("Authority Level");
        roleSelect.setItems(RoleEnum.ROLE_ADMIN, RoleEnum.ROLE_MARKETER);
        roleSelect.setItemLabelGenerator(r -> r.name().replace("ROLE_", ""));
        roleSelect.setValue(user.getRole());
        roleSelect.setWidthFull();

        HorizontalLayout footer = new HorizontalLayout();
        footer.setWidthFull();
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        Button cancel = new Button("Cancel", e -> roleDialog.close());
        cancel.addClassName("crm-btn-secondary");

        Button save = new Button("Update Role", e -> {
            RoleEnum selected = roleSelect.getValue();
            if (selected == null || selected == user.getRole()) {
                roleDialog.close();
                return;
            }
            try {
                userApiService.updateUserRole(user.getId(), selected);
                NotificationUtils.showSuccess("Role for @" + user.getUsername() + " updated to " + selected.name());
                roleDialog.close();
                loadUsers();
            } catch (ApiException ex) {
                NotificationUtils.showError("Failed to update role: " + ex.getMessage());
            }
        });
        save.addClassName("crm-btn-primary");

        footer.add(cancel, save);
        container.add(title, roleSelect, footer);
        roleDialog.add(container);
        roleDialog.open();
    }

    private void openPasswordResetDialog(UserResponse user) {
        PasswordResetDialog dialog = new PasswordResetDialog(userApiService, user, this::loadUsers);
        dialog.open();
    }

    private void confirmDeactivation(UserResponse user) {
        ConfirmDialog confirmDialog = new ConfirmDialog(
                "Deactivate User Account",
                "Are you sure you want to deactivate @" + user.getUsername() + "? This user will immediately lose access to all CRM endpoints.",
                "Deactivate Account",
                () -> {
                    try {
                        userApiService.deactivateUser(user.getId());
                        NotificationUtils.showSuccess("User @" + user.getUsername() + " deactivated.");
                        loadUsers();
                    } catch (ApiException e) {
                        NotificationUtils.showError("Deactivation failed: " + e.getMessage());
                    } catch (Exception e) {
                        NotificationUtils.showError("Unexpected error: " + e.getMessage());
                    }
                }
        );
        confirmDialog.open();
    }

    private void openUserFormDialog() {
        UserFormDialog dialog = new UserFormDialog(userApiService, u -> loadUsers());
        dialog.open();
    }

    private void loadUsers() {
        try {
            PageResult<UserResponse> page = userApiService.listUsers(currentPage, pageSize);
            if (page != null && page.getContent() != null) {
                grid.setItems(page.getContent());
                totalPages = Math.max(1, page.getTotalPages());
                updatePagination();
            } else {
                grid.setItems();
            }
        } catch (ApiException e) {
            NotificationUtils.showError("Failed to retrieve user directory: " + e.getMessage());
        } catch (Exception e) {
            NotificationUtils.showError("Error loading users: " + e.getMessage());
        }
    }

    private HorizontalLayout createPaginationBar() {
        HorizontalLayout bar = new HorizontalLayout();
        bar.setWidthFull();
        bar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        bar.setAlignItems(FlexComponent.Alignment.CENTER);

        prevBtn.addClassName("crm-btn-secondary");
        prevBtn.addClickListener(e -> {
            if (currentPage > 0) {
                currentPage--;
                loadUsers();
            }
        });

        nextBtn.addClassName("crm-btn-secondary");
        nextBtn.addClickListener(e -> {
            if (currentPage < totalPages - 1) {
                currentPage++;
                loadUsers();
            }
        });

        pageLabel.getStyle().set("color", "#8b949e").set("font-size", "13px");

        bar.add(prevBtn, pageLabel, nextBtn);
        return bar;
    }

    private void updatePagination() {
        pageLabel.setText(String.format("Page %d of %d", currentPage + 1, totalPages));
        prevBtn.setEnabled(currentPage > 0);
        nextBtn.setEnabled(currentPage < totalPages - 1);
    }
}
