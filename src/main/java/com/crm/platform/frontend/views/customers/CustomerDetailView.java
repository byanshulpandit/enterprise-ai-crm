package com.crm.platform.frontend.views.customers;

import com.crm.platform.customer.dto.CustomerResponseDto;
import com.crm.platform.frontend.client.ApiException;
import com.crm.platform.frontend.components.common.ConfirmDialog;
import com.crm.platform.frontend.components.common.KpiStatCard;
import com.crm.platform.frontend.components.common.StateViewContainer;
import com.crm.platform.frontend.layout.MainLayout;
import com.crm.platform.frontend.security.FrontendSecurityContext;
import com.crm.platform.frontend.service.CustomerApiService;
import com.crm.platform.frontend.util.FormattingUtils;
import com.crm.platform.frontend.util.NotificationUtils;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.ZoneId;

/**
 * SCR-04: Customer Profile Inspector displaying aggregate commercial metrics and metadata.
 */
@Route(value = "customers/:id", layout = MainLayout.class)
@PageTitle("Customer Profile | Enterprise AI-CRM")
public class CustomerDetailView extends VerticalLayout implements BeforeEnterObserver {

    private final CustomerApiService customerApiService;
    private final StateViewContainer stateContainer;
    private Long customerId;

    public CustomerDetailView(CustomerApiService customerApiService) {
        this.customerApiService = customerApiService;

        addClassName("crm-content-container");
        setWidthFull();
        setPadding(true);
        setSpacing(true);

        stateContainer = new StateViewContainer();
        add(stateContainer);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String idParam = event.getRouteParameters().get("id").orElse(null);
        if (idParam == null) {
            stateContainer.showError("Customer ID parameter is missing.", () -> getUI().ifPresent(ui -> ui.navigate("customers")));
            return;
        }

        try {
            this.customerId = Long.parseLong(idParam);
            loadCustomerProfile();
        } catch (NumberFormatException e) {
            stateContainer.showError("Invalid Customer ID: " + idParam, () -> getUI().ifPresent(ui -> ui.navigate("customers")));
        }
    }

    private void loadCustomerProfile() {
        stateContainer.showLoading("Loading customer profile #" + customerId + "...");

        try {
            CustomerResponseDto customer = customerApiService.getCustomerById(customerId);
            if (customer == null) {
                stateContainer.showEmpty("Customer Not Found", "No customer found with identifier #" + customerId);
                return;
            }

            VerticalLayout content = new VerticalLayout();
            content.setPadding(false);
            content.setSpacing(true);
            content.setWidthFull();

            // Navigation Bar
            Button backBtn = new Button("← Back to Customers", e -> getUI().ifPresent(ui -> ui.navigate("customers")));
            backBtn.addClassName("crm-btn-secondary");
            content.add(backBtn);

            // Header Row
            HorizontalLayout header = new HorizontalLayout();
            header.setWidthFull();
            header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
            header.setAlignItems(FlexComponent.Alignment.CENTER);

            Div headerLeft = new Div();
            H2 nameHeader = new H2(customer.getFirstName() + " " + customer.getLastName());
            nameHeader.addClassName("crm-view-title");

            Paragraph metadata = new Paragraph("Customer Identifier: #" + customer.getId() + " • Registered: " +
                    (customer.getCreatedAt() != null ? FormattingUtils.formatDateTime(customer.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime()) : "—"));
            metadata.addClassName("crm-view-subtitle");
            headerLeft.add(nameHeader, metadata);

            HorizontalLayout actionBtns = new HorizontalLayout();

            Button editBtn = new Button("Edit Profile", VaadinIcon.EDIT.create(), e -> {
                CustomerFormDialog dialog = new CustomerFormDialog(customerApiService, customer, saved -> loadCustomerProfile());
                dialog.open();
            });
            editBtn.addClassName("crm-btn-secondary");
            actionBtns.add(editBtn);

            if (FrontendSecurityContext.isAdmin()) {
                Button deleteBtn = new Button("Delete Profile", VaadinIcon.TRASH.create(), e -> {
                    ConfirmDialog confirm = new ConfirmDialog(
                            "Delete Customer Profile",
                            "Are you sure you want to delete " + customer.getFirstName() + " " + customer.getLastName() + "?",
                            "Delete",
                            true,
                            () -> {
                                try {
                                    customerApiService.deleteCustomer(customer.getId());
                                    NotificationUtils.showSuccess("Customer record deleted.");
                                    getUI().ifPresent(ui -> ui.navigate("customers"));
                                } catch (ApiException ex) {
                                    NotificationUtils.showError(ex.getUserFriendlyMessage());
                                }
                            }
                    );
                    confirm.open();
                });
                deleteBtn.addClassName("crm-btn-danger");
                actionBtns.add(deleteBtn);
            }

            header.add(headerLeft, actionBtns);
            content.add(header);

            // KPI Grid
            Div kpiGrid = new Div();
            kpiGrid.addClassName("crm-kpi-grid");

            kpiGrid.add(new KpiStatCard(
                    "Total Spend",
                    FormattingUtils.formatCurrency(customer.getTotalSpend()),
                    "Aggregate verified spend"
            ));

            kpiGrid.add(new KpiStatCard(
                    "Total Visits",
                    FormattingUtils.formatInteger(customer.getVisitCount()),
                    "Session interactions logged"
            ));

            kpiGrid.add(new KpiStatCard(
                    "Last Active Date",
                    FormattingUtils.formatDate(customer.getLastActiveDate()),
                    "Most recent activity recorded"
            ));

            content.add(kpiGrid);

            // Profile Detail Cards
            HorizontalLayout panelsRow = new HorizontalLayout();
            panelsRow.setWidthFull();
            panelsRow.setSpacing(true);

            // Contact Info
            Div contactPanel = new Div();
            contactPanel.addClassName("crm-panel");
            contactPanel.setWidth("50%");

            H3 contactTitle = new H3("Contact & Demographic Attributes");
            contactTitle.addClassName("crm-panel-header");

            VerticalLayout contactList = new VerticalLayout();
            contactList.setPadding(false);
            contactList.setSpacing(true);

            contactList.add(createFieldRow("Email Address", customer.getEmail()));
            contactList.add(createFieldRow("Phone Number", customer.getPhone() != null ? customer.getPhone() : "—"));
            contactList.add(createFieldRow("City", customer.getCity() != null ? customer.getCity() : "—"));
            contactList.add(createFieldRow("Country", customer.getCountry() != null ? customer.getCountry() : "—"));

            contactPanel.add(contactTitle, contactList);

            // Tags & Behavioral Info
            Div tagsPanel = new Div();
            tagsPanel.addClassName("crm-panel");
            tagsPanel.setWidth("50%");

            H3 tagsTitle = new H3("Segmentation Tags & System Audit");
            tagsTitle.addClassName("crm-panel-header");

            VerticalLayout tagsList = new VerticalLayout();
            tagsList.setPadding(false);
            tagsList.setSpacing(true);

            HorizontalLayout tagBadges = new HorizontalLayout();
            tagBadges.setSpacing(true);
            if (customer.getTags() != null && !customer.getTags().isEmpty()) {
                customer.getTags().forEach(t -> {
                    Span tag = new Span(t);
                    tag.addClassName("crm-brand-badge");
                    tagBadges.add(tag);
                });
            } else {
                tagBadges.add(new Span("No tags assigned"));
            }

            tagsList.add(createFieldRow("Assigned Tags", tagBadges));
            tagsList.add(createFieldRow("Last Profile Update",
                    customer.getUpdatedAt() != null ? FormattingUtils.formatDateTime(customer.getUpdatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime()) : "—"));

            tagsPanel.add(tagsTitle, tagsList);

            panelsRow.add(contactPanel, tagsPanel);
            content.add(panelsRow);

            stateContainer.setContent(content);

        } catch (ApiException apiEx) {
            stateContainer.showError(apiEx.getUserFriendlyMessage(), this::loadCustomerProfile);
        } catch (Exception ex) {
            stateContainer.showError("Failed to load customer profile: " + ex.getMessage(), this::loadCustomerProfile);
        }
    }

    private HorizontalLayout createFieldRow(String label, Object value) {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        Span labelSpan = new Span(label);
        labelSpan.getStyle().set("font-size", "12px").set("color", "var(--crm-text-secondary)");

        Span valSpan;
        if (value instanceof com.vaadin.flow.component.Component comp) {
            row.add(labelSpan, comp);
            return row;
        } else {
            valSpan = new Span(value != null ? value.toString() : "—");
            valSpan.getStyle().set("font-size", "13px").set("font-weight", "500").set("color", "var(--crm-text-primary)");
            row.add(labelSpan, valSpan);
            return row;
        }
    }
}
