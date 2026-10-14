package com.crm.platform.frontend.views.customers;

import com.crm.platform.customer.dto.CustomerResponseDto;
import com.crm.platform.frontend.client.ApiException;
import com.crm.platform.frontend.client.PageResult;
import com.crm.platform.frontend.components.common.ConfirmDialog;
import com.crm.platform.frontend.layout.MainLayout;
import com.crm.platform.frontend.security.FrontendSecurityContext;
import com.crm.platform.frontend.service.CustomerApiService;
import com.crm.platform.frontend.util.FormattingUtils;
import com.crm.platform.frontend.util.NotificationUtils;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * SCR-03: Paginated Customer Directory with multi-attribute filtering and admin actions.
 */
@Route(value = "customers", layout = MainLayout.class)
@PageTitle("Customer Directory | Enterprise AI-CRM")
public class CustomerListView extends VerticalLayout {

    private final CustomerApiService customerApiService;
    private final Grid<CustomerResponseDto> grid;

    private final TextField nameFilter = new TextField();
    private final TextField emailFilter = new TextField();
    private final TextField cityFilter = new TextField();
    private final TextField tagFilter = new TextField();

    public CustomerListView(CustomerApiService customerApiService) {
        this.customerApiService = customerApiService;

        addClassName("crm-content-container");
        setWidthFull();
        setPadding(true);
        setSpacing(true);

        createHeader();
        createFilterBar();

        grid = new Grid<>();
        grid.setWidthFull();
        grid.setHeight("600px");
        setupGrid();

        add(grid);
    }

    private void createHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        Div left = new Div();
        H2 title = new H2("Customer Directory");
        title.addClassName("crm-view-title");
        Paragraph sub = new Paragraph("Master directory of customer profiles, engagement metrics, and spending history.");
        sub.addClassName("crm-view-subtitle");
        left.add(title, sub);

        Button addCustomerBtn = new Button("New Customer", VaadinIcon.PLUS.create(), e -> {
            CustomerFormDialog dialog = new CustomerFormDialog(customerApiService, null, saved -> grid.getDataProvider().refreshAll());
            dialog.open();
        });
        addCustomerBtn.addClassName("crm-btn-primary");

        header.add(left, addCustomerBtn);
        add(header);
    }

    private void createFilterBar() {
        HorizontalLayout filterBar = new HorizontalLayout();
        filterBar.addClassName("crm-filter-bar");
        filterBar.setWidthFull();
        filterBar.setAlignItems(FlexComponent.Alignment.END);

        nameFilter.setPlaceholder("Filter by name...");
        nameFilter.setClearButtonVisible(true);
        nameFilter.setWidth("180px");

        emailFilter.setPlaceholder("Filter by email...");
        emailFilter.setClearButtonVisible(true);
        emailFilter.setWidth("200px");

        cityFilter.setPlaceholder("Filter by city...");
        cityFilter.setClearButtonVisible(true);
        cityFilter.setWidth("150px");

        tagFilter.setPlaceholder("Filter by tag...");
        tagFilter.setClearButtonVisible(true);
        tagFilter.setWidth("150px");

        Button applyBtn = new Button("Apply", e -> grid.getDataProvider().refreshAll());
        applyBtn.addClassName("crm-btn-primary");

        Button resetBtn = new Button("Reset", e -> {
            nameFilter.clear();
            emailFilter.clear();
            cityFilter.clear();
            tagFilter.clear();
            grid.getDataProvider().refreshAll();
        });
        resetBtn.addClassName("crm-btn-secondary");

        filterBar.add(nameFilter, emailFilter, cityFilter, tagFilter, applyBtn, resetBtn);
        add(filterBar);
    }

    private void setupGrid() {
        CallbackDataProvider<CustomerResponseDto, Void> dataProvider = DataProvider.fromCallbacks(
                query -> {
                    int offset = query.getOffset();
                    int limit = query.getLimit();
                    int page = offset / limit;

                    String name = nameFilter.getValue();
                    String email = emailFilter.getValue();
                    String city = cityFilter.getValue();
                    String tag = tagFilter.getValue();

                    PageResult<CustomerResponseDto> result = customerApiService.searchCustomers(
                            name != null && !name.isBlank() ? name.trim() : null,
                            null,
                            email != null && !email.isBlank() ? email.trim() : null,
                            city != null && !city.isBlank() ? city.trim() : null,
                            null,
                            tag != null && !tag.isBlank() ? tag.trim() : null,
                            page,
                            limit,
                            "id,desc"
                    );
                    return result.getContent().stream();
                },
                query -> {
                    String name = nameFilter.getValue();
                    String email = emailFilter.getValue();
                    String city = cityFilter.getValue();
                    String tag = tagFilter.getValue();

                    PageResult<CustomerResponseDto> result = customerApiService.searchCustomers(
                            name != null && !name.isBlank() ? name.trim() : null,
                            null,
                            email != null && !email.isBlank() ? email.trim() : null,
                            city != null && !city.isBlank() ? city.trim() : null,
                            null,
                            tag != null && !tag.isBlank() ? tag.trim() : null,
                            0,
                            1,
                            null
                    );
                    return (int) result.getTotalElements();
                }
        );

        grid.setDataProvider(dataProvider);

        grid.addColumn(c -> "#" + c.getId())
                .setHeader("ID")
                .setWidth("80px")
                .setFlexGrow(0)
                .setClassNameGenerator(c -> "crm-mono-cell");

        grid.addComponentColumn(c -> {
            Button nameLink = new Button(c.getFirstName() + " " + c.getLastName(), e -> {
                getUI().ifPresent(ui -> ui.navigate("customers/" + c.getId()));
            });
            nameLink.getStyle()
                    .set("background", "none")
                    .set("border", "none")
                    .set("padding", "0")
                    .set("font-weight", "600")
                    .set("color", "var(--crm-text-primary)")
                    .set("cursor", "pointer");
            return nameLink;
        }).setHeader("Full Name").setFlexGrow(2);

        grid.addColumn(CustomerResponseDto::getEmail)
                .setHeader("Email")
                .setFlexGrow(2)
                .setClassNameGenerator(c -> "crm-mono-cell");

        grid.addColumn(c -> c.getCity() != null ? c.getCity() : "—")
                .setHeader("City")
                .setWidth("130px")
                .setFlexGrow(0);

        grid.addColumn(c -> FormattingUtils.formatCurrency(c.getTotalSpend()))
                .setHeader("Spend")
                .setWidth("120px")
                .setFlexGrow(0)
                .setClassNameGenerator(c -> "crm-numeric-cell");

        grid.addColumn(c -> FormattingUtils.formatInteger(c.getVisitCount()))
                .setHeader("Visits")
                .setWidth("80px")
                .setFlexGrow(0)
                .setClassNameGenerator(c -> "crm-numeric-cell");

        grid.addColumn(c -> FormattingUtils.formatDate(c.getLastActiveDate()))
                .setHeader("Last Active")
                .setWidth("120px")
                .setFlexGrow(0);

        grid.addComponentColumn(c -> {
            if (c.getTags() == null || c.getTags().isEmpty()) {
                return new Span("—");
            }
            HorizontalLayout tagsLayout = new HorizontalLayout();
            tagsLayout.setSpacing(true);
            c.getTags().forEach(t -> {
                Span tag = new Span(t);
                tag.addClassName("crm-brand-badge");
                tagsLayout.add(tag);
            });
            return tagsLayout;
        }).setHeader("Tags").setFlexGrow(1);

        grid.addComponentColumn(c -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(true);

            Button editBtn = new Button(VaadinIcon.EDIT.create(), e -> {
                CustomerFormDialog dialog = new CustomerFormDialog(customerApiService, c, saved -> grid.getDataProvider().refreshAll());
                dialog.open();
            });
            editBtn.addClassName("crm-btn-secondary");
            editBtn.getStyle().set("padding", "2px 8px");
            actions.add(editBtn);

            if (FrontendSecurityContext.isAdmin()) {
                Button deleteBtn = new Button(VaadinIcon.TRASH.create(), e -> {
                    ConfirmDialog confirm = new ConfirmDialog(
                            "Delete Customer Profile",
                            "Are you sure you want to delete customer #" + c.getId() + " (" + c.getFirstName() + " " + c.getLastName() + ")? This will soft-delete the record.",
                            "Delete",
                            true,
                            () -> {
                                try {
                                    customerApiService.deleteCustomer(c.getId());
                                    NotificationUtils.showSuccess("Customer record deleted.");
                                    grid.getDataProvider().refreshAll();
                                } catch (ApiException apiEx) {
                                    NotificationUtils.showError(apiEx.getUserFriendlyMessage());
                                }
                            }
                    );
                    confirm.open();
                });
                deleteBtn.addClassName("crm-btn-danger");
                deleteBtn.getStyle().set("padding", "2px 8px");
                actions.add(deleteBtn);
            }

            return actions;
        }).setHeader("Actions").setWidth("120px").setFlexGrow(0);
    }
}
