package com.crm.platform.campaign;

import com.crm.platform.campaign.dto.CampaignCreateRequest;
import com.crm.platform.campaign.dto.CampaignUpdateRequest;
import com.crm.platform.campaign.entity.Campaign;
import com.crm.platform.campaign.entity.CampaignStatus;
import com.crm.platform.campaign.repository.CampaignRepository;
import com.crm.platform.segment.entity.Segment;
import com.crm.platform.segment.repository.SegmentRepository;
import com.crm.platform.user.entity.RoleEnum;
import com.crm.platform.user.entity.User;
import com.crm.platform.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class CampaignControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private SegmentRepository segmentRepository;

    @Autowired
    private UserRepository userRepository;

    private User adminUser;
    private User marketerUser;
    private Segment segment;
    private Campaign draftCampaign;

    @BeforeEach
    void setUp() {
        campaignRepository.deleteAll();
        segmentRepository.deleteAll();
        userRepository.deleteAll();

        adminUser = new User("admin_camp", "admin_camp@crm.internal", "hash", RoleEnum.ROLE_ADMIN, Boolean.TRUE);
        adminUser = userRepository.saveAndFlush(adminUser);

        marketerUser = new User("marketer_camp", "marketer_camp@crm.internal", "hash", RoleEnum.ROLE_MARKETER, Boolean.TRUE);
        marketerUser = userRepository.saveAndFlush(marketerUser);

        segment = new Segment("Festival Shoppers", "Diwali Segment", "{\"combinator\":\"AND\"}", adminUser);
        segment = segmentRepository.saveAndFlush(segment);

        draftCampaign = new Campaign(
                "Diwali Mega Sale",
                "Festive promotion",
                segment,
                "Hi {{firstName}}, check out our Diwali discounts!",
                Boolean.TRUE,
                adminUser
        );
        draftCampaign = campaignRepository.saveAndFlush(draftCampaign);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        campaignRepository.deleteAll();
        segmentRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "admin_camp", roles = {"ADMIN"})
    @DisplayName("POST /api/v1/campaigns as ADMIN creates campaign in DRAFT and returns 201 Created")
    void createCampaign_AsAdmin_Returns201() throws Exception {
        CampaignCreateRequest request = new CampaignCreateRequest(
                "New Year Blast",
                "New year sale",
                segment.getId(),
                "Happy New Year {{firstName}}!",
                Boolean.FALSE
        );

        mockMvc.perform(post("/api/v1/campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.name", is("New Year Blast")))
                .andExpect(jsonPath("$.data.status", is("DRAFT")))
                .andExpect(jsonPath("$.data.segmentId", is(segment.getId().intValue())))
                .andExpect(jsonPath("$.data.createdBy", is(adminUser.getId().intValue())))
                .andExpect(jsonPath("$.data.createdByName", is("admin_camp")));
    }

    @Test
    @WithMockUser(username = "marketer_camp", roles = {"MARKETER"})
    @DisplayName("POST /api/v1/campaigns as MARKETER creates campaign and returns 201 Created")
    void createCampaign_AsMarketer_Returns201() throws Exception {
        CampaignCreateRequest request = new CampaignCreateRequest(
                "Monsoon Treats",
                "Rainy day promotion",
                segment.getId(),
                "Enjoy hot coffee with 15% discount!",
                Boolean.TRUE
        );

        mockMvc.perform(post("/api/v1/campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", is("Monsoon Treats")))
                .andExpect(jsonPath("$.data.status", is("DRAFT")))
                .andExpect(jsonPath("$.data.createdBy", is(marketerUser.getId().intValue())))
                .andExpect(jsonPath("$.data.createdByName", is("marketer_camp")));
    }

    @Test
    @WithMockUser(username = "admin_camp", roles = {"ADMIN"})
    @DisplayName("POST /api/v1/campaigns ignores client-supplied server fields (id, status, createdBy, timestamps)")
    void createCampaign_WithForbiddenFields_IgnoredAndServerControlled() throws Exception {
        String payloadWithForbiddenFields = "{" +
                "\"name\":\"Mass Assignment Test\"," +
                "\"description\":\"Testing forbidden fields\"," +
                "\"segmentId\":" + segment.getId() + "," +
                "\"messageTemplate\":\"Safe Template\"," +
                "\"id\":999999," +
                "\"status\":\"COMPLETED\"," +
                "\"createdBy\":8888," +
                "\"createdAt\":\"2020-01-01T00:00:00Z\"," +
                "\"updatedAt\":\"2020-01-01T00:00:00Z\"," +
                "\"startedAt\":\"2020-01-01T00:00:00Z\"," +
                "\"completedAt\":\"2020-01-01T00:00:00Z\"," +
                "\"aiSummary\":\"Hacked summary\"" +
                "}";

        mockMvc.perform(post("/api/v1/campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadWithForbiddenFields))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("DRAFT")))
                .andExpect(jsonPath("$.data.aiSummary").doesNotExist())
                .andExpect(jsonPath("$.data.createdBy", is(adminUser.getId().intValue())))
                .andExpect(jsonPath("$.data.createdByName", is("admin_camp")));

        Campaign saved = campaignRepository.findAll().stream()
                .filter(c -> "Mass Assignment Test".equals(c.getName()))
                .findFirst()
                .orElseThrow();
        org.assertj.core.api.Assertions.assertThat(saved.getCreatedBy().getId()).isEqualTo(adminUser.getId());
    }

    @Test
    @WithMockUser(username = "marketer_camp", roles = {"MARKETER"})
    @DisplayName("POST /api/v1/campaigns authoritatively attributes createdBy to principal and rejects client JSON override")
    void createCampaign_AuthenticatedUserAttribution_CannotBeOverriddenByJson() throws Exception {
        String payload = "{" +
                "\"name\":\"Attribution Campaign\"," +
                "\"description\":\"Attribution check\"," +
                "\"segmentId\":" + segment.getId() + "," +
                "\"messageTemplate\":\"Hello {{firstName}}\"," +
                "\"personalizationEnabled\":true," +
                "\"createdBy\":999999," +
                "\"createdByName\":\"hacker\"" +
                "}";

        mockMvc.perform(post("/api/v1/campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", is("Attribution Campaign")))
                .andExpect(jsonPath("$.data.createdBy", is(marketerUser.getId().intValue())))
                .andExpect(jsonPath("$.data.createdByName", is("marketer_camp")));

        Campaign saved = campaignRepository.findAll().stream()
                .filter(c -> "Attribution Campaign".equals(c.getName()))
                .findFirst()
                .orElseThrow();
        org.assertj.core.api.Assertions.assertThat(saved.getCreatedBy().getId()).isEqualTo(marketerUser.getId());
    }

    @Test
    @DisplayName("POST /api/v1/campaigns unauthenticated returns 401 Unauthorized")
    void createCampaign_Unauthenticated_Returns401() throws Exception {
        CampaignCreateRequest request = new CampaignCreateRequest(
                "Unauth Campaign", "Desc", segment.getId(), "Hello", false
        );

        mockMvc.perform(post("/api/v1/campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code", is("UNAUTHORIZED")));
    }

    @Test
    @WithMockUser(username = "admin_camp", roles = {"ADMIN"})
    @DisplayName("POST /api/v1/campaigns with non-existent segmentId returns 404 Not Found")
    void createCampaign_SegmentNotFound_Returns404() throws Exception {
        CampaignCreateRequest request = new CampaignCreateRequest(
                "Missing Segment Campaign", "Desc", 99999L, "Hello", false
        );

        mockMvc.perform(post("/api/v1/campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code", is("RESOURCE_NOT_FOUND")))
                .andExpect(jsonPath("$.error.message", containsString("Segment not found with id: 99999")));
    }

    @Test
    @WithMockUser(username = "admin_camp", roles = {"ADMIN"})
    @DisplayName("POST /api/v1/campaigns with blank name returns 400 Bad Request")
    void createCampaign_BlankName_Returns400() throws Exception {
        CampaignCreateRequest request = new CampaignCreateRequest(
                "", "Desc", segment.getId(), "Hello", false
        );

        mockMvc.perform(post("/api/v1/campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code", is("VALIDATION_FAILED")));
    }

    @Test
    @WithMockUser(username = "marketer_camp", roles = {"MARKETER"})
    @DisplayName("GET /api/v1/campaigns/{id} returns 200 OK with campaign details")
    void getCampaignById_Found_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/campaigns/{id}", draftCampaign.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(draftCampaign.getId().intValue())))
                .andExpect(jsonPath("$.data.name", is("Diwali Mega Sale")))
                .andExpect(jsonPath("$.data.status", is("DRAFT")));
    }

    @Test
    @WithMockUser(username = "marketer_camp", roles = {"MARKETER"})
    @DisplayName("GET /api/v1/campaigns/{id} missing returns 404 Not Found")
    void getCampaignById_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/v1/campaigns/{id}", 88888L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code", is("RESOURCE_NOT_FOUND")));
    }

    @Test
    @WithMockUser(username = "marketer_camp", roles = {"MARKETER"})
    @DisplayName("PATCH /api/v1/campaigns/{id} on DRAFT campaign updates properties and returns 200 OK")
    void updateCampaign_OnDraft_Returns200() throws Exception {
        CampaignUpdateRequest request = new CampaignUpdateRequest(
                "Updated Diwali Mega Sale", "New Desc", null, null, null
        );

        mockMvc.perform(patch("/api/v1/campaigns/{id}", draftCampaign.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", is("Updated Diwali Mega Sale")))
                .andExpect(jsonPath("$.data.description", is("New Desc")));
    }

    @Test
    @WithMockUser(username = "marketer_camp", roles = {"MARKETER"})
    @DisplayName("PATCH /api/v1/campaigns/{id} ignores client-supplied server fields (status, id, timestamps, aiSummary)")
    void updateCampaign_WithForbiddenFields_IgnoredAndServerControlled() throws Exception {
        String payloadWithForbiddenFields = "{" +
                "\"name\":\"Legitimate Name Update\"," +
                "\"status\":\"COMPLETED\"," +
                "\"id\":777777," +
                "\"aiSummary\":\"Tampered summary\"," +
                "\"startedAt\":\"2020-01-01T00:00:00Z\"," +
                "\"completedAt\":\"2020-01-01T00:00:00Z\"" +
                "}";

        mockMvc.perform(patch("/api/v1/campaigns/{id}", draftCampaign.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadWithForbiddenFields))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", is("Legitimate Name Update")))
                .andExpect(jsonPath("$.data.status", is("DRAFT")))
                .andExpect(jsonPath("$.data.aiSummary").doesNotExist());
    }

    @Test
    @WithMockUser(username = "marketer_camp", roles = {"MARKETER"})
    @DisplayName("PATCH /api/v1/campaigns/{id} on non-DRAFT campaign returns 409 Conflict")
    void updateCampaign_OnNonDraft_Returns409() throws Exception {
        draftCampaign.setStatus(CampaignStatus.RUNNING);
        campaignRepository.saveAndFlush(draftCampaign);

        CampaignUpdateRequest request = new CampaignUpdateRequest(
                "Attempted Update", null, null, null, null
        );

        mockMvc.perform(patch("/api/v1/campaigns/{id}", draftCampaign.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code", is("CONFLICT")))
                .andExpect(jsonPath("$.error.message", containsString("permitted only when status is DRAFT")));
    }

    @Test
    @WithMockUser(username = "marketer_camp", roles = {"MARKETER"})
    @DisplayName("DELETE /api/v1/campaigns/{id} with ROLE_MARKETER returns 403 Forbidden (ADMIN only)")
    void deleteCampaign_AsMarketer_Returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/campaigns/{id}", draftCampaign.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code", is("FORBIDDEN")));
    }

    @Test
    @WithMockUser(username = "admin_camp", roles = {"ADMIN"})
    @DisplayName("DELETE /api/v1/campaigns/{id} with ROLE_ADMIN on non-DRAFT returns 409 Conflict")
    void deleteCampaign_OnNonDraft_Returns409() throws Exception {
        draftCampaign.setStatus(CampaignStatus.RUNNING);
        campaignRepository.saveAndFlush(draftCampaign);

        mockMvc.perform(delete("/api/v1/campaigns/{id}", draftCampaign.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code", is("CONFLICT")))
                .andExpect(jsonPath("$.error.message", containsString("permitted only when status is DRAFT")));
    }

    @Test
    @WithMockUser(username = "admin_camp", roles = {"ADMIN"})
    @DisplayName("DELETE /api/v1/campaigns/{id} with ROLE_ADMIN on DRAFT returns 204 No Content")
    void deleteCampaign_OnDraft_Returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/campaigns/{id}", draftCampaign.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/campaigns/{id}", draftCampaign.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "marketer_camp", roles = {"MARKETER"})
    @DisplayName("GET /api/v1/campaigns returns paginated directory of campaigns")
    void listCampaigns_Returns200WithPagination() throws Exception {
        mockMvc.perform(get("/api/v1/campaigns?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.metadata.pagination.totalElements", is(1)));
    }

    @Test
    @WithMockUser(username = "marketer_camp", roles = {"MARKETER"})
    @DisplayName("GET /api/v1/campaigns with status filter returns matching campaigns")
    void listCampaigns_FilterByStatus_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/campaigns?status=DRAFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)));

        mockMvc.perform(get("/api/v1/campaigns?status=COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }
}
