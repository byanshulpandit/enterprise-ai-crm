package com.crm.platform.upload.parser;

import com.crm.platform.common.exception.InvalidRequestException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class XlsxCustomerParserTest {

    private XlsxCustomerParser parser;

    @BeforeEach
    void setUp() {
        parser = new XlsxCustomerParser();
    }

    private byte[] createXlsx(List<List<String>> rows) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Customers");
            for (int r = 0; r < rows.size(); r++) {
                Row row = sheet.createRow(r);
                List<String> cells = rows.get(r);
                for (int c = 0; c < cells.size(); c++) {
                    row.createCell(c).setCellValue(cells.get(c));
                }
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }

    @Test
    @DisplayName("Should successfully parse valid XLSX rows")
    void testParseValidXlsx() throws Exception {
        List<List<String>> data = List.of(
                List.of("First Name", "Last Name", "Email", "City", "Total Spend", "Visit Count", "Last Active Date", "Tags"),
                List.of("Ananya", "Deshmukh", "ananya@example.com", "Pune", "5200.75", "4", "2026-07-20", "VIP, Active"),
                List.of("Vikram", "Malhotra", "vikram@example.com", "Bengaluru", "0.00", "0", "", "")
        );

        byte[] xlsxBytes = createXlsx(data);
        List<ParsedRow> rows = new ArrayList<>();
        parser.parse(new ByteArrayInputStream(xlsxBytes), rows::add);

        assertEquals(2, rows.size());

        ParsedRow r1 = rows.get(0);
        assertEquals("Ananya", r1.getFirstName());
        assertEquals("Deshmukh", r1.getLastName());
        assertEquals("ananya@example.com", r1.getEmail());
        assertEquals("Pune", r1.getCity());
        assertEquals(new BigDecimal("5200.75"), r1.getTotalSpend());
        assertEquals(4, r1.getVisitCount());
        assertEquals(LocalDate.of(2026, 7, 20), r1.getLastActiveDate());
        assertTrue(r1.getTags().contains("VIP"));
        assertNull(r1.getParseError());

        ParsedRow r2 = rows.get(1);
        assertEquals("Vikram", r2.getFirstName());
        assertEquals("Malhotra", r2.getLastName());
        assertEquals("vikram@example.com", r2.getEmail());
        assertEquals(0, BigDecimal.ZERO.compareTo(r2.getTotalSpend()));
        assertEquals(0, r2.getVisitCount());
        assertNull(r2.getLastActiveDate());
    }

    @Test
    @DisplayName("Should throw InvalidRequestException when required headers are missing in XLSX")
    void testMissingRequiredHeadersXlsx() throws Exception {
        List<List<String>> data = List.of(
                List.of("First Name", "City", "Total Spend"),
                List.of("Ananya", "Pune", "5200.75")
        );

        byte[] xlsxBytes = createXlsx(data);
        assertThrows(InvalidRequestException.class, () ->
                parser.parse(new ByteArrayInputStream(xlsxBytes), r -> {})
        );
    }

    @Test
    @DisplayName("Should reject malformed bytes as InvalidRequestException")
    void testMalformedXlsx() {
        byte[] garbage = "not an excel file".getBytes();
        assertThrows(InvalidRequestException.class, () ->
                parser.parse(new ByteArrayInputStream(garbage), r -> {})
        );
    }
}
