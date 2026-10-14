package com.crm.platform.frontend.views.customers;

import com.crm.platform.customer.dto.CustomerPatchRequestDto;
import com.crm.platform.customer.dto.CustomerRequestDto;
import com.crm.platform.customer.dto.CustomerResponseDto;
import com.crm.platform.frontend.client.ApiException;
import com.crm.platform.frontend.service.CustomerApiService;
import com.crm.platform.frontend.util.NotificationUtils;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Modal form for creating and editing customer profiles.
 */
public class CustomerFormDialog extends Dialog {

    private final CustomerApiService customerApiService;
    private final CustomerResponseDto existingCustomer;
    private final Consumer<CustomerResponseDto> onSaved;

    private final TextField firstNameField = new TextField("First Name");
    private final TextField lastNameField = new TextField("Last Name");
    private final EmailField emailField = new EmailField("Email");
    private final TextField phoneField = new TextField("Phone");
    private final TextField cityField = new TextField("City");
    private final TextField countryField = new TextField("Country");
    private final BigDecimalField totalSpendField = new BigDecimalField("Total Spend (₹)");
    private final IntegerField visitCountField = new IntegerField("Visit Count");
    private final DatePicker lastActiveDatePicker = new DatePicker("Last Active Date");
    private final TextField tagsField = new TextField("Tags (comma separated)");

    public CustomerFormDialog(CustomerApiService customerApiService, CustomerResponseDto customerToEdit, Consumer<CustomerResponseDto> onSaved) {
        this.customerApiService = customerApiService;
        this.existingCustomer = customerToEdit;
        this.onSaved = onSaved;

        setCloseOnEsc(true);
        setCloseOnOutsideClick(false);
        setWidth("600px");

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(true);
        layout.setSpacing(true);

        boolean isEdit = existingCustomer != null;
        H3 title = new H3(isEdit ? "Edit Customer Profile" : "Create Customer Profile");
        title.getStyle().set("margin", "0 0 12px 0").set("color", "var(--crm-text-primary)");

        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );

        firstNameField.setRequired(true);
        lastNameField.setRequired(true);
        emailField.setRequiredIndicatorVisible(true);

        totalSpendField.setValue(BigDecimal.ZERO);
        visitCountField.setValue(0);
        visitCountField.setMin(0);

        formLayout.add(firstNameField, lastNameField);
        formLayout.add(emailField, phoneField);
        formLayout.add(cityField, countryField);
        formLayout.add(totalSpendField, visitCountField);
        formLayout.add(lastActiveDatePicker, tagsField);

        if (isEdit) {
            firstNameField.setValue(existingCustomer.getFirstName() != null ? existingCustomer.getFirstName() : "");
            lastNameField.setValue(existingCustomer.getLastName() != null ? existingCustomer.getLastName() : "");
            emailField.setValue(existingCustomer.getEmail() != null ? existingCustomer.getEmail() : "");
            phoneField.setValue(existingCustomer.getPhone() != null ? existingCustomer.getPhone() : "");
            cityField.setValue(existingCustomer.getCity() != null ? existingCustomer.getCity() : "");
            countryField.setValue(existingCustomer.getCountry() != null ? existingCustomer.getCountry() : "");
            if (existingCustomer.getTotalSpend() != null) totalSpendField.setValue(existingCustomer.getTotalSpend());
            if (existingCustomer.getVisitCount() != null) visitCountField.setValue(existingCustomer.getVisitCount());
            if (existingCustomer.getLastActiveDate() != null) lastActiveDatePicker.setValue(existingCustomer.getLastActiveDate());
            if (existingCustomer.getTags() != null) {
                tagsField.setValue(String.join(", ", existingCustomer.getTags()));
            }
        }

        Button cancelBtn = new Button("Cancel", e -> close());
        cancelBtn.addClassName("crm-btn-secondary");

        Button saveBtn = new Button(isEdit ? "Save Changes" : "Create Customer", e -> saveCustomer());
        saveBtn.addClassName("crm-btn-primary");

        HorizontalLayout actions = new HorizontalLayout(cancelBtn, saveBtn);
        actions.setWidthFull();
        actions.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        layout.add(title, formLayout, actions);
        add(layout);
    }

    private void saveCustomer() {
        if (firstNameField.isEmpty() || lastNameField.isEmpty() || emailField.isEmpty() || emailField.isInvalid()) {
            NotificationUtils.showError("Please provide valid First Name, Last Name, and Email.");
            return;
        }

        Set<String> tags = new HashSet<>();
        if (!tagsField.isEmpty()) {
            tags = Arrays.stream(tagsField.getValue().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());
        }

        try {
            CustomerResponseDto saved;
            if (existingCustomer == null) {
                CustomerRequestDto req = new CustomerRequestDto(
                        firstNameField.getValue().trim(),
                        lastNameField.getValue().trim(),
                        emailField.getValue().trim(),
                        phoneField.getValue() != null ? phoneField.getValue().trim() : null,
                        cityField.getValue() != null ? cityField.getValue().trim() : null,
                        countryField.getValue() != null ? countryField.getValue().trim() : null,
                        totalSpendField.getValue() != null ? totalSpendField.getValue() : BigDecimal.ZERO,
                        visitCountField.getValue() != null ? visitCountField.getValue() : 0,
                        lastActiveDatePicker.getValue(),
                        tags
                );
                saved = customerApiService.createCustomer(req);
                NotificationUtils.showSuccess("Customer created successfully.");
            } else {
                CustomerPatchRequestDto patch = new CustomerPatchRequestDto();
                patch.setFirstName(firstNameField.getValue().trim());
                patch.setLastName(lastNameField.getValue().trim());
                patch.setEmail(emailField.getValue().trim());
                patch.setPhone(phoneField.getValue() != null ? phoneField.getValue().trim() : null);
                patch.setCity(cityField.getValue() != null ? cityField.getValue().trim() : null);
                patch.setCountry(countryField.getValue() != null ? countryField.getValue().trim() : null);
                patch.setTotalSpend(totalSpendField.getValue());
                patch.setVisitCount(visitCountField.getValue());
                patch.setLastActiveDate(lastActiveDatePicker.getValue());
                patch.setTags(tags);
                saved = customerApiService.patchCustomer(existingCustomer.getId(), patch);
                NotificationUtils.showSuccess("Customer profile updated.");
            }

            close();
            if (onSaved != null) {
                onSaved.accept(saved);
            }
        } catch (ApiException apiEx) {
            NotificationUtils.showError(apiEx.getUserFriendlyMessage());
        } catch (Exception ex) {
            NotificationUtils.showError("Error saving customer: " + ex.getMessage());
        }
    }
}
