package com.crm.platform.frontend.views.uploads;

import com.crm.platform.frontend.client.ApiException;
import com.crm.platform.frontend.client.PageResult;
import com.crm.platform.frontend.components.common.KpiStatCard;
import com.crm.platform.frontend.components.common.StatusBadge;
import com.crm.platform.frontend.layout.MainLayout;
import com.crm.platform.frontend.service.UploadApiService;
import com.crm.platform.frontend.util.FormattingUtils;
import com.crm.platform.frontend.util.NotificationUtils;
import com.crm.platform.upload.dto.UploadErrorDetail;
import com.crm.platform.upload.dto.UploadHistoryDto;
import com.crm.platform.upload.dto.UploadResultResponse;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.io.InputStream;
import java.time.ZoneId;

/**
 * SCR-11: Bulk Customer Ingestion Hub with drag-and-drop file upload, row error inspector, and history tracking.
 */
@Route(value = "uploads", layout = MainLayout.class)
@PageTitle("Bulk Ingestion | Enterprise AI-CRM")
public class BulkUploadView extends VerticalLayout {

    private final UploadApiService uploadApiService;
    private final Grid<UploadHistoryDto> historyGrid = new Grid<>();
    private final Div resultPanel = new Div();
    private final Grid<UploadErrorDetail> errorGrid = new Grid<>();

    public BulkUploadView(UploadApiService uploadApiService) {
        this.uploadApiService = uploadApiService;

        addClassName("crm-content-container");
        setWidthFull();
        setPadding(true);
        setSpacing(true);

        createHeader();
        createUploadSection();
        createResultSection();
        createHistorySection();
    }

    private void createHeader() {
        Div left = new Div();
        H2 title = new H2("Customer Bulk Ingestion Hub");
        title.addClassName("crm-view-title");
        Paragraph sub = new Paragraph("Ingest thousands of customer profiles via streaming CSV or XLSX batch processing.");
        sub.addClassName("crm-view-subtitle");
        left.add(title, sub);

        add(left);
    }

    private void createUploadSection() {
        Div uploadCard = new Div();
        uploadCard.addClassName("crm-panel");
        uploadCard.setWidthFull();

        H3 cardTitle = new H3("Upload Customer Dataset (CSV / XLSX)");
        cardTitle.addClassName("crm-panel-header");

        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes(".csv", ".xlsx", ".xls", "text/csv", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        upload.setMaxFileSize(10 * 1024 * 1024); // 10 MB limit
        upload.setWidthFull();

        upload.addSucceededListener(event -> {
            String fileName = event.getFileName();
            try (InputStream is = buffer.getInputStream()) {
                byte[] bytes = is.readAllBytes();
                if (bytes == null || bytes.length == 0) {
                    NotificationUtils.showError("Cannot ingest an empty file: " + fileName);
                    return;
                }
                UploadResultResponse response = uploadApiService.bulkUpload(fileName, bytes);
                NotificationUtils.showSuccess("File processed: " + fileName);
                displayUploadResult(response);
                historyGrid.getDataProvider().refreshAll();
            } catch (ApiException apiEx) {
                NotificationUtils.showError(apiEx.getUserFriendlyMessage());
            } catch (Exception ex) {
                NotificationUtils.showError("Failed to ingest file: " + ex.getMessage());
            }
        });

        upload.addFileRejectedListener(event -> {
            NotificationUtils.showError("File rejected: " + event.getErrorMessage());
        });

        uploadCard.add(cardTitle, upload);
        add(uploadCard);
    }

    private void createResultSection() {
        resultPanel.addClassName("crm-panel");
        resultPanel.setWidthFull();
        resultPanel.setVisible(false);

        errorGrid.setWidthFull();
        errorGrid.setHeight("250px");
        errorGrid.addColumn(UploadErrorDetail::getRow).setHeader("Row #").setWidth("80px").setFlexGrow(0);
        errorGrid.addColumn(UploadErrorDetail::getEmail).setHeader("Email").setWidth("220px").setFlexGrow(0).setClassNameGenerator(e -> "crm-mono-cell");
        errorGrid.addColumn(UploadErrorDetail::getReason).setHeader("Rejection Reason").setFlexGrow(3);

        add(resultPanel);
    }

    private void displayUploadResult(UploadResultResponse result) {
        resultPanel.removeAll();
        if (result == null) {
            resultPanel.setVisible(false);
            return;
        }

        resultPanel.setVisible(true);

        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        H3 title = new H3("Batch Ingestion Outcome: " + result.getFileName());
        title.addClassName("crm-panel-header");
        title.getStyle().set("margin-bottom", "0");

        StatusBadge badge = new StatusBadge(result.getStatus() != null ? result.getStatus().name() : "SUCCESS");
        header.add(title, badge);

        Div kpiGrid = new Div();
        kpiGrid.addClassName("crm-kpi-grid");
        kpiGrid.getStyle().set("margin-top", "12px");

        kpiGrid.add(new KpiStatCard("Total Records", FormattingUtils.formatInteger(result.getTotalRecords()), "Rows processed"));
        kpiGrid.add(new KpiStatCard("Committed Successfully", FormattingUtils.formatInteger(result.getSuccessfulRecords()), "Persisted to customer directory"));
        kpiGrid.add(new KpiStatCard("Failed Records", FormattingUtils.formatInteger(result.getFailedRecords()), "Rejected during validation"));

        resultPanel.add(header, kpiGrid);

        if (result.getErrors() != null && !result.getErrors().isEmpty()) {
            Span errorLabel = new Span("Rejected Row Details (First " + result.getErrors().size() + " anomalies):");
            errorLabel.getStyle().set("font-size", "13px").set("font-weight", "600").set("color", "var(--crm-status-error-base)").set("display", "block").set("margin", "12px 0 6px 0");

            errorGrid.setItems(result.getErrors());
            resultPanel.add(errorLabel, errorGrid);
        }
    }

    private void createHistorySection() {
        Div historyPanel = new Div();
        historyPanel.addClassName("crm-panel");
        historyPanel.setWidthFull();

        H3 cardTitle = new H3("Historical Upload Batches");
        cardTitle.addClassName("crm-panel-header");

        historyGrid.setWidthFull();
        historyGrid.setHeight("400px");

        CallbackDataProvider<UploadHistoryDto, Void> dataProvider = DataProvider.fromCallbacks(
                query -> {
                    int offset = query.getOffset();
                    int limit = query.getLimit();
                    int page = offset / limit;
                    PageResult<UploadHistoryDto> result = uploadApiService.getUploadHistory(page, limit);
                    return result.getContent().stream();
                },
                query -> {
                    PageResult<UploadHistoryDto> result = uploadApiService.getUploadHistory(0, 1);
                    return (int) result.getTotalElements();
                }
        );
        historyGrid.setDataProvider(dataProvider);

        historyGrid.addColumn(h -> "#" + h.getId())
                .setHeader("ID")
                .setWidth("80px")
                .setFlexGrow(0)
                .setClassNameGenerator(h -> "crm-mono-cell");

        historyGrid.addColumn(UploadHistoryDto::getFileName)
                .setHeader("File Name")
                .setFlexGrow(2);

        historyGrid.addComponentColumn(h -> new StatusBadge(h.getStatus() != null ? h.getStatus().name() : "SUCCESS"))
                .setHeader("Status")
                .setWidth("140px")
                .setFlexGrow(0);

        historyGrid.addColumn(h -> FormattingUtils.formatInteger(h.getTotalRows()))
                .setHeader("Total")
                .setWidth("90px")
                .setFlexGrow(0)
                .setClassNameGenerator(h -> "crm-numeric-cell");

        historyGrid.addColumn(h -> FormattingUtils.formatInteger(h.getSuccessCount()))
                .setHeader("Success")
                .setWidth("90px")
                .setFlexGrow(0)
                .setClassNameGenerator(h -> "crm-numeric-cell");

        historyGrid.addColumn(h -> FormattingUtils.formatInteger(h.getFailureCount()))
                .setHeader("Failed")
                .setWidth("90px")
                .setFlexGrow(0)
                .setClassNameGenerator(h -> "crm-numeric-cell");

        historyGrid.addColumn(h -> h.getCreatedAt() != null ?
                FormattingUtils.formatDateTime(h.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime()) : "—")
                .setHeader("Timestamp")
                .setWidth("160px")
                .setFlexGrow(0);

        historyPanel.add(cardTitle, historyGrid);
        add(historyPanel);
    }
}
