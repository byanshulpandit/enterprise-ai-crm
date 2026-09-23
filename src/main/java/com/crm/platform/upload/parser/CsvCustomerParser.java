package com.crm.platform.upload.parser;

import com.crm.platform.common.exception.InvalidRequestException;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Component
public class CsvCustomerParser implements CustomerStreamingParser {

    @Override
    public void parse(InputStream inputStream, Consumer<ParsedRow> rowConsumer) throws Exception {
        try (CSVReader reader = new CSVReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String[] headerLine = reader.readNext();
            if (headerLine == null || headerLine.length == 0 || (headerLine.length == 1 && headerLine[0].trim().isEmpty())) {
                throw new InvalidRequestException("Uploaded CSV file is empty");
            }

            Map<String, Integer> colMap = buildColumnMap(headerLine);
            validateRequiredHeaders(colMap);

            String[] line;
            int rowNumber = 1; // header is row 1
            while ((line = reader.readNext()) != null) {
                rowNumber++;
                // Skip completely empty lines
                if (line.length == 0 || (line.length == 1 && line[0].trim().isEmpty())) {
                    continue;
                }

                ParsedRow row = parseRow(line, colMap, rowNumber);
                rowConsumer.accept(row);
            }

            if (rowNumber == 1) {
                // Only header row was present
                throw new InvalidRequestException("Uploaded file contains headers but no data rows");
            }
        } catch (CsvValidationException | IOException e) {
            throw new InvalidRequestException("Malformed CSV file: " + e.getMessage());
        }
    }

    private Map<String, Integer> buildColumnMap(String[] headerLine) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < headerLine.length; i++) {
            if (headerLine[i] != null) {
                String normalized = normalizeHeader(headerLine[i]);
                map.put(normalized, i);
            }
        }
        return map;
    }

    private String normalizeHeader(String raw) {
        return raw.trim().toLowerCase().replaceAll("[_\\-\\s]+", "");
    }

    private void validateRequiredHeaders(Map<String, Integer> colMap) {
        boolean hasFirstName = colMap.containsKey("firstname");
        boolean hasLastName = colMap.containsKey("lastname");
        boolean hasEmail = colMap.containsKey("email");

        if (!hasFirstName || !hasLastName || !hasEmail) {
            StringBuilder sb = new StringBuilder("Missing required header(s): ");
            if (!hasFirstName) sb.append("firstName ");
            if (!hasLastName) sb.append("lastName ");
            if (!hasEmail) sb.append("email ");
            throw new InvalidRequestException(sb.toString().trim());
        }
    }

    private ParsedRow parseRow(String[] line, Map<String, Integer> colMap, int rowNumber) {
        ParsedRow row = new ParsedRow(rowNumber);

        row.setFirstName(getVal(line, colMap, "firstname"));
        row.setLastName(getVal(line, colMap, "lastname"));
        row.setEmail(getVal(line, colMap, "email"));
        row.setPhone(getVal(line, colMap, "phone"));
        
        String city = getVal(line, colMap, "city");
        if (city == null) {
            city = getVal(line, colMap, "location");
        }
        row.setCity(city);
        row.setCountry(getVal(line, colMap, "country"));

        String spendStr = getVal(line, colMap, "totalspend");
        if (spendStr != null && !spendStr.isEmpty()) {
            try {
                row.setTotalSpend(new BigDecimal(spendStr));
            } catch (NumberFormatException e) {
                row.setParseError("Invalid numeric totalSpend value: '" + spendStr + "'");
            }
        } else {
            row.setTotalSpend(BigDecimal.ZERO);
        }

        String visitStr = getVal(line, colMap, "visitcount");
        if (visitStr == null) {
            visitStr = getVal(line, colMap, "ordercount");
        }
        if (visitStr != null && !visitStr.isEmpty()) {
            try {
                row.setVisitCount(Integer.parseInt(visitStr));
            } catch (NumberFormatException e) {
                row.setParseError("Invalid integer visitCount/orderCount value: '" + visitStr + "'");
            }
        } else {
            row.setVisitCount(0);
        }

        String dateStr = getVal(line, colMap, "lastactivedate");
        if (dateStr == null) {
            dateStr = getVal(line, colMap, "lastorderdate");
        }
        if (dateStr != null && !dateStr.isEmpty()) {
            try {
                // If timestamp format like 2026-08-15T14:30:00Z, extract date part
                if (dateStr.contains("T")) {
                    dateStr = dateStr.substring(0, dateStr.indexOf("T"));
                }
                row.setLastActiveDate(LocalDate.parse(dateStr));
            } catch (DateTimeParseException e) {
                row.setParseError("Invalid date format for lastActiveDate: '" + dateStr + "' (expected YYYY-MM-DD)");
            }
        }

        String tagsStr = getVal(line, colMap, "tags");
        if (tagsStr == null) {
            tagsStr = getVal(line, colMap, "tag");
        }
        if (tagsStr != null && !tagsStr.isEmpty()) {
            Set<String> tagSet = Arrays.stream(tagsStr.split("[,;]"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());
            row.setTags(tagSet);
        }

        return row;
    }

    private String getVal(String[] line, Map<String, Integer> colMap, String key) {
        Integer idx = colMap.get(key);
        if (idx != null && idx < line.length && line[idx] != null) {
            String trimmed = line[idx].trim();
            return trimmed.isEmpty() ? null : trimmed;
        }
        return null;
    }
}
