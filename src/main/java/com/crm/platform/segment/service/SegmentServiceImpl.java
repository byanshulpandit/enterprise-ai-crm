package com.crm.platform.segment.service;

import com.crm.platform.campaign.repository.CampaignRepository;
import com.crm.platform.common.exception.ConflictException;
import com.crm.platform.common.exception.DuplicateResourceException;
import com.crm.platform.common.exception.InvalidRequestException;
import com.crm.platform.common.exception.ResourceNotFoundException;
import com.crm.platform.customer.dto.CustomerResponseDto;
import com.crm.platform.customer.entity.Customer;
import com.crm.platform.customer.mapper.CustomerMapper;
import com.crm.platform.customer.repository.CustomerRepository;
import com.crm.platform.segment.compiler.SegmentCriteriaCompiler;
import com.crm.platform.segment.dto.SegmentCreateRequest;
import com.crm.platform.segment.dto.SegmentPreviewResponse;
import com.crm.platform.segment.dto.SegmentUpdateRequest;
import com.crm.platform.segment.entity.Segment;
import com.crm.platform.segment.model.RuleNode;
import com.crm.platform.segment.parser.SegmentRuleParser;
import com.crm.platform.segment.repository.SegmentRepository;
import com.crm.platform.user.entity.User;
import com.crm.platform.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional
public class SegmentServiceImpl implements SegmentService {

    private final SegmentRepository segmentRepository;
    private final CampaignRepository campaignRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final SegmentRuleParser ruleParser;
    private final SegmentCriteriaCompiler criteriaCompiler;

    public SegmentServiceImpl(SegmentRepository segmentRepository,
                              CampaignRepository campaignRepository,
                              UserRepository userRepository,
                              CustomerRepository customerRepository,
                              CustomerMapper customerMapper,
                              SegmentRuleParser ruleParser,
                              SegmentCriteriaCompiler criteriaCompiler) {
        this.segmentRepository = segmentRepository;
        this.campaignRepository = campaignRepository;
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.customerMapper = customerMapper;
        this.ruleParser = ruleParser;
        this.criteriaCompiler = criteriaCompiler;
    }

    @Override
    public Segment createSegment(SegmentCreateRequest request, String callerUsername) {
        if (request == null) {
            throw new InvalidRequestException("Segment creation request must not be null");
        }
        if (request.getRules() == null || request.getRules().isNull()) {
            throw new InvalidRequestException("Segment rules must not be null");
        }

        String trimmedName = request.getName().trim();
        if (segmentRepository.existsByName(trimmedName)) {
            throw new DuplicateResourceException("Segment with name '" + trimmedName + "' already exists");
        }

        User creator = resolveUser(callerUsername);
        String rulesJson = request.getRules().toString();
        String description = request.getDescription() != null ? request.getDescription().trim() : null;

        Segment segment = new Segment(
                trimmedName,
                description,
                rulesJson,
                creator
        );

        return segmentRepository.save(segment);
    }

    @Override
    @Transactional(readOnly = true)
    public Segment getSegmentById(Long id) {
        if (id == null) {
            throw new ResourceNotFoundException("Segment id must not be null");
        }
        return segmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Segment not found with id: " + id));
    }

    @Override
    public Segment updateSegment(Long id, SegmentUpdateRequest request) {
        if (request == null) {
            throw new InvalidRequestException("Segment update request must not be null");
        }
        Segment segment = getSegmentById(id);

        if (request.getName() != null && !request.getName().isBlank()) {
            String newName = request.getName().trim();
            if (segmentRepository.existsByNameAndIdNot(newName, id)) {
                throw new DuplicateResourceException("Segment with name '" + newName + "' already exists");
            }
            segment.setName(newName);
        }
        if (request.getDescription() != null) {
            segment.setDescription(request.getDescription().trim());
        }
        if (request.getRules() != null && !request.getRules().isNull()) {
            segment.setRules(request.getRules().toString());
        }

        return segmentRepository.save(segment);
    }

    @Override
    public void deleteSegment(Long id) {
        Segment segment = getSegmentById(id);

        if (campaignRepository.existsBySegmentId(id)) {
            throw new ConflictException("Segment cannot be deleted because it is bound to one or more campaigns");
        }

        segmentRepository.delete(segment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Segment> listSegments(Pageable pageable) {
        return segmentRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Specification<Customer> compileSegmentRules(Long id) {
        Segment segment = getSegmentById(id);
        RuleNode ast = ruleParser.parse(segment.getRules());
        return criteriaCompiler.compile(ast);
    }

    @Override
    @Transactional(readOnly = true)
    public SegmentPreviewResponse previewSegment(Long id) {
        Segment segment = getSegmentById(id);
        Specification<Customer> spec = compileSegmentRules(id);
        long count = customerRepository.count(spec);
        return new SegmentPreviewResponse(
                segment.getId(),
                segment.getName(),
                count,
                Instant.now()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerResponseDto> getSegmentMembers(Long id, Pageable pageable) {
        Specification<Customer> spec = compileSegmentRules(id);
        Pageable effectivePageable = pageable != null ? pageable : PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "id"));
        Page<Customer> page = customerRepository.findAll(spec, effectivePageable);
        return page.map(customerMapper::toDto);
    }

    private User resolveUser(String username) {
        if (username == null || username.isBlank()) {
            throw new InvalidRequestException("Authenticated user identity is required");
        }
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + username));
    }
}
