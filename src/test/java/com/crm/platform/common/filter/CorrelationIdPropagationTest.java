package com.crm.platform.common.filter;

import com.crm.platform.delivery.entity.CampaignDeliveryOutbox;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

public class CorrelationIdPropagationTest {

    @Test
    @DisplayName("RequestIdFilter preserves supplied X-Request-Id, sets response header, and cleans up MDC")
    void testFilter_PreservesSuppliedRequestId_AndCleansUpMdc() throws ServletException, IOException {
        RequestIdFilter filter = new RequestIdFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        String suppliedId = "test-correlation-id-12345";
        request.addHeader(RequestIdFilter.REQUEST_ID_HEADER, suppliedId);

        AtomicReference<String> capturedMdcId = new AtomicReference<>();
        FilterChain chain = (req, res) -> {
            capturedMdcId.set(MDC.get(RequestIdFilter.MDC_REQUEST_ID_KEY));
        };

        filter.doFilter(request, response, chain);

        assertThat(capturedMdcId.get()).isEqualTo(suppliedId);
        assertThat(response.getHeader(RequestIdFilter.REQUEST_ID_HEADER)).isEqualTo(suppliedId);
        // Ensure MDC was cleaned up in finally block
        assertThat(MDC.get(RequestIdFilter.MDC_REQUEST_ID_KEY)).isNull();
    }

    @Test
    @DisplayName("RequestIdFilter generates UUID when X-Request-Id header is absent, and cleans up MDC")
    void testFilter_GeneratesUuidWhenAbsent_AndCleansUpMdc() throws ServletException, IOException {
        RequestIdFilter filter = new RequestIdFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> capturedMdcId = new AtomicReference<>();
        FilterChain chain = (req, res) -> {
            capturedMdcId.set(MDC.get(RequestIdFilter.MDC_REQUEST_ID_KEY));
        };

        filter.doFilter(request, response, chain);

        assertThat(capturedMdcId.get()).isNotBlank();
        // Check that it's a valid UUID
        UUID.fromString(capturedMdcId.get());
        assertThat(response.getHeader(RequestIdFilter.REQUEST_ID_HEADER)).isEqualTo(capturedMdcId.get());
        // MDC cleaned up
        assertThat(MDC.get(RequestIdFilter.MDC_REQUEST_ID_KEY)).isNull();
    }

    @Test
    @DisplayName("CampaignDeliveryOutbox accurately stores correlation ID across constructor and getters")
    void testOutboxCorrelationId_Preserved() {
        String testCorrelationId = "corr-xyz-987";
        CampaignDeliveryOutbox outbox = new CampaignDeliveryOutbox(10L, 20L, 30L, testCorrelationId);

        assertThat(outbox.getCorrelationId()).isEqualTo(testCorrelationId);
        assertThat(outbox.getCampaignId()).isEqualTo(10L);
        assertThat(outbox.getCustomerId()).isEqualTo(20L);
        assertThat(outbox.getDeliveryRecordId()).isEqualTo(30L);
    }
}
