package com.crm.platform.ai.controller;

import com.crm.platform.ai.dto.AiRuleGenerationRequest;
import com.crm.platform.ai.dto.AiRuleGenerationResponse;
import com.crm.platform.ai.dto.AiSegmentAuditDto;
import com.crm.platform.ai.service.AiService;
import com.crm.platform.common.dto.ApiResponse;
import com.crm.platform.common.dto.PageMetadata;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/segments/generate-rules")
    public ResponseEntity<ApiResponse<AiRuleGenerationResponse>> generateRules(
            @Valid @RequestBody AiRuleGenerationRequest request,
            Authentication authentication) {
        String callerUsername = authentication != null ? authentication.getName() : null;
        AiRuleGenerationResponse response = aiService.generateSegmentRules(request, callerUsername);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/segments/audits")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<AiSegmentAuditDto>>> getAudits(
            @PageableDefault(page = 0, size = 10, sort = "id") Pageable pageable) {
        Page<AiSegmentAuditDto> page = aiService.getAudits(pageable);
        List<AiSegmentAuditDto> content = page.getContent();
        PageMetadata pagination = PageMetadata.fromPage(page);
        return ResponseEntity.ok(ApiResponse.success(content, pagination));
    }
}
