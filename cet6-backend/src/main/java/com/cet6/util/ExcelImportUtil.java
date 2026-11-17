package com.cet6.util;

import com.cet6.common.GlobalExceptionHandler.BusinessException;
import com.cet6.entity.Cet6Word;
import com.cet6.entity.Cet6Question;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ExcelImportUtil {

    private ExcelImportUtil() {}

    public static List<Cet6Word> parseWords(MultipartFile file, Long bookId) {
        List<Cet6Word> list = new ArrayList<>();
        try (InputStream is = file.getInputStream(); Workbook wb = new XSSFWorkbook(is)) {
            Sheet sheet = wb.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                Cet6Word word = new Cet6Word();
                word.setBookId(bookId);
                word.setWord(getCellStr(row.getCell(0)));
                word.setPhonetic(getCellStr(row.getCell(1)));
                word.setSpeech(getCellStr(row.getCell(2)));
                word.setTranslate(getCellStr(row.getCell(3)));
                word.setSentence(getCellStr(row.getCell(4)));
                word.setSort(i);
                word.setStatus(1);
                if (!word.getWord().isBlank()) {
                    list.add(word);
                }
            }
        } catch (Exception e) {
            throw new BusinessException("Excel解析失败：" + e.getMessage());
        }
        return list;
    }

    public static List<Cet6Question> parseQuestions(MultipartFile file, Long paperId) {
        List<Cet6Question> list = new ArrayList<>();
        try (InputStream is = file.getInputStream(); Workbook wb = new XSSFWorkbook(is)) {
            Sheet sheet = wb.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                Cet6Question q = new Cet6Question();
                q.setPaperId(paperId);
                q.setQuestionType((int) getCellNum(row.getCell(0), 2));
                q.setSubType((int) getCellNum(row.getCell(1), 201));
                q.setDifficult((int) getCellNum(row.getCell(2), 2));
                q.setTitle(getCellStr(row.getCell(3)));
                q.setOptions(getCellStr(row.getCell(4)));
                q.setStandardAnswer(getCellStr(row.getCell(5)));
                q.setAnalysis(getCellStr(row.getCell(6)));
                q.setSort(i);
                q.setStatus(1);
                if (!q.getTitle().isBlank()) {
                    list.add(q);
                }
            }
        } catch (Exception e) {
            throw new BusinessException("Excel解析失败：" + e.getMessage());
        }
        return list;
    }

    private static String getCellStr(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            default -> "";
        };
    }

    private static double getCellNum(Cell cell, double defaultVal) {
        if (cell == null) return defaultVal;
        if (cell.getCellType() == CellType.NUMERIC) return cell.getNumericCellValue();
        try {
            return Double.parseDouble(getCellStr(cell));
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }
}
