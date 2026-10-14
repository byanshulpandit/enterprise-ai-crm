package com.crm.platform.frontend.views.admin;

import com.crm.platform.ai.dto.AiSegmentAuditDto;
import com.crm.platform.frontend.client.PageResult;
import com.crm.platform.frontend.layout.MainLayout;
import com.crm.platform.frontend.service.AiApiService;
import com.crm.platform.frontend.util.FormattingUtils;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.time.ZoneId;

/**
 * SCR-15: AI Audit Log Inspector (ADMIN only) tracking prompts and synthesized rule trees.
 */
@Route(value = "admin/ai-audits", layout = MainLayout.class)
@RolesAllowed("ADMIN")
@PageTitle("AI Audit Logs | Enterprise AI-CRM")
public class AiAuditsView extends VerticalLayout {

    private final AiApiService aiApiService;
    private final Grid<AiSegmentAuditDto> grid;

    public AiAuditsView(AiApiService aiApiService) {
        this.aiApiService = aiApiService;

        addClassName("crm-content-container");
        setWidthFull();
        setPadding(true);
        setSpacing(true);

        createHeader();

        grid = new Grid<>();
        grid.setWidthFull();
        grid.setHeight("600px");
        setupGrid();

        add(grid);
    }

    private void createHeader() {
        Div left = new Div();
        H2 title = new H2("AI Generation Audits");
        title.addClassName("crm-view-title");
        Paragraph sub = new Paragraph("System-level audit records for all AI-assisted segment prompt interactions and synthesized criteria.");
        sub.addClassName("crm-view-subtitle");
        left.add(title, sub);

        add(left);
    }

    private void setupGrid() {
        CallbackDataProvider<AiSegmentAuditDto, Void> dataProvider = DataProvider.fromCallbacks(
                query -> {
                    int offset = query.getOffset();
                    int limit = query.getLimit();
                    int page = offset / limit;

                    PageResult<AiSegmentAuditDto> result = aiApiService.getAiAudits(page, limit);
                    return result.getContent().stream();
                },
                query -> {
                    PageResult<AiSegmentAuditDto> result = aiApiService.getAiAudits(0, 1);
                    return (int) result.getTotalElements();
                }
        );

        grid.setDataProvider(dataProvider);

        grid.addColumn(a -> "#" + a.getId())
                .setHeader("ID")
                .setWidth("80px")
                .setFlexGrow(0)
                .setClassNameGenerator(a -> "crm-mono-cell");

        grid.addColumn(a -> a.getUsername() != null ? a.getUsername() : "System")
                .setHeader("User")
                .setWidth("130px")
                .setFlexGrow(0);

        grid.addColumn(AiSegmentAuditDto::getPromptText)
                .setHeader("Natural Language Prompt")
                .setFlexGrow(3);

        grid.addColumn(a -> a.getGeneratedRules() != null ? a.getGeneratedRules().toString() : "—")
                .setHeader("Synthesized AST")
                .setFlexGrow(3)
                .setClassNameGenerator(a -> "crm-mono-cell");

        grid.addColumn(a -> a.getActionTaken() != null ? a.getActionTaken() : "SAVED")
                .setHeader("Action")
                .setWidth("120px")
                .setFlexGrow(0);

        grid.addColumn(a -> a.getCreatedAt() != null ?
                FormattingUtils.formatDateTime(a.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime()) : "—")
                .setHeader("Timestamp")
                .setWidth("150px")
                .setFlexGrow(0);
    }
}
