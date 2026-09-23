package com.crm.platform.upload.service;

import com.crm.platform.common.exception.InvalidRequestException;
import com.crm.platform.customer.entity.Customer;
import com.crm.platform.customer.repository.CustomerRepository;
import com.crm.platform.upload.dto.UploadHistoryDto;
import com.crm.platform.upload.dto.UploadResultResponse;
import com.crm.platform.upload.entity.UploadFileType;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.HttpMediaTypeNotSupportedException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UploadServiceTest {

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

        testUser = new User("marketer1", "marketer1@crm.internal", "hashedpassword", RoleEnum.ROLE_MARKETER);
        testUser.setId(10L);
    }

    @Test
    @DisplayName("Should successfully process valid CSV file and record history with SUCCESS status")
    void testProcessValidCsvSuccess() throws Exception {
        when(userRepository.findByUsername("marketer1")).thenReturn(Optional.of(testUser));
        when(customerRepository.findByEmail(any())).thenReturn(Optional.empty());

        when(uploadHistoryRepository.save(any(UploadHistory.class))).thenAnswer(inv -> {
            UploadHistory h = inv.getArgument(0);
            h.setId(1L);
            return h;
        });

        String csv = """
                firstName,lastName,email,phone,city,totalSpend,visitCount,tags
                Rahul,Dravid,rahul@example.com,+919999999999,Bengaluru,5000.00,10,"Cricket, Legend"
                Sourav,Ganguly,sourav@example.com,+918888888888,Kolkata,7500.00,8,Captain
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "customers.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );

        UploadResultResponse response = uploadService.processBulkUpload(file, "marketer1");

        assertNotNull(response);
        assertEquals(UploadStatus.SUCCESS, response.getStatus());
        assertEquals(2, response.getTotalRecords());
        assertEquals(2, response.getSuccessfulRecords());
        assertEquals(0, response.getFailedRecords());
        assertTrue(response.getErrors().isEmpty());

        verify(customerRepository, atLeastOnce()).saveAll(any());
        verify(uploadHistoryRepository).save(any(UploadHistory.class));
    }

    @Test
    @DisplayName("Should process partial success when file contains duplicate and invalid rows")
    void testProcessPartialSuccess() throws Exception {
        Customer existing = new Customer();
        existing.setEmail("existing@example.com");

        when(userRepository.findByUsername("marketer1")).thenReturn(Optional.of(testUser));
        when(customerRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(existing));
        when(customerRepository.findByEmail("valid@example.com")).thenReturn(Optional.empty());

        when(uploadHistoryRepository.save(any(UploadHistory.class))).thenAnswer(inv -> {
            UploadHistory h = inv.getArgument(0);
            h.setId(2L);
            return h;
        });

        String csv = """
                firstName,lastName,email,phone,city,totalSpend,visitCount
                Valid,User,valid@example.com,+919999999999,Delhi,100.00,1
                DuplicateInFile,User,valid@example.com,+919999999999,Delhi,100.00,1
                ExistingInDb,User,existing@example.com,+919999999999,Delhi,100.00,1
                InvalidEmail,User,notanemail,+919999999999,Delhi,100.00,1
                ,NoFirstName,nofirst@example.com,+919999999999,Delhi,100.00,1
                NegativeSpend,User,neg@example.com,+919999999999,Delhi,-50.00,1
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "mixed_customers.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );

        UploadResultResponse response = uploadService.processBulkUpload(file, "marketer1");

        assertNotNull(response);
        assertEquals(UploadStatus.PARTIAL_SUCCESS, response.getStatus());
        assertEquals(6, response.getTotalRecords());
        assertEquals(1, response.getSuccessfulRecords());
        assertEquals(5, response.getFailedRecords());
        assertEquals(5, response.getErrors().size());

        ArgumentCaptor<UploadHistory> captor = ArgumentCaptor.forClass(UploadHistory.class);
        verify(uploadHistoryRepository).save(captor.capture());
        UploadHistory saved = captor.getValue();
        assertEquals(UploadStatus.PARTIAL_SUCCESS, saved.getStatus());
        assertEquals(1, saved.getSuccessCount());
        assertEquals(5, saved.getFailureCount());
    }

    @Test
    @DisplayName("Should throw HttpMediaTypeNotSupportedException for unsupported file format")
    void testUnsupportedFormat() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.json",
                "application/json",
                "{}".getBytes(StandardCharsets.UTF_8)
        );

        assertThrows(HttpMediaTypeNotSupportedException.class, () ->
                uploadService.processBulkUpload(file, "marketer1")
        );
    }

    @Test
    @DisplayName("Should throw InvalidRequestException for empty file")
    void testEmptyFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.csv",
                "text/csv",
                new byte[0]
        );

        assertThrows(InvalidRequestException.class, () ->
                uploadService.processBulkUpload(file, "marketer1")
        );
    }

    @Test
    @DisplayName("Should retrieve paginated upload history")
    void testGetUploadHistory() {
        UploadHistory h1 = new UploadHistory(10L, "f1.csv", UploadFileType.CSV, 10, 10, 0, UploadStatus.SUCCESS, null);
        h1.setId(1L);
        h1.setCreatedAt(Instant.now());

        Page<UploadHistory> page = new PageImpl<>(List.of(h1), PageRequest.of(0, 20), 1);
        when(uploadHistoryRepository.findAllByOrderByCreatedAtDesc(any())).thenReturn(page);

        Page<UploadHistoryDto> result = uploadService.getUploadHistory(PageRequest.of(0, 20));
        assertEquals(1, result.getTotalElements());
        assertEquals("f1.csv", result.getContent().get(0).getFileName());
    }
}
