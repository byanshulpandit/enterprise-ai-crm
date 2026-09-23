package com.crm.platform.upload.controller;

import com.crm.platform.customer.repository.CustomerRepository;
import com.crm.platform.upload.repository.UploadHistoryRepository;
import com.crm.platform.user.entity.RoleEnum;
import com.crm.platform.user.entity.User;
import com.crm.platform.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class UploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private UploadHistoryRepository uploadHistoryRepository;

    @BeforeEach
    void setUp() {
        if (!userRepository.existsByUsername("marketer_upload")) {
            User marketer = new User("marketer_upload", "marketer_upload@crm.internal", "$2a$12$e/samplehash", RoleEnum.ROLE_MARKETER);
            userRepository.save(marketer);
        }
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        uploadHistoryRepository.deleteAll();
    }

    @Test
    @DisplayName("Should successfully upload CSV when authenticated as MARKETER")
    @WithMockUser(username = "marketer_upload", roles = {"MARKETER"})
    void testUploadCsvAsMarketer() throws Exception {
        long ts = System.currentTimeMillis();
        String csv = "firstName,lastName,email,phone,city,totalSpend,visitCount,tags\n" +
                "UploadA,Test,uploada_" + ts + "@example.com,+911111111111,Mumbai,150.00,2,VIP\n" +
                "UploadB,Test,uploadb_" + ts + "@example.com,+912222222222,Delhi,250.00,4,\"Retail, Gold\"\n";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test_upload.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/v1/uploads/bulk").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.fileName", is("test_upload.csv")))
                .andExpect(jsonPath("$.data.status", is("SUCCESS")))
                .andExpect(jsonPath("$.data.totalRecords", is(2)))
                .andExpect(jsonPath("$.data.successfulRecords", is(2)))
                .andExpect(jsonPath("$.data.failedRecords", is(0)));
    }

    @Test
    @DisplayName("Should reject unauthenticated upload with 401")
    void testUploadUnauthenticated() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.csv",
                "text/csv",
                "firstName,lastName,email\nA,B,a@b.com".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/v1/uploads/bulk").file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should reject unsupported file format with 415")
    @WithMockUser(username = "marketer_upload", roles = {"MARKETER"})
    void testUploadUnsupportedFormat() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "some content".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/v1/uploads/bulk").file(file))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("UNSUPPORTED_MEDIA_TYPE")));
    }

    @Test
    @DisplayName("Should retrieve paginated upload history as MARKETER")
    @WithMockUser(username = "marketer_upload", roles = {"MARKETER"})
    void testGetHistory() throws Exception {
        mockMvc.perform(get("/api/v1/uploads/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", notNullValue()))
                .andExpect(jsonPath("$.metadata.pagination", notNullValue()));
    }
}
