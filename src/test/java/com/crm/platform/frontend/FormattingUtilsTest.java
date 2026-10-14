package com.crm.platform.frontend;

import com.crm.platform.frontend.util.FormattingUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("FormattingUtils Unit Tests")
class FormattingUtilsTest {

    @Test
    @DisplayName("Should format currency with INR symbol and thousands separators")
    void shouldFormatCurrency() {
        assertEquals("₹0.00", FormattingUtils.formatCurrency(null));
        assertEquals("₹0.00", FormattingUtils.formatCurrency(BigDecimal.ZERO));
        assertEquals("₹12,450.50", FormattingUtils.formatCurrency(new BigDecimal("12450.50")));
        assertEquals("₹1,000,000.00", FormattingUtils.formatCurrency(new BigDecimal("1000000.00")));
    }

    @Test
    @DisplayName("Should format numbers with commas")
    void shouldFormatInteger() {
        assertEquals("0", FormattingUtils.formatInteger(null));
        assertEquals("0", FormattingUtils.formatInteger(0));
        assertEquals("1,250", FormattingUtils.formatInteger(1250));
        assertEquals("9,876,543", FormattingUtils.formatNumber(9876543L));
    }

    @Test
    @DisplayName("Should format percentages with one decimal digit")
    void shouldFormatPercentage() {
        assertEquals("0.0%", FormattingUtils.formatPercentage(0.0));
        assertEquals("94.6%", FormattingUtils.formatPercentage(94.62));
        assertEquals("100.0%", FormattingUtils.formatPercentage(100.0));
    }

    @Test
    @DisplayName("Should format date and date time strings")
    void shouldFormatDateAndTime() {
        assertEquals("—", FormattingUtils.formatDate(null));
        LocalDate date = LocalDate.of(2026, 10, 14);
        assertEquals("14 Oct 2026", FormattingUtils.formatDate(date));

        assertEquals("—", FormattingUtils.formatDateTime(null));
        LocalDateTime dateTime = LocalDateTime.of(2026, 10, 14, 15, 30);
        assertEquals("14 Oct 2026 15:30", FormattingUtils.formatDateTime(dateTime));

        Instant instant = Instant.parse("2026-10-14T10:00:00Z");
        assertEquals("14 Oct 2026 15:30", FormattingUtils.formatInstant(instant, ZoneId.of("Asia/Kolkata")));
    }
}
