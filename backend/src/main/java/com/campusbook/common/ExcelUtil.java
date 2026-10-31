package com.campusbook.common;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 极简 xlsx 生成器。
 * 导出的表只有文字和数字，没必要引一整个 Apache POI（那会多出十几 MB 依赖）。
 * xlsx 本质就是一个 zip 包着几份 XML，这里手写最小可用的那几份：
 *   [Content_Types].xml / _rels/.rels / xl/workbook.xml
 *   xl/_rels/workbook.xml.rels / xl/worksheets/sheet1.xml
 * 单元格统一用 inlineStr（内联字符串），省掉 sharedStrings 那层间接。
 */
public final class ExcelUtil {

    private ExcelUtil() {
    }

    public static byte[] build(String sheetName, String[] headers, List<String[]> rows) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(bos)) {

            put(zip, "[Content_Types].xml", contentTypes());
            put(zip, "_rels/.rels", rootRels());
            put(zip, "xl/workbook.xml", workbook(sheetName));
            put(zip, "xl/_rels/workbook.xml.rels", workbookRels());
            put(zip, "xl/worksheets/sheet1.xml", sheet(headers, rows));

            zip.finish();
            return bos.toByteArray();
        } catch (Exception e) {
            throw new BizException("导出失败：" + e.getMessage());
        }
    }

    private static void put(ZipOutputStream zip, String name, String body) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(body.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static String contentTypes() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
                + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                + "</Types>";
    }

    private static String rootRels() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
                + "</Relationships>";
    }

    private static String workbookRels() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
                + "</Relationships>";
    }

    private static String workbook(String sheetName) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\""
                + " xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
                + "<sheets><sheet name=\"" + esc(sheetName) + "\" sheetId=\"1\" r:id=\"rId1\"/></sheets>"
                + "</workbook>";
    }

    private static String sheet(String[] headers, List<String[]> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        sb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">");

        // 列宽按内容估算：中文按 2 个字符宽，再夹在 8~40 之间
        sb.append("<cols>");
        for (int i = 0; i < headers.length; i++) {
            int width = displayWidth(headers[i]);
            for (String[] line : rows) {
                if (i < line.length) width = Math.max(width, displayWidth(line[i]));
            }
            int col = Math.min(40, Math.max(8, width + 4));
            sb.append("<col min=\"").append(i + 1).append("\" max=\"").append(i + 1)
                    .append("\" width=\"").append(col).append("\" customWidth=\"1\"/>");
        }
        sb.append("</cols><sheetData>");

        sb.append(row(1, headers));
        int r = 2;
        for (String[] line : rows) {
            sb.append(row(r++, line));
        }

        sb.append("</sheetData></worksheet>");
        return sb.toString();
    }

    private static String row(int r, String[] cells) {
        StringBuilder sb = new StringBuilder("<row r=\"" + r + "\">");
        for (int i = 0; i < cells.length; i++) {
            String v = cells[i] == null ? "" : cells[i];
            String ref = colName(i) + r;
            boolean numeric = v.matches("-?\\d+(\\.\\d+)?") && v.length() < 15;
            if (numeric) {
                sb.append("<c r=\"").append(ref).append("\"><v>").append(v).append("</v></c>");
            } else {
                sb.append("<c r=\"").append(ref).append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
                        .append(esc(v)).append("</t></is></c>");
            }
        }
        sb.append("</row>");
        return sb.toString();
    }

    /** 估算显示宽度：ASCII 算 1，其余（中文、全角）算 2 */
    private static int displayWidth(String s) {
        if (s == null) return 0;
        int w = 0;
        for (int i = 0; i < s.length(); i++) {
            w += s.charAt(i) < 128 ? 1 : 2;
        }
        return w;
    }

    /** 0 -> A, 25 -> Z, 26 -> AA */
    private static String colName(int index) {
        StringBuilder sb = new StringBuilder();
        int n = index;
        while (n >= 0) {
            sb.insert(0, (char) ('A' + n % 26));
            n = n / 26 - 1;
        }
        return sb.toString();
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
