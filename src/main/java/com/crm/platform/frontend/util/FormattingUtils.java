package com.crm.platform.frontend.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Standard formatting utility for currency, numbers, dates, and percentages.
 */
public final class FormattingUtils {

    private static final NumberFormat CURRENCY_FORMAT = new DecimalFormat("₹#,##0.00");
    private static final NumberFormat INTEGER_FORMAT = NumberFormat.getIntegerInstance(Locale.US);
    private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("0.0'%'");

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    private FormattingUtils() {
    }

    public static String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "₹0.00";
        }
        return CURRENCY_FORMAT.format(amount);
    }

    public static String formatInteger(Number number) {
        if (number == null) {
            return "0";
        }
        return INTEGER_FORMAT.format(number);
    }

    public static String formatNumber(Number number) {
        return formatInteger(number);
    }

    public static String formatPercentage(double percent) {
        return PERCENT_FORMAT.format(percent);
    }

    public static String formatDate(LocalDate date) {
        if (date == null) {
            return "—";
        }
        return date.format(DATE_FORMATTER);
    }

    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "—";
        }
        return dateTime.format(DATE_TIME_FORMATTER);
    }

    public static String formatInstant(Instant instant, ZoneId zoneId) {
        if (instant == null) {
            return "—";
        }
        return DATE_TIME_FORMATTER.format(instant.atZone(zoneId != null ? zoneId : ZoneId.systemDefault()));
    }
}
