package com.example.messagebroadcast.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ExcelParserService {

    public List<String> extractAllPhoneNumbers(MultipartFile file) {
        List<String> phoneNumbers = new ArrayList<>();
        
        try {
            if (file.getOriginalFilename() != null && file.getOriginalFilename().toLowerCase().endsWith(".csv")) {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(file.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] cols = line.split(",");
                        // Scan across the first 5 columns to find the phone number
                        for (String col : cols) {
                            String cleanedNumber = col.replaceAll("[^0-9]", "");
                            // A real phone number is usually between 10 to 15 digits!
                            if (cleanedNumber.length() >= 10 && cleanedNumber.length() <= 15) {
                                phoneNumbers.add(cleanedNumber);
                                break; // Found it for this row! Move to next row.
                            }
                        }
                    }
                }
                log.info("Successfully extracted {} numbers from the CSV file!", phoneNumbers.size());
                return phoneNumbers;
            }

            // Otherwise, boot up Apache POI to parse binary .xlsx / .xls files
            try (InputStream is = file.getInputStream();
                 Workbook workbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(is)) {
                
                log.info("Starting to read Excel workbook: {}", file.getOriginalFilename());
                
                Sheet sheet = workbook.getSheetAt(0);
                org.apache.poi.ss.usermodel.DataFormatter dataFormatter = new org.apache.poi.ss.usermodel.DataFormatter();
                
                for (Row row : sheet) {
                    if (row == null) continue;
                    
                    // Scan the first 5 columns across the row to intelligently find the phone number
                    for (int i = 0; i < 5; i++) {
                        org.apache.poi.ss.usermodel.Cell cell = row.getCell(i);
                        if (cell == null) continue;
                        
                        String rawNumber = "";
                        if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC) {
                            // Automatically blocks scientific notation data loss!
                            rawNumber = java.math.BigDecimal.valueOf(cell.getNumericCellValue()).toPlainString();
                        } else {
                            rawNumber = dataFormatter.formatCellValue(cell);
                        }
                        
                        String cleanedNumber = rawNumber.replaceAll("[^0-9]", "");
                        
                        // Valid Phone Numbers typically fall in the 10-15 character range.
                        if (cleanedNumber.length() >= 10 && cleanedNumber.length() <= 15) {
                            phoneNumbers.add(cleanedNumber);
                            break; // We found the phone number for this row, stop scanning other columns!
                        }
                    }
                }
                
                log.info("Successfully extracted {} numbers from the Excel file!", phoneNumbers.size());
            }

        } catch (Exception e) {
            log.error("Failed to parse document", e);
            throw new RuntimeException("Could not read the document. Make sure it's a valid .xlsx or .csv file and the numbers are in the first column.");
        }
        
        return phoneNumbers;
    }
}
