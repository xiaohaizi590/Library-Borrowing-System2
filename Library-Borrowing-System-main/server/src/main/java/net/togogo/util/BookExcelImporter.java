package net.togogo.util;

import net.togogo.dto.CreateBookRequest;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.*;

//解析execl文件，按表头映射成图书数据
public class BookExcelImporter {

    /**
     * 解析 Excel 文件，返回图书请求列表
     */
    public static List<CreateBookRequest> parse(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            return parseBooks(is);
        } catch (IOException e) {
            throw new RuntimeException("Excel 解析失败: " + e.getMessage(), e);
        }
    }

    public static List<CreateBookRequest> parse(InputStream is) {
        return parseBooks(is);
    }

    /**
     * 解析 Excel 输入流，返回图书请求列表（仅包含书名和作者均非空的记录）
     */
    private static List<CreateBookRequest> parseBooks(InputStream inputStream) {
        List<CreateBookRequest> books = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(inputStream)) { // 支持 .xlsx
            Sheet sheet = workbook.getSheetAt(0); // 取第一个工作表

            // 1. 读取表头行（第一行），建立列索引 → 字段名的映射
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new RuntimeException("Excel 表头行不存在");
            }
            Map<Integer, String> columnToField = new HashMap<>();
            for (Cell cell : headerRow) {
                String headerText = getCellStringValue(cell);
                if (headerText != null && !headerText.isEmpty()) {
                    String fieldName = HEADER_MAP.get(headerText.trim());
                    if (fieldName != null) {
                        columnToField.put(cell.getColumnIndex(), fieldName);
                    }
                }
            }

            // 2. 遍历数据行（从第2行开始）
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row, columnToField.keySet())) {
                    continue; // 跳过空行
                }

                CreateBookRequest req = new CreateBookRequest();
                // 3. 根据列映射填充 CreateBookRequest 对象
                for (Map.Entry<Integer, String> entry : columnToField.entrySet()) {
                    Cell cell = row.getCell(entry.getKey());
                    String value = getCellStringValue(cell);
                    if (value != null) {
                        setFieldValue(req, entry.getValue(), value);
                    }
                }

                // 4. 必填校验：书名和作者非空
                if (req.getTitle() != null && !req.getTitle().isEmpty()
                        && req.getAuthor() != null && !req.getAuthor().isEmpty()) {
                    books.add(req);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Excel 解析失败: " + e.getMessage(), e);
        }
        return books;
    }

    // ========== 辅助方法 ==========

    /**
     * 安全获取单元格的字符串值（支持多种类型）
     */
    private static String getCellStringValue(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                double val = cell.getNumericCellValue();
                // 整数去掉多余的小数位
                if (val == Math.floor(val)) {
                    return String.valueOf((long) val);
                }
                return String.valueOf(val);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue().trim();
                } catch (IllegalStateException e) {
                    return String.valueOf(cell.getNumericCellValue());
                }
            default:
                return null;
        }
    }

    /**
     * 判断某行是否全为空（基于所有有映射的列）
     */
    private static boolean isRowEmpty(Row row, Set<Integer> columnIndexes) {
        for (int colIndex : columnIndexes) {
            Cell cell = row.getCell(colIndex);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String val = getCellStringValue(cell);
                if (val != null && !val.isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 通过反射将字符串值设置到实体对象的指定字段（支持基本类型转换）
     */
    private static void setFieldValue(Object obj, String fieldName, String value) {
        if (value == null) return;
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Class<?> type = field.getType();

            if (type == String.class) {
                field.set(obj, value);
            } else if (type == Integer.class || type == int.class) {
                field.set(obj, Integer.valueOf(value));
            } else if (type == Long.class || type == long.class) {
                field.set(obj, Long.valueOf(value));
            } else if (type == Double.class || type == double.class) {
                field.set(obj, Double.valueOf(value));
            } else if (type == Boolean.class || type == boolean.class) {
                field.set(obj, Boolean.valueOf(value));
            } else if (type == java.math.BigDecimal.class) {
                field.set(obj, new java.math.BigDecimal(value));
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // 忽略不存在的字段
        }
    }

    // 预定义表头别名 → 字段名
    private static final Map<String, String> HEADER_MAP = Map.ofEntries(
        Map.entry("书名", "title"),
        Map.entry("title", "title"),
        Map.entry("图书名称", "title"),
        Map.entry("作者", "author"),
        Map.entry("author", "author"),
        Map.entry("isbn", "isbn"),
        Map.entry("ISBN", "isbn"),
        Map.entry("ISBN编号", "isbn"),
        Map.entry("出版社", "publisher"),
        Map.entry("publisher", "publisher"),
        Map.entry("分类", "category"),
        Map.entry("category", "category"),
        Map.entry("图书分类", "category"),
        Map.entry("库存", "stock"),
        Map.entry("stock", "stock"),
        Map.entry("库存数量", "stock"),
        Map.entry("描述", "description"),
        Map.entry("description", "description"),
        Map.entry("简介", "description")
    );
}
