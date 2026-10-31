package com.campusbook.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 导出工具的单测。
 * 这里不启动 Spring 容器——ExcelUtil 是纯函数，直接测最快也最稳。
 */
class ExcelUtilTest {

    private List<String> entries(byte[] data) throws Exception {
        List<String> names = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(data))) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) names.add(e.getName());
        }
        return names;
    }

    private String sheetXml(byte[] data) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(data))) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                if ("xl/worksheets/sheet1.xml".equals(e.getName())) {
                    return new String(zis.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }
        return "";
    }

    @Test
    @DisplayName("生成的文件是一个结构完整的 zip 包")
    void buildProducesValidPackage() throws Exception {
        byte[] data = ExcelUtil.build("测试", new String[]{"A", "B"},
                List.<String[]>of(new String[]{"1", "2"}));

        assertTrue(data.length > 0, "输出不能为空");
        List<String> names = entries(data);
        assertTrue(names.contains("[Content_Types].xml"));
        assertTrue(names.contains("xl/workbook.xml"));
        assertTrue(names.contains("xl/worksheets/sheet1.xml"));
        assertEquals(5, names.size(), "只需要这 5 个部件");
    }

    @Test
    @DisplayName("表头和数据行都写进了 sheet")
    void writesHeadersAndRows() throws Exception {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"张三", "计算机科学与技术"});
        rows.add(new String[]{"李四", "会计学"});

        String xml = sheetXml(ExcelUtil.build("用户", new String[]{"昵称", "专业"}, rows));

        assertTrue(xml.contains("昵称"));
        assertTrue(xml.contains("张三"));
        assertTrue(xml.contains("会计学"));
        assertEquals(3, countRows(xml), "表头 + 2 行数据");
    }

    @Test
    @DisplayName("XML 特殊字符会被转义，不会破坏文件结构")
    void escapesSpecialCharacters() throws Exception {
        String xml = sheetXml(ExcelUtil.build("转义", new String[]{"内容"},
                List.<String[]>of(new String[]{"A&B <test> \"quoted\""})));

        assertTrue(xml.contains("A&amp;B"));
        assertTrue(xml.contains("&lt;test&gt;"));
        assertTrue(xml.contains("&quot;quoted&quot;"));
        assertFalse(xml.contains("<test>"), "原始尖括号不应该出现");
    }

    @Test
    @DisplayName("纯数字写成数值单元格，带前导零的仍按文本处理")
    void detectsNumbers() throws Exception {
        String xml = sheetXml(ExcelUtil.build("数字", new String[]{"值", "编号"},
                List.<String[]>of(new String[]{"123", "000123"})));

        assertTrue(xml.contains("<v>123</v>"), "数字应该用 v 标签");
        assertTrue(xml.contains(">000123<"), "前导零必须保留为文本");
    }

    @Test
    @DisplayName("列名超过 26 列能正确进位成 AA")
    void handlesColumnOverflow() throws Exception {
        String[] headers = new String[30];
        for (int i = 0; i < headers.length; i++) headers[i] = "H" + i;

        String xml = sheetXml(ExcelUtil.build("宽表", headers, List.<String[]>of(headers)));

        assertTrue(xml.contains("r=\"Z1\""), "第 26 列是 Z");
        assertTrue(xml.contains("r=\"AA1\""), "第 27 列应进位为 AA");
        assertTrue(xml.contains("r=\"AD1\""), "第 30 列是 AD");
    }

    @Test
    @DisplayName("空数据也能生成合法文件")
    void handlesEmptyRows() throws Exception {
        byte[] data = ExcelUtil.build("空表", new String[]{"列一"}, List.of());

        assertEquals(5, entries(data).size());
        assertEquals(1, countRows(sheetXml(data)), "只有表头一行");
    }

    private int countRows(String xml) {
        int n = 0, i = 0;
        while ((i = xml.indexOf("<row ", i)) >= 0) {
            n++;
            i++;
        }
        return n;
    }
}