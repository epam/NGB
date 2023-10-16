/*
 * MIT License
 *
 * Copyright (c) 2023 EPAM Systems
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.epam.catgenome.manager.export;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public final class ExcelExportUtils {
    private ExcelExportUtils() {
        // no operations by default
    }

    public static InputStream export(final Workbook workbook) throws IOException, ParseException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            workbook.write(bos);
            final byte[] bytes = bos.toByteArray();
            return new ByteArrayInputStream(bytes);
        }
    }

    public static <T> void writeSheet(final String sheetName,
                                      final List<? extends ExportField<T>> exportFields,
                                      final List<T> entries,
                                      final Workbook workbook) {
        final Sheet sheet = workbook.createSheet(sheetName);
        fillHeader(exportFields, workbook, sheet);
        for (int i = 0; i < entries.size(); i++) {
            Row row = sheet.createRow(i + 1);
            T indexEntry = entries.get(i);
            for (int j = 0; j < exportFields.size(); j++) {
                String value = exportFields.get(j).getGetter().apply(indexEntry);
                Cell cell = row.createCell(j);
                cell.setCellValue(value);
            }
        }
    }

    private static <T> void fillHeader(final List<? extends ExportField<T>> exportFields,
                                       final Workbook workbook, final Sheet sheet) {
        final Row header = sheet.createRow(0);
        final CellStyle headerStyle = workbook.createCellStyle();
        final XSSFFont font = ((XSSFWorkbook) workbook).createFont();
        font.setBold(true);
        headerStyle.setFont(font);
        Cell headerCell;
        for (int i = 0; i < exportFields.size(); i++) {
            headerCell = header.createCell(i);
            headerCell.setCellValue(exportFields.get(i).getLabel());
            headerCell.setCellStyle(headerStyle);
            sheet.autoSizeColumn(i);
        }
    }
}
