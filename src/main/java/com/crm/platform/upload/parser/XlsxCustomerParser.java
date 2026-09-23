package com.crm.platform.upload.parser;

import com.crm.platform.common.exception.InvalidRequestException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.model.StylesTable;
import org.springframework.stereotype.Component;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import javax.xml.parsers.SAXParserFactory;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Component
public class XlsxCustomerParser implements CustomerStreamingParser {

    @Override
    public void parse(InputStream inputStream, Consumer<ParsedRow> rowConsumer) throws Exception {
        OPCPackage pkg;
        try {
            pkg = OPCPackage.open(inputStream);
        } catch (Exception e) {
            throw new InvalidRequestException("Malformed or unreadable XLSX file: " + e.getMessage());
        }

        try {
            ReadOnlySharedStringsTable strings = new ReadOnlySharedStringsTable(pkg);
            XSSFReader xssfReader = new XSSFReader(pkg);
            StylesTable styles = xssfReader.getStylesTable();
            XSSFReader.SheetIterator iter = (XSSFReader.SheetIterator) xssfReader.getSheetsData();

            if (!iter.hasNext()) {
                throw new InvalidRequestException("Uploaded XLSX file contains no sheets");
            }

            try (InputStream sheetStream = iter.next()) {
                SheetProcessor processor = new SheetProcessor(rowConsumer);
                SAXParserFactory saxFactory = SAXParserFactory.newInstance();
                saxFactory.setNamespaceAware(true);
                // Mitigate XML External Entity (XXE) vulnerabilities
                saxFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                saxFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
                saxFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

                XMLReader sheetParser = saxFactory.newSAXParser().getXMLReader();
                XSSFSheetXMLHandler handler = new XSSFSheetXMLHandler(styles, strings, processor, false);
                sheetParser.setContentHandler(handler);
                sheetParser.parse(new InputSource(sheetStream));

                if (processor.isHeaderMissing()) {
                    throw new InvalidRequestException("Uploaded XLSX file is empty");
                }
                if (processor.getDataRowCount() == 0) {
                    throw new InvalidRequestException("Uploaded file contains headers but no data rows");
                }
            }
        } finally {
            pkg.revert();
        }
    }

    private static class SheetProcessor implements XSSFSheetXMLHandler.SheetContentsHandler {
        private final Consumer<ParsedRow> rowConsumer;
        private final Map<Integer, String> headerIndexMap = new HashMap<>();
        private final Map<String, Integer> colMap = new HashMap<>();
        private final Map<Integer, String> currentRowData = new HashMap<>();
        private boolean isHeaderProcessed = false;
        private int dataRowCount = 0;

        public SheetProcessor(Consumer<ParsedRow> rowConsumer) {
            this.rowConsumer = rowConsumer;
        }

        public boolean isHeaderMissing() {
            return !isHeaderProcessed;
        }

        public int getDataRowCount() {
            return dataRowCount;
        }

        @Override
        public void startRow(int rowNum) {
            currentRowData.clear();
        }

        @Override
        public void endRow(int rowNum) {
            if (rowNum == 0 && !isHeaderProcessed) {
                // Process header row
                for (Map.Entry<Integer, String> entry : currentRowData.entrySet()) {
                    String norm = normalizeHeader(entry.getValue());
                    colMap.put(norm, entry.getKey());
                    headerIndexMap.put(entry.getKey(), norm);
                }
                validateRequiredHeaders(colMap);
                isHeaderProcessed = true;
                return;
            }

            if (!isHeaderProcessed) {
                return;
            }

            // Skip empty rows
            boolean allEmpty = currentRowData.values().stream().allMatch(v -> v == null || v.trim().isEmpty());
            if (allEmpty) {
                return;
            }

            dataRowCount++;
            int logicalRowNumber = rowNum + 1; // 1-based row index for reporting
            ParsedRow row = parseCurrentRow(logicalRowNumber);
            rowConsumer.accept(row);
        }

        @Override
        public void cell(String cellReference, String formattedValue, org.apache.poi.xssf.usermodel.XSSFComment comment) {
            if (cellReference == null) return;
            int colIndex = new CellReference(cellReference).getCol();
            currentRowData.put(colIndex, formattedValue != null ? formattedValue.trim() : null);
        }

        private String normalizeHeader(String raw) {
            if (raw == null) return "";
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

        private ParsedRow parseCurrentRow(int rowNumber) {
            ParsedRow row = new ParsedRow(rowNumber);

            row.setFirstName(getVal("firstname"));
            row.setLastName(getVal("lastname"));
            row.setEmail(getVal("email"));
            row.setPhone(getVal("phone"));

            String city = getVal("city");
            if (city == null) {
                city = getVal("location");
            }
            row.setCity(city);
            row.setCountry(getVal("country"));

            String spendStr = getVal("totalspend");
            if (spendStr != null && !spendStr.isEmpty()) {
                try {
                    // strip possible currency symbols or commas
                    String cleanSpend = spendStr.replaceAll("[$€£,]", "").trim();
                    row.setTotalSpend(new BigDecimal(cleanSpend));
                } catch (NumberFormatException e) {
                    row.setParseError("Invalid numeric totalSpend value: '" + spendStr + "'");
                }
            } else {
                row.setTotalSpend(BigDecimal.ZERO);
            }

            String visitStr = getVal("visitcount");
            if (visitStr == null) {
                visitStr = getVal("ordercount");
            }
            if (visitStr != null && !visitStr.isEmpty()) {
                try {
                    // Sometimes Excel formats integers as "5.0"
                    if (visitStr.contains(".")) {
                        visitStr = visitStr.substring(0, visitStr.indexOf("."));
                    }
                    row.setVisitCount(Integer.parseInt(visitStr));
                } catch (NumberFormatException e) {
                    row.setParseError("Invalid integer visitCount/orderCount value: '" + visitStr + "'");
                }
            } else {
                row.setVisitCount(0);
            }

            String dateStr = getVal("lastactivedate");
            if (dateStr == null) {
                dateStr = getVal("lastorderdate");
            }
            if (dateStr != null && !dateStr.isEmpty()) {
                try {
                    if (dateStr.contains("T")) {
                        dateStr = dateStr.substring(0, dateStr.indexOf("T"));
                    }
                    row.setLastActiveDate(LocalDate.parse(dateStr));
                } catch (DateTimeParseException e) {
                    row.setParseError("Invalid date format for lastActiveDate: '" + dateStr + "' (expected YYYY-MM-DD)");
                }
            }

            String tagsStr = getVal("tags");
            if (tagsStr == null) {
                tagsStr = getVal("tag");
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

        private String getVal(String key) {
            Integer colIdx = colMap.get(key);
            if (colIdx != null) {
                String val = currentRowData.get(colIdx);
                if (val != null && !val.trim().isEmpty()) {
                    return val.trim();
                }
            }
            return null;
        }
    }
}
