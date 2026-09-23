package com.crm.platform.ai.controller;

import com.crm.platform.ai.repository.AiSegmentAuditRepository;
import com.crm.platform.user.entity.RoleEnum;
import com.crm.platform.user.entity.User;
import com.crm.platform.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AiSegmentAuditRepository auditRepository;

    @BeforeEach
    void setUp() {
        auditRepository.deleteAll();

        if (!userRepository.existsByUsername("admin_ai_test")) {
            User admin = new User("admin_ai_test", "admin_ai_test@crm.internal", "$2a$12$hash", RoleEnum.ROLE_ADMIN);
            userRepository.save(admin);
        }
        if (!userRepository.existsByUsername("marketer_ai_test")) {
            User marketer = new User("marketer_ai_test", "marketer_ai_test@crm.internal", "$2a$12$hash", RoleEnum.ROLE_MARKETER);
            userRepository.save(marketer);
        }
    }

    @AfterEach
    void tearDown() {
        auditRepository.deleteAll();
    }

    @Test
    @DisplayName("Should return 401 for unauthenticated rule generation request")
    void testGenerateRulesUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/ai/segments/generate-rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"Target customers living in Mumbai who spent over 15000\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should successfully generate rule AST for authenticated MARKETER")
    @WithMockUser(username = "marketer_ai_test", roles = {"MARKETER"})
    void testGenerateRulesAuthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/ai/segments/generate-rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"Target customers living in Mumbai who spent over 15000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.isValidated", is(true)))
                .andExpect(jsonPath("$.data.ruleTree", notNullValue()));
    }

    @Test
    @DisplayName("Should return 400 for prompt shorter than 10 characters")
    @WithMockUser(username = "marketer_ai_test", roles = {"MARKETER"})
    void testGenerateRulesPromptTooShort() throws Exception {
        mockMvc.perform(post("/api/v1/ai/segments/generate-rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"Short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("VALIDATION_FAILED")));
    }

    @Test
    @DisplayName("Should permit ROLE_ADMIN to view compliance audits")
    @WithMockUser(username = "admin_ai_test", roles = {"ADMIN"})
    void testGetAuditsAsAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/ai/segments/audits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", notNullValue()))
                .andExpect(jsonPath("$.metadata.pagination", notNullValue()));
    }

    @Test
    @DisplayName("Should return 403 Forbidden when ROLE_MARKETER accesses audits")
    @WithMockUser(username = "marketer_ai_test", roles = {"MARKETER"})
    void testGetAuditsAsMarketerForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/ai/segments/audits"))
                .andExpect(status().isForbidden());
    }
}
