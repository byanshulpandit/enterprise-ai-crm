package com.crm.platform.upload.service;

import com.crm.platform.customer.entity.Customer;
import com.crm.platform.customer.repository.CustomerRepository;
import com.crm.platform.upload.dto.UploadResultResponse;
import com.crm.platform.upload.entity.UploadHistory;
import com.crm.platform.upload.entity.UploadStatus;
import com.crm.platform.upload.parser.CsvCustomerParser;
import com.crm.platform.upload.parser.XlsxCustomerParser;
import com.crm.platform.upload.repository.UploadHistoryRepository;
import com.crm.platform.user.entity.RoleEnum;
import com.crm.platform.user.entity.User;
import com.crm.platform.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UploadHardeningTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private UploadHistoryRepository uploadHistoryRepository;

    @Mock
    private UserRepository userRepository;

    private UploadServiceImpl uploadService;
    private User testUser;

    @BeforeEach
    void setUp() {
        uploadService = new UploadServiceImpl(
                customerRepository,
                uploadHistoryRepository,
                userRepository,
                new CsvCustomerParser(),
                new XlsxCustomerParser(),
                new ObjectMapper()
        );

        testUser = new User("admin", "admin@crm.internal", "hashedpassword", RoleEnum.ROLE_ADMIN);
        testUser.setId(1L);
    }

    @Test
    @DisplayName("Should detect and reject duplicate of soft-deleted customer email with explicit message")
    void testUpload_RejectsSoftDeletedEmailDuplicate() throws Exception {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));

        Customer softDeleted = new Customer();
        softDeleted.setId(99L);
        softDeleted.setEmail("deleted@example.com");
        softDeleted.setDeletedAt(Instant.now().minusSeconds(86400));

        when(customerRepository.findByEmail("deleted@example.com")).thenReturn(Optional.of(softDeleted));
        when(customerRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());

        when(uploadHistoryRepository.save(any(UploadHistory.class))).thenAnswer(inv -> {
            UploadHistory h = inv.getArgument(0);
            h.setId(10L);
            return h;
        });

        String csv = """
                firstName,lastName,email
                Alice,Smith,deleted@example.com
                Bob,Jones,new@example.com
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );

        UploadResultResponse response = uploadService.processBulkUpload(file, "admin");

        assertThat(response.getStatus()).isEqualTo(UploadStatus.PARTIAL_SUCCESS);
        assertThat(response.getSuccessfulRecords()).isEqualTo(1);
        assertThat(response.getFailedRecords()).isEqualTo(1);
        assertThat(response.getErrors().get(0).getReason()).contains("Customer with email already exists (soft-deleted)");
    }

    @Test
    @DisplayName("Should invoke per-row fallback when batch persistence encounters database exception")
    void testUpload_BatchSaveFallbackToPerRow() throws Exception {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));
        when(customerRepository.findByEmail(any())).thenReturn(Optional.empty());

        when(uploadHistoryRepository.save(any(UploadHistory.class))).thenAnswer(inv -> {
            UploadHistory h = inv.getArgument(0);
            h.setId(11L);
            return h;
        });

        // Batch save fails
        doThrow(new DataIntegrityViolationException("Simulated constraint violation in batch"))
                .when(customerRepository).saveAll(any());

        // Per-row save succeeds for first, fails for second
        when(customerRepository.saveAndFlush(any(Customer.class)))
                .thenAnswer(inv -> inv.getArgument(0))
                .thenThrow(new DataIntegrityViolationException("Unique constraint failure on row 2"));

        String csv = """
                firstName,lastName,email
                First,User,first@example.com
                Second,User,second@example.com
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "batch_fallback.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );

        UploadResultResponse response = uploadService.processBulkUpload(file, "admin");

        assertThat(response.getStatus()).isEqualTo(UploadStatus.PARTIAL_SUCCESS);
        assertThat(response.getSuccessfulRecords()).isEqualTo(1);
        assertThat(response.getFailedRecords()).isEqualTo(1);
        verify(customerRepository, atLeastOnce()).saveAndFlush(any(Customer.class));
    }
}
