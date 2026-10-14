package com.crm.platform.frontend.views.admin;

import com.crm.platform.frontend.client.ApiException;
import com.crm.platform.frontend.service.UserApiService;
import com.crm.platform.frontend.util.NotificationUtils;
import com.crm.platform.user.dto.UserCreateRequest;
import com.crm.platform.user.dto.UserResponse;
import com.crm.platform.user.entity.RoleEnum;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;

import java.util.function.Consumer;

/**
 * Modal dialog for provisioning a new enterprise user.
 */
public class UserFormDialog extends Dialog {

    private final UserApiService userApiService;
    private final Consumer<UserResponse> onSuccess;

    private final TextField usernameField = new TextField("Username");
    private final EmailField emailField = new EmailField("Email Address");
    private final PasswordField passwordField = new PasswordField("Temporary Password");
    private final ComboBox<RoleEnum> roleCombo = new ComboBox<>("Assigned Authority");

    private final Button saveBtn = new Button("Provision User");
    private final Button cancelBtn = new Button("Cancel");

    public UserFormDialog(UserApiService userApiService, Consumer<UserResponse> onSuccess) {
        this.userApiService = userApiService;
        this.onSuccess = onSuccess;

        setWidth("460px");
        addClassName("crm-dialog");
        setCloseOnEsc(true);
        setCloseOnOutsideClick(false);

        initLayout();
    }

    private void initLayout() {
        VerticalLayout container = new VerticalLayout();
        container.setPadding(false);
        container.setSpacing(true);

        H3 title = new H3("Provision Enterprise User");
        title.getStyle().set("margin", "0 0 8px 0").set("color", "#f0f6fc");

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        usernameField.setRequired(true);
        usernameField.setPlaceholder("e.g. jdoe");
        usernameField.setMaxLength(50);
        usernameField.setWidthFull();

        emailField.setRequired(true);
        emailField.setPlaceholder("user@domain.com");
        emailField.setMaxLength(255);
        emailField.setWidthFull();

        passwordField.setRequired(true);
        passwordField.setPlaceholder("Enter initial password");
        passwordField.setWidthFull();

        roleCombo.setItems(RoleEnum.ROLE_ADMIN, RoleEnum.ROLE_MARKETER);
        roleCombo.setItemLabelGenerator(r -> r.name().replace("ROLE_", ""));
        roleCombo.setValue(RoleEnum.ROLE_MARKETER);
        roleCombo.setRequired(true);
        roleCombo.setWidthFull();

        form.add(usernameField, emailField, passwordField, roleCombo);

        HorizontalLayout footer = new HorizontalLayout();
        footer.setWidthFull();
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        footer.setSpacing(true);

        cancelBtn.addClassName("crm-btn-secondary");
        cancelBtn.addClickListener(e -> close());

        saveBtn.addClassName("crm-btn-primary");
        saveBtn.addClickListener(e -> handleSave());

        footer.add(cancelBtn, saveBtn);

        container.add(title, form, footer);
        add(container);
    }

    private void handleSave() {
        String username = usernameField.getValue() != null ? usernameField.getValue().trim() : "";
        String email = emailField.getValue() != null ? emailField.getValue().trim() : "";
        String password = passwordField.getValue() != null ? passwordField.getValue().trim() : "";
        RoleEnum role = roleCombo.getValue();

        if (username.isBlank()) {
            NotificationUtils.showWarning("Username is required.");
            usernameField.focus();
            return;
        }

        if (email.isBlank() || emailField.isInvalid()) {
            NotificationUtils.showWarning("A valid email address is required.");
            emailField.focus();
            return;
        }

        if (password.isBlank()) {
            NotificationUtils.showWarning("Temporary password is required.");
            passwordField.focus();
            return;
        }

        if (role == null) {
            NotificationUtils.showWarning("Role selection is required.");
            roleCombo.focus();
            return;
        }

        saveBtn.setEnabled(false);
        saveBtn.setText("Provisioning...");

        try {
            UserCreateRequest request = new UserCreateRequest(username, email, password, role);
            UserResponse response = userApiService.createUser(request);
            NotificationUtils.showSuccess("User @" + response.getUsername() + " successfully provisioned.");
            close();
            if (onSuccess != null) {
                onSuccess.accept(response);
            }
        } catch (ApiException e) {
            NotificationUtils.showError("Provisioning failed: " + e.getMessage());
            saveBtn.setEnabled(true);
            saveBtn.setText("Provision User");
        } catch (Exception e) {
            NotificationUtils.showError("Unexpected error: " + e.getMessage());
            saveBtn.setEnabled(true);
            saveBtn.setText("Provision User");
        }
    }
}
