package com.sdu.dorm_system.service;

import com.sdu.dorm_system.domain.UserAccount;
import com.sdu.dorm_system.domain.enums.Gender;
import com.sdu.dorm_system.exception.BusinessException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class StudentImportService {

    private final UserManagementService userManagementService;

    @Transactional
    public List<UserAccount> importStudents(MultipartFile file, UserAccount actor) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest("An Excel file is required");
        }

        Gender forcedGender = actor.getRole().isGenderAdmin() ? actor.getRole().managedGender() : null;
        List<UserAccount> importedStudents = new ArrayList<>();

        try (InputStream inputStream = file.getInputStream(); Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw BusinessException.badRequest("The Excel file must contain a header row");
            }

            Map<String, Integer> headerIndexes = readHeaders(headerRow);
            requireHeader(headerIndexes, "name");
            requireHeader(headerIndexes, "surname");
            requireHeader(headerIndexes, "id");
            requireHeader(headerIndexes, "email");

            if (forcedGender == null) {
                requireHeader(headerIndexes, "gender");
            }

            DataFormatter formatter = new DataFormatter();
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || isBlankRow(row, formatter)) {
                    continue;
                }

                Gender gender = forcedGender != null
                    ? forcedGender
                    : Gender.fromText(readValue(row, headerIndexes.get("gender"), formatter));

                UserManagementService.StudentUpsertCommand command = new UserManagementService.StudentUpsertCommand(
                    readValue(row, headerIndexes.get("name"), formatter),
                    readValue(row, headerIndexes.get("surname"), formatter),
                    readValue(row, headerIndexes.get("email"), formatter),
                    readValue(row, headerIndexes.get("id"), formatter),
                    gender
                );

                importedStudents.add(userManagementService.createStudent(command, actor));
            }
        } catch (IOException exception) {
            throw BusinessException.badRequest("The uploaded Excel file could not be read");
        }

        return importedStudents;
    }

    private Map<String, Integer> readHeaders(Row headerRow) {
        Map<String, Integer> headerIndexes = new HashMap<>();
        DataFormatter formatter = new DataFormatter();

        for (Cell cell : headerRow) {
            String normalizedHeader = formatter.formatCellValue(cell).trim().toLowerCase(Locale.ROOT);
            headerIndexes.put(normalizedHeader, cell.getColumnIndex());
        }

        return headerIndexes;
    }

    private void requireHeader(Map<String, Integer> headerIndexes, String headerName) {
        if (!headerIndexes.containsKey(headerName)) {
            throw BusinessException.badRequest("Missing required column: " + headerName);
        }
    }

    private String readValue(Row row, Integer columnIndex, DataFormatter formatter) {
        if (columnIndex == null) {
            return null;
        }

        Cell cell = row.getCell(columnIndex);
        String value = cell == null ? "" : formatter.formatCellValue(cell);

        if (value == null || value.isBlank()) {
            throw BusinessException.badRequest("Excel row " + (row.getRowNum() + 1) + " contains an empty required value");
        }

        return value.trim();
    }

    private boolean isBlankRow(Row row, DataFormatter formatter) {
        for (Cell cell : row) {
            if (!formatter.formatCellValue(cell).isBlank()) {
                return false;
            }
        }

        return true;
    }
}
