package org.docx4j.convert.in.xhtml;

/**
 * @author Terrence
 */
public class HtmlToDocConfig {
    // px
    public static final int DEFAULT_FONT_SIZE = 14;

    /**
     * Font
     */
    public static final String SONGTI = "宋体";
    public static final String HEITI = "黑体";
    public static final String FANGSONG = "仿宋";
    public static final String KAITI = "楷体";
    public static final String LISHU = "隶书";
    public static final String YOUYUAN = "幼圆";
    public static final String YAHEI = "微软雅黑";
    public static final String[] FONTS_ZH_CN = new String[] { SONGTI, HEITI, FANGSONG, KAITI, LISHU, YOUYUAN, YAHEI };

    /**
     *
     */
    public static final String PARAGRAPH_SPACING_BEFORE_POUNDS = "docx4j-ImportXHTML.Element.P.BeforePounds";
    public static final String PARAGRAPH_SPACING_AFTER_POUNDS = "docx4j-ImportXHTML.Element.P.AfterPounds";
    public static final String PARAGRAPH_SPACING_BEFORE_LINES = "docx4j-ImportXHTML.Element.P.BeforeLines";
    public static final String PARAGRAPH_SPACING_AFTER_LINES = "docx4j-ImportXHTML.Element.P.AfterLines";
    public static final String PARAGRAPH_SPACING_LINE_VALUE = "docx4j-ImportXHTML.Element.P.LineValue";
}
