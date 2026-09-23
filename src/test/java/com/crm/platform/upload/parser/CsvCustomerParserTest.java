package com.crm.platform.upload.parser;

import com.crm.platform.common.exception.InvalidRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvCustomerParserTest {

    private CsvCustomerParser parser;

    @BeforeEach
    void setUp() {
        parser = new CsvCustomerParser();
    }

    @Test
    @DisplayName("Should successfully parse valid CSV rows with various column formats")
    void testParseValidCsv() throws Exception {
        String csv = """
                firstName,last_name,EMAIL,Phone,location,Country,total_spend,visit_count,last_active_date,tags
                Aarav,Sharma,aarav@example.com,+919876543210,Mumbai,India,12500.50,5,2026-08-15,"VIP, Retail"
                Priya,Patel,priya@example.com,,Delhi,India,0,0,,
                """;

        List<ParsedRow> rows = new ArrayList<>();
        parser.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), rows::add);

        assertEquals(2, rows.size());

        ParsedRow r1 = rows.get(0);
        assertEquals(2, r1.getRowNumber());
        assertEquals("Aarav", r1.getFirstName());
        assertEquals("Sharma", r1.getLastName());
        assertEquals("aarav@example.com", r1.getEmail());
        assertEquals("+919876543210", r1.getPhone());
        assertEquals("Mumbai", r1.getCity());
        assertEquals("India", r1.getCountry());
        assertEquals(new BigDecimal("12500.50"), r1.getTotalSpend());
        assertEquals(5, r1.getVisitCount());
        assertEquals(LocalDate.of(2026, 8, 15), r1.getLastActiveDate());
        assertTrue(r1.getTags().contains("VIP"));
        assertTrue(r1.getTags().contains("Retail"));
        assertNull(r1.getParseError());

        ParsedRow r2 = rows.get(1);
        assertEquals(3, r2.getRowNumber());
        assertEquals("Priya", r2.getFirstName());
        assertEquals("Patel", r2.getLastName());
        assertEquals("priya@example.com", r2.getEmail());
        assertNull(r2.getPhone());
        assertEquals("Delhi", r2.getCity());
        assertEquals(BigDecimal.ZERO, r2.getTotalSpend());
        assertEquals(0, r2.getVisitCount());
        assertNull(r2.getLastActiveDate());
        assertTrue(r2.getTags().isEmpty());
    }

    @Test
    @DisplayName("Should throw InvalidRequestException when required headers are missing")
    void testMissingRequiredHeaders() {
        String csv = """
                firstName,phone,location
                Aarav,+919876543210,Mumbai
                """;

        InvalidRequestException ex = assertThrows(InvalidRequestException.class, () ->
                parser.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), r -> {})
        );
        assertTrue(ex.getMessage().contains("Missing required header(s)"));
        assertTrue(ex.getMessage().contains("lastName"));
        assertTrue(ex.getMessage().contains("email"));
    }

    @Test
    @DisplayName("Should capture parse errors on invalid numeric or date fields")
    void testInvalidFieldFormats() throws Exception {
        String csv = """
                firstName,lastName,email,totalSpend,visitCount,lastActiveDate
                Rohan,Verma,rohan@example.com,not-a-number,three,invalid-date
                """;

        List<ParsedRow> rows = new ArrayList<>();
        parser.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), rows::add);

        assertEquals(1, rows.size());
        ParsedRow r = rows.get(0);
        assertNotNull(r.getParseError());
    }

    @Test
    @DisplayName("Should throw InvalidRequestException on empty CSV")
    void testEmptyCsv() {
        String csv = "";
        assertThrows(InvalidRequestException.class, () ->
                parser.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), r -> {})
        );
    }

    @Test
    @DisplayName("Should throw InvalidRequestException when CSV has headers only")
    void testHeadersOnlyCsv() {
        String csv = "firstName,lastName,email\n";
        assertThrows(InvalidRequestException.class, () ->
                parser.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), r -> {})
        );
    }
}
