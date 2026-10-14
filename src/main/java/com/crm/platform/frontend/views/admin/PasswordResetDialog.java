package com.crm.platform.frontend.views.admin;

import com.crm.platform.frontend.client.ApiException;
import com.crm.platform.frontend.service.UserApiService;
import com.crm.platform.frontend.util.NotificationUtils;
import com.crm.platform.user.dto.UserResponse;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;

/**
 * Modal dialog for administrative password reset of enterprise accounts.
 */
public class PasswordResetDialog extends Dialog {

    private final UserApiService userApiService;
    private final UserResponse targetUser;
    private final Runnable onSuccess;

    private final PasswordField newPasswordField = new PasswordField("New Password");
    private final PasswordField confirmPasswordField = new PasswordField("Confirm Password");
    private final Button submitBtn = new Button("Update Password");
    private final Button cancelBtn = new Button("Cancel");

    public PasswordResetDialog(UserApiService userApiService, UserResponse targetUser, Runnable onSuccess) {
        this.userApiService = userApiService;
        this.targetUser = targetUser;
        this.onSuccess = onSuccess;

        setWidth("420px");
        addClassName("crm-dialog");
        setCloseOnEsc(true);
        setCloseOnOutsideClick(false);

        initLayout();
    }

    private void initLayout() {
        VerticalLayout container = new VerticalLayout();
        container.setPadding(false);
        container.setSpacing(true);

        H3 title = new H3("Reset Password: @" + targetUser.getUsername());
        title.getStyle().set("margin", "0 0 4px 0").set("color", "#f0f6fc");

        Paragraph sub = new Paragraph("Enter an updated credential for user #" + targetUser.getId() + " (" + targetUser.getEmail() + ").");
        sub.getStyle().set("margin", "0 0 12px 0").set("color", "#8b949e").set("font-size", "13px");

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        newPasswordField.setRequired(true);
        newPasswordField.setPlaceholder("Enter new password");
        newPasswordField.setWidthFull();

        confirmPasswordField.setRequired(true);
        confirmPasswordField.setPlaceholder("Re-enter new password");
        confirmPasswordField.setWidthFull();

        form.add(newPasswordField, confirmPasswordField);

        HorizontalLayout footer = new HorizontalLayout();
        footer.setWidthFull();
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        footer.setSpacing(true);

        cancelBtn.addClassName("crm-btn-secondary");
        cancelBtn.addClickListener(e -> close());

        submitBtn.addClassName("crm-btn-primary");
        submitBtn.addClickListener(e -> handleReset());

        footer.add(cancelBtn, submitBtn);

        container.add(title, sub, form, footer);
        add(container);
    }

    private void handleReset() {
        String newPass = newPasswordField.getValue() != null ? newPasswordField.getValue().trim() : "";
        String confirmPass = confirmPasswordField.getValue() != null ? confirmPasswordField.getValue().trim() : "";

        if (newPass.isBlank()) {
            NotificationUtils.showWarning("New password cannot be blank.");
            newPasswordField.focus();
            return;
        }

        if (!newPass.equals(confirmPass)) {
            NotificationUtils.showWarning("Passwords do not match.");
            confirmPasswordField.focus();
            return;
        }

        submitBtn.setEnabled(false);
        submitBtn.setText("Updating...");

        try {
            userApiService.updateUserPassword(targetUser.getId(), newPass);
            NotificationUtils.showSuccess("Password for @" + targetUser.getUsername() + " successfully updated.");
            close();
            if (onSuccess != null) {
                onSuccess.run();
            }
        } catch (ApiException e) {
            NotificationUtils.showError("Failed to update password: " + e.getMessage());
            submitBtn.setEnabled(true);
            submitBtn.setText("Update Password");
        } catch (Exception e) {
            NotificationUtils.showError("Unexpected error updating password: " + e.getMessage());
            submitBtn.setEnabled(true);
            submitBtn.setText("Update Password");
        }
    }
}
