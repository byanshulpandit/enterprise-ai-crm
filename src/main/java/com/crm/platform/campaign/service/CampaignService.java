package com.crm.platform.campaign.service;

import com.crm.platform.campaign.dto.CampaignCreateRequest;
import com.crm.platform.campaign.dto.CampaignUpdateRequest;
import com.crm.platform.campaign.entity.Campaign;
import com.crm.platform.campaign.entity.CampaignStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CampaignService {

    Campaign createCampaign(CampaignCreateRequest request, String callerUsername);

    Campaign getCampaignById(Long id);

    Campaign updateCampaign(Long id, CampaignUpdateRequest request);

    void deleteCampaign(Long id);

    Page<Campaign> listCampaigns(CampaignStatus status, Pageable pageable);
}
