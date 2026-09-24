package com.crm.platform.upload.service;

import com.crm.platform.common.exception.InvalidRequestException;
import com.crm.platform.customer.entity.Customer;
import com.crm.platform.customer.repository.CustomerRepository;
import com.crm.platform.upload.dto.UploadErrorDetail;
import com.crm.platform.upload.dto.UploadHistoryDto;
import com.crm.platform.upload.dto.UploadResultResponse;
import com.crm.platform.upload.entity.UploadFileType;
import com.crm.platform.upload.entity.UploadHistory;
import com.crm.platform.upload.entity.UploadStatus;
import com.crm.platform.upload.parser.CsvCustomerParser;
import com.crm.platform.upload.parser.CustomerStreamingParser;
import com.crm.platform.upload.parser.ParsedRow;
import com.crm.platform.upload.parser.XlsxCustomerParser;
import com.crm.platform.upload.repository.UploadHistoryRepository;
import com.crm.platform.common.exception.ResourceNotFoundException;
import com.crm.platform.user.entity.User;
import com.crm.platform.user.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class UploadServiceImpl implements UploadService {

    private static final Logger log = LoggerFactory.getLogger(UploadServiceImpl.class);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final int BATCH_SIZE = 200;

    private final CustomerRepository customerRepository;
    private final UploadHistoryRepository uploadHistoryRepository;
    private final UserRepository userRepository;
    private final CsvCustomerParser csvCustomerParser;
    private final XlsxCustomerParser xlsxCustomerParser;
    private final ObjectMapper objectMapper;
    private final UploadBatchPersister uploadBatchPersister;

    @Value("${crm.upload.max-response-errors:100}")
    private int maxResponseErrors = 100;

    public UploadServiceImpl(CustomerRepository customerRepository,
                             UploadHistoryRepository uploadHistoryRepository,
                             UserRepository userRepository,
                             CsvCustomerParser csvCustomerParser,
                             XlsxCustomerParser xlsxCustomerParser,
                             ObjectMapper objectMapper,
                             UploadBatchPersister uploadBatchPersister) {
        this.customerRepository = customerRepository;
        this.uploadHistoryRepository = uploadHistoryRepository;
        this.userRepository = userRepository;
        this.csvCustomerParser = csvCustomerParser;
        this.xlsxCustomerParser = xlsxCustomerParser;
        this.objectMapper = objectMapper;
        this.uploadBatchPersister = uploadBatchPersister;
    }

    @Override
    public UploadResultResponse processBulkUpload(MultipartFile file, String callerUsername) throws HttpMediaTypeNotSupportedException {
        if (file == null || file.isEmpty()) {
            throw new InvalidRequestException("Uploaded file is empty");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new InvalidRequestException("Uploaded file must have a valid filename");
        }

        UploadFileType fileType = detectFileType(originalFilename);
        Long uploadedBy = resolveUserId(callerUsername);
        CustomerStreamingParser parser = (fileType == UploadFileType.CSV) ? csvCustomerParser : xlsxCustomerParser;

        List<ParsedRow> batch = new ArrayList<>(BATCH_SIZE);
        List<UploadErrorDetail> allErrors = new ArrayList<>();
        Set<String> seenEmailsInFile = new HashSet<>();
        int[] totalProcessed = new int[]{0};
        int[] successCount = new int[]{0};

        try (InputStream is = file.getInputStream()) {
            parser.parse(is, row -> {
                totalProcessed[0]++;

                if (row.getParseError() != null) {
                    allErrors.add(new UploadErrorDetail(row.getRowNumber(), row.getEmail() != null ? row.getEmail() : "N/A", row.getParseError()));
                    return;
                }

                String validationError = validateRow(row, seenEmailsInFile);
                if (validationError != null) {
                    allErrors.add(new UploadErrorDetail(row.getRowNumber(), row.getEmail() != null ? row.getEmail() : "N/A", validationError));
                    return;
                }

                batch.add(row);
                if (batch.size() >= BATCH_SIZE) {
                    int saved = persistBatchWithFallback(batch, allErrors);
                    successCount[0] += saved;
                    batch.clear();
                }
            });

            if (!batch.isEmpty()) {
                int saved = persistBatchWithFallback(batch, allErrors);
                successCount[0] += saved;
                batch.clear();
            }
        } catch (InvalidRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse uploaded file: {}", originalFilename, e);
            throw new InvalidRequestException("Failed to process file: " + e.getMessage());
        }

        int failedCount = allErrors.size();
        UploadStatus status;
        if (failedCount == 0 && successCount[0] > 0) {
            status = UploadStatus.SUCCESS;
        } else if (successCount[0] > 0 && failedCount > 0) {
            status = UploadStatus.PARTIAL_SUCCESS;
        } else {
            status = UploadStatus.FAILED;
        }

        String errorDetailsJson = serializeErrors(allErrors);
        UploadHistory history = new UploadHistory(
                uploadedBy,
                originalFilename,
                fileType,
                totalProcessed[0],
                successCount[0],
                failedCount,
                status,
                errorDetailsJson
        );
        history = uploadHistoryRepository.save(history);

        List<UploadErrorDetail> responseErrors = allErrors;
        if (allErrors.size() > maxResponseErrors) {
            responseErrors = allErrors.subList(0, maxResponseErrors);
        }

        return new UploadResultResponse(
                history.getId(),
                originalFilename,
                status,
                totalProcessed[0],
                successCount[0],
                failedCount,
                responseErrors
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UploadHistoryDto> getUploadHistory(Pageable pageable) {
        return uploadHistoryRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toDto);
    }

    private UploadFileType detectFileType(String filename) throws HttpMediaTypeNotSupportedException {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".csv")) {
            return UploadFileType.CSV;
        } else if (lower.endsWith(".xlsx")) {
            return UploadFileType.XLSX;
        } else {
            throw new HttpMediaTypeNotSupportedException("Only CSV (.csv) and Excel (.xlsx) file formats are supported");
        }
    }

    private String validateRow(ParsedRow row, Set<String> seenEmailsInFile) {
        if (row.getFirstName() == null || row.getFirstName().trim().isEmpty()) {
            return "First name is required";
        }
        if (row.getFirstName().length() > 100) {
            return "First name must not exceed 100 characters";
        }

        if (row.getLastName() == null || row.getLastName().trim().isEmpty()) {
            return "Last name is required";
        }
        if (row.getLastName().length() > 100) {
            return "Last name must not exceed 100 characters";
        }

        if (row.getEmail() == null || row.getEmail().trim().isEmpty()) {
            return "Email is required";
        }
        String email = row.getEmail().trim();
        if (email.length() > 255) {
            return "Email must not exceed 255 characters";
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return "Email must be a valid email address";
        }

        if (row.getPhone() != null && row.getPhone().length() > 30) {
            return "Phone must not exceed 30 characters";
        }
        if (row.getCity() != null && row.getCity().length() > 100) {
            return "City must not exceed 100 characters";
        }
        if (row.getCountry() != null && row.getCountry().length() > 100) {
            return "Country must not exceed 100 characters";
        }

        if (row.getTotalSpend() != null && row.getTotalSpend().compareTo(BigDecimal.ZERO) < 0) {
            return "Total spend must be greater than or equal to 0.00";
        }
        if (row.getVisitCount() != null && row.getVisitCount() < 0) {
            return "Visit count cannot be negative";
        }

        String normalizedEmail = email.toLowerCase();
        if (!seenEmailsInFile.add(normalizedEmail)) {
            return "Duplicate email address within uploaded file: " + email;
        }

        java.util.Optional<Customer> existingCustomer = customerRepository.findByEmail(normalizedEmail);
        if (existingCustomer.isEmpty()) {
            existingCustomer = customerRepository.findByEmail(email);
        }
        if (existingCustomer.isPresent()) {
            if (existingCustomer.get().getDeletedAt() != null) {
                return "Customer with email already exists (soft-deleted): " + email;
            } else {
                return "Customer with email already exists: " + email;
            }
        }

        return null;
    }

    public int persistBatchWithFallback(List<ParsedRow> batch, List<UploadErrorDetail> allErrors) {
        List<Customer> customersToSave = new ArrayList<>(batch.size());
        for (ParsedRow row : batch) {
            customersToSave.add(toCustomerEntity(row));
        }

        try {
            uploadBatchPersister.persistBatch(customersToSave);
            return customersToSave.size();
        } catch (Exception batchEx) {
            log.warn("Batch save failed for {} records; invoking per-row fallback: {}",
                    batch.size(), batchEx.getMessage());
            int savedCount = 0;
            for (int i = 0; i < batch.size(); i++) {
                ParsedRow row = batch.get(i);
                Customer customer = customersToSave.get(i);
                try {
                    uploadBatchPersister.persistSingle(customer);
                    savedCount++;
                } catch (Exception rowEx) {
                    log.debug("Fallback per-row save failed for row #{}, email={}: {}",
                            row.getRowNumber(), row.getEmail(), rowEx.getMessage());
                    allErrors.add(new UploadErrorDetail(
                            row.getRowNumber(),
                            row.getEmail() != null ? row.getEmail() : "N/A",
                            "Database constraint error: " + (rowEx.getMessage() != null ? rowEx.getMessage() : "Insert failed")
                    ));
                }
            }
            return savedCount;
        }
    }

    private Customer toCustomerEntity(ParsedRow row) {
        Customer customer = new Customer();
        customer.setFirstName(row.getFirstName().trim());
        customer.setLastName(row.getLastName().trim());
        customer.setEmail(row.getEmail().trim());
        customer.setPhone(row.getPhone());
        customer.setCity(row.getCity());
        customer.setCountry(row.getCountry());
        customer.setTotalSpend(row.getTotalSpend() != null ? row.getTotalSpend() : BigDecimal.ZERO);
        customer.setVisitCount(row.getVisitCount() != null ? row.getVisitCount() : 0);
        customer.setLastActiveDate(row.getLastActiveDate());

        if (row.getTags() != null) {
            for (String tag : row.getTags()) {
                customer.addTag(tag);
            }
        }
        return customer;
    }

    private String serializeErrors(List<UploadErrorDetail> errors) {
        if (errors == null || errors.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(errors);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize upload errors to JSON", e);
            return "[]";
        }
    }

    private Long resolveUserId(String username) {
        if (username == null || username.isBlank()) {
            throw new InvalidRequestException("Authenticated user identity is required");
        }
        return userRepository.findByUsername(username)
                .map(User::getId)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + username));
    }

    private UploadHistoryDto toDto(UploadHistory entity) {
        return new UploadHistoryDto(
                entity.getId(),
                entity.getUploadedBy(),
                entity.getFileName(),
                entity.getFileType(),
                entity.getTotalRows(),
                entity.getSuccessCount(),
                entity.getFailureCount(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }
}
