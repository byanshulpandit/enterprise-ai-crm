package com.crm.platform.campaign.controller;

import com.crm.platform.campaign.dto.CampaignCreateRequest;
import com.crm.platform.campaign.dto.CampaignResponse;
import com.crm.platform.campaign.dto.CampaignUpdateRequest;
import com.crm.platform.campaign.entity.Campaign;
import com.crm.platform.campaign.entity.CampaignStatus;
import com.crm.platform.campaign.service.CampaignService;
import com.crm.platform.common.dto.ApiResponse;
import com.crm.platform.common.dto.PageMetadata;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/campaigns")
public class CampaignController {

    private final CampaignService campaignService;

    public CampaignController(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CampaignResponse>> createCampaign(
            @Valid @RequestBody CampaignCreateRequest request,
            Authentication authentication) {
        String callerUsername = authentication != null ? authentication.getName() : null;
        Campaign created = campaignService.createCampaign(request, callerUsername);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(ApiResponse.success(CampaignResponse.fromEntity(created)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CampaignResponse>> getCampaignById(@PathVariable Long id) {
        Campaign campaign = campaignService.getCampaignById(id);
        return ResponseEntity.ok(ApiResponse.success(CampaignResponse.fromEntity(campaign)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<CampaignResponse>> updateCampaign(
            @PathVariable Long id,
            @Valid @RequestBody CampaignUpdateRequest request) {
        Campaign updated = campaignService.updateCampaign(id, request);
        return ResponseEntity.ok(ApiResponse.success(CampaignResponse.fromEntity(updated)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteCampaign(@PathVariable Long id) {
        campaignService.deleteCampaign(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CampaignResponse>>> listCampaigns(
            @RequestParam(required = false) CampaignStatus status,
            @PageableDefault(page = 0, size = 10, sort = "id") Pageable pageable) {
        Page<Campaign> page = campaignService.listCampaigns(status, pageable);
        List<CampaignResponse> dtos = page.getContent().stream().map(CampaignResponse::fromEntity).toList();
        PageMetadata pagination = PageMetadata.fromPage(page);
        return ResponseEntity.ok(ApiResponse.success(dtos, pagination));
    }

    @PostMapping("/{id}/launch")
    public ResponseEntity<ApiResponse<com.crm.platform.campaign.dto.CampaignLaunchResponse>> launchCampaign(@PathVariable Long id) {
        com.crm.platform.campaign.dto.CampaignLaunchResponse response = campaignService.launchCampaign(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
