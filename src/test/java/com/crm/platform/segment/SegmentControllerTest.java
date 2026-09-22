package com.crm.platform.segment;

import com.crm.platform.campaign.entity.Campaign;
import com.crm.platform.campaign.repository.CampaignRepository;
import com.crm.platform.segment.dto.SegmentCreateRequest;
import com.crm.platform.segment.dto.SegmentUpdateRequest;
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
public class SegmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SegmentRepository segmentRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private UserRepository userRepository;

    private User adminUser;
    private User marketerUser;
    private Segment existingSegment;

    @BeforeEach
    void setUp() {
        campaignRepository.deleteAll();
        segmentRepository.deleteAll();
        userRepository.deleteAll();

        adminUser = new User("admin_tester", "admin_tester@crm.internal", "hash", RoleEnum.ROLE_ADMIN, Boolean.TRUE);
        adminUser = userRepository.saveAndFlush(adminUser);

        marketerUser = new User("marketer_tester", "marketer_tester@crm.internal", "hash", RoleEnum.ROLE_MARKETER, Boolean.TRUE);
        marketerUser = userRepository.saveAndFlush(marketerUser);

        existingSegment = new Segment("Existing Segment", "Existing Description", "{\"combinator\":\"AND\"}", adminUser);
        existingSegment = segmentRepository.saveAndFlush(existingSegment);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        campaignRepository.deleteAll();
        segmentRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "admin_tester", roles = {"ADMIN"})
    @DisplayName("POST /api/v1/segments as ADMIN creates segment and returns 201 Created")
    void createSegment_AsAdmin_Returns201() throws Exception {
        SegmentCreateRequest request = new SegmentCreateRequest(
                "High Value Segment",
                "High spending customers",
                objectMapper.readTree("{\"field\":\"totalSpend\",\"op\":\"GREATER_THAN\",\"value\":5000}")
        );

        mockMvc.perform(post("/api/v1/segments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.name", is("High Value Segment")))
                .andExpect(jsonPath("$.data.description", is("High spending customers")))
                .andExpect(jsonPath("$.data.rules.field", is("totalSpend")))
                .andExpect(jsonPath("$.data.createdBy", is(adminUser.getId().intValue())))
                .andExpect(jsonPath("$.data.createdByName", is("admin_tester")));
    }

    @Test
    @WithMockUser(username = "admin_tester", roles = {"ADMIN"})
    @DisplayName("POST /api/v1/segments authoritatively attributes createdBy to principal and rejects client override")
    void createSegment_AuthenticatedUserAttribution_CannotBeOverriddenByJson() throws Exception {
        String payloadWithMaliciousCreatedBy = "{" +
                "\"name\":\"Malicious Override Segment\"," +
                "\"description\":\"Testing creator attribution\"," +
                "\"rules\":{\"field\":\"spend\",\"op\":\"GREATER_THAN\",\"value\":100}," +
                "\"createdBy\":999999," +
                "\"createdByName\":\"hacker\"" +
                "}";

        mockMvc.perform(post("/api/v1/segments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadWithMaliciousCreatedBy))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.createdBy", is(adminUser.getId().intValue())))
                .andExpect(jsonPath("$.data.createdByName", is("admin_tester")));

        Segment saved = segmentRepository.findByName("Malicious Override Segment").orElseThrow();
        org.assertj.core.api.Assertions.assertThat(saved.getCreatedBy().getId()).isEqualTo(adminUser.getId());
    }

    @Test
    @WithMockUser(username = "marketer_tester", roles = {"MARKETER"})
    @DisplayName("POST /api/v1/segments as MARKETER creates segment and returns 201 Created")
    void createSegment_AsMarketer_Returns201() throws Exception {
        SegmentCreateRequest request = new SegmentCreateRequest(
                "Marketer Segment",
                "Created by marketer",
                objectMapper.readTree("{\"field\":\"city\",\"op\":\"EQUALS\",\"value\":\"Delhi\"}")
        );

        mockMvc.perform(post("/api/v1/segments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", is("Marketer Segment")));
    }

    @Test
    @DisplayName("POST /api/v1/segments unauthenticated returns 401 Unauthorized")
    void createSegment_Unauthenticated_Returns401() throws Exception {
        SegmentCreateRequest request = new SegmentCreateRequest(
                "Public Attempt", null, objectMapper.readTree("{}")
        );

        mockMvc.perform(post("/api/v1/segments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code", is("UNAUTHORIZED")));
    }

    @Test
    @WithMockUser(username = "admin_tester", roles = {"ADMIN"})
    @DisplayName("POST /api/v1/segments with blank name returns 400 Bad Request")
    void createSegment_BlankName_Returns400() throws Exception {
        SegmentCreateRequest request = new SegmentCreateRequest("", "Desc", objectMapper.readTree("{}"));

        mockMvc.perform(post("/api/v1/segments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code", is("VALIDATION_FAILED")));
    }

    @Test
    @WithMockUser(username = "admin_tester", roles = {"ADMIN"})
    @DisplayName("POST /api/v1/segments with duplicate name returns 409 Conflict")
    void createSegment_DuplicateName_Returns409() throws Exception {
        SegmentCreateRequest request = new SegmentCreateRequest(
                existingSegment.getName(),
                "Duplicate description",
                objectMapper.readTree("{\"op\":\"AND\"}")
        );

        mockMvc.perform(post("/api/v1/segments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code", is("DUPLICATE_RESOURCE")))
                .andExpect(jsonPath("$.error.message", containsString("already exists")));
    }

    @Test
    @WithMockUser(username = "marketer_tester", roles = {"MARKETER"})
    @DisplayName("GET /api/v1/segments/{id} returns 200 OK with segment data")
    void getSegmentById_Found_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/segments/{id}", existingSegment.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(existingSegment.getId().intValue())))
                .andExpect(jsonPath("$.data.name", is("Existing Segment")));
    }

    @Test
    @WithMockUser(username = "marketer_tester", roles = {"MARKETER"})
    @DisplayName("GET /api/v1/segments/{id} missing returns 404 Not Found")
    void getSegmentById_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/v1/segments/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code", is("RESOURCE_NOT_FOUND")));
    }

    @Test
    @WithMockUser(username = "marketer_tester", roles = {"MARKETER"})
    @DisplayName("PATCH /api/v1/segments/{id} updates segment properties and returns 200 OK")
    void updateSegment_Success_Returns200() throws Exception {
        SegmentUpdateRequest request = new SegmentUpdateRequest("Updated Segment Title", null, null);

        mockMvc.perform(patch("/api/v1/segments/{id}", existingSegment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", is("Updated Segment Title")))
                .andExpect(jsonPath("$.data.description", is("Existing Description")));
    }

    @Test
    @WithMockUser(username = "marketer_tester", roles = {"MARKETER"})
    @DisplayName("PATCH /api/v1/segments/{id} with same existing name succeeds without triggering DuplicateResourceException")
    void updateSegment_SameExistingName_Succeeds() throws Exception {
        SegmentUpdateRequest request = new SegmentUpdateRequest(existingSegment.getName(), "Updated Description With Same Name", null);

        mockMvc.perform(patch("/api/v1/segments/{id}", existingSegment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", is(existingSegment.getName())))
                .andExpect(jsonPath("$.data.description", is("Updated Description With Same Name")));
    }

    @Test
    @WithMockUser(username = "marketer_tester", roles = {"MARKETER"})
    @DisplayName("PATCH /api/v1/segments/{id} with duplicate name returns 409 Conflict")
    void updateSegment_DuplicateName_Returns409() throws Exception {
        Segment secondSegment = new Segment("Second Segment", "Desc", "{}", adminUser);
        secondSegment = segmentRepository.saveAndFlush(secondSegment);

        SegmentUpdateRequest request = new SegmentUpdateRequest(existingSegment.getName(), null, null);

        mockMvc.perform(patch("/api/v1/segments/{id}", secondSegment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code", is("DUPLICATE_RESOURCE")))
                .andExpect(jsonPath("$.error.message", containsString("already exists")));
    }

    @Test
    @WithMockUser(username = "marketer_tester", roles = {"MARKETER"})
    @DisplayName("PATCH /api/v1/segments/{id} bound to a campaign succeeds per API-Design.md Section 14.4.3")
    void updateSegment_BoundToCampaign_SucceedsPerDocumentedContract() throws Exception {
        Campaign campaign = new Campaign("Bound Campaign", "Desc", existingSegment, "Template", adminUser);
        campaignRepository.saveAndFlush(campaign);

        SegmentUpdateRequest request = new SegmentUpdateRequest(
                "Updated Bound Segment", "Updated Description", objectMapper.readTree("{\"rule\":\"new\"}")
        );

        mockMvc.perform(patch("/api/v1/segments/{id}", existingSegment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", is("Updated Bound Segment")))
                .andExpect(jsonPath("$.data.description", is("Updated Description")));
    }

    @Test
    @WithMockUser(username = "marketer_tester", roles = {"MARKETER"})
    @DisplayName("DELETE /api/v1/segments/{id} unbound segment returns 204 No Content")
    void deleteSegment_Unbound_Returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/segments/{id}", existingSegment.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/segments/{id}", existingSegment.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "marketer_tester", roles = {"MARKETER"})
    @DisplayName("DELETE /api/v1/segments/{id} bound to a campaign returns 409 Conflict")
    void deleteSegment_BoundToCampaign_Returns409() throws Exception {
        Campaign campaign = new Campaign("Linked Campaign", "Desc", existingSegment, "Template", adminUser);
        campaignRepository.saveAndFlush(campaign);

        mockMvc.perform(delete("/api/v1/segments/{id}", existingSegment.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code", is("CONFLICT")))
                .andExpect(jsonPath("$.error.message", containsString("bound to one or more campaigns")));
    }

    @Test
    @WithMockUser(username = "marketer_tester", roles = {"MARKETER"})
    @DisplayName("GET /api/v1/segments returns paginated directory of segments")
    void listSegments_Returns200WithPagination() throws Exception {
        mockMvc.perform(get("/api/v1/segments?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.metadata.pagination.totalElements", is(1)))
                .andExpect(jsonPath("$.metadata.pagination.page", is(0)));
    }
}
