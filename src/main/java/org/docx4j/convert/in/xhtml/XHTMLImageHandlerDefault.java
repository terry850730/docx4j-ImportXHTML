package org.docx4j.convert.in.xhtml;

import org.apache.commons.codec.binary.Base64;
import org.docx4j.dml.wordprocessingDrawing.Inline;
import org.docx4j.jaxb.Context;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.Part;
import org.docx4j.openpackaging.parts.WordprocessingML.BinaryPartAbstractImage;
import org.docx4j.org.xhtmlrenderer.docx.Docx4JFSImage;
import org.docx4j.org.xhtmlrenderer.docx.Docx4jUserAgent;
import org.docx4j.wml.CTTblCellMar;
import org.docx4j.wml.CTTblPrBase;
import org.docx4j.wml.P;
import org.docx4j.wml.R;
import org.docx4j.wml.Style;
import org.docx4j.wml.Style.BasedOn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.text.MessageFormat;
import java.util.HashMap;

public class XHTMLImageHandlerDefault implements XHTMLImageHandler {

    private static Logger log = LoggerFactory.getLogger(XHTMLImageHandlerDefault.class);

    private static final String SRC = "src";
    private static final String DATA_IMAGE = "data:image";
    private static final String ALT = "alt";

    private int maxWidth = -1;
    private String tableStyle;
    private Part sourcePart;

    private HashMap<String, BinaryPartAbstractImage> imagePartCache = new HashMap<String, BinaryPartAbstractImage>();

    private XHTMLImporterImpl importer;

    public XHTMLImageHandlerDefault(XHTMLImporterImpl importer) {
        this.importer = importer;
    }

    public void addImage(Docx4jUserAgent docx4jUserAgent, WordprocessingMLPackage wordMLPackage, P p, Element e, Long cx, Long cy) {
        BinaryPartAbstractImage imagePart = null;
        boolean isError = false;
        try {
            byte[] imageBytes = null;
            String src = e.getAttribute(SRC);
            if (src.startsWith(DATA_IMAGE)) {
                /**
                 * Supports
                 *     data:[<MIME-type>][;charset=<encoding>][;base64],<data>
                 *     eg data:image/png;base64,iVBORw0KGgo...
                 * http://www.greywyvern.com/code/php/binary2base64 is a convenient online encoder
                 */
                String base64String = src;
                int commaPos = base64String.indexOf(",");
                if (commaPos < 6) {
                    // .. its broken
                    writeError(p, "[INVALID DATA URI: " + src);
                    return;
                }
                base64String = base64String.substring(commaPos + 1);
                log.debug(base64String);
                imageBytes = Base64.decodeBase64(base64String.getBytes("UTF8"));
            } else {
                imagePart = imagePartCache.get(src);
                if (imagePart == null) {
                    imageBytes = readImageFromUrl(docx4jUserAgent, src);
                }
            }
            if (imageBytes == null && imagePart == null) {
                isError = true;
            } else {
                if (imagePart == null) {
                    if (sourcePart == null) {
                        imagePart = BinaryPartAbstractImage.createImagePart(wordMLPackage, imageBytes);
                    } else {
                        imagePart = BinaryPartAbstractImage.createImagePart(wordMLPackage, sourcePart, imageBytes);
                    }
                    if (!src.startsWith(DATA_IMAGE)) {
                        // cache it
                        imagePartCache.put(src, imagePart);
                    }
                }

                Inline inline = null;
                if (cx == null && cy == null) {
                    if (maxWidth > 0) {
                        log.debug("image maxWidth:" + maxWidth + ", table style: " + tableStyle);
                        long excessWidth = getTblCellMargins(tableStyle);
                        if (excessWidth > 0) {
                            log.debug("table style margins subtracted (twips): " + excessWidth);
                        }
                        inline = imagePart.createImageInline(null, e.getAttribute(ALT), 0, 1, false, maxWidth - (int) excessWidth);
                    } else {
                        inline = imagePart.createImageInline(null, e.getAttribute(ALT), 0, 1, false);
                    }
                } else {
                    if (cx == null) {
                        cx = imagePart.getImageInfo().getSize().getWidthPx() * (cy / imagePart.getImageInfo().getSize().getHeightPx());

                    } else if (cy == null) {
                        cy = imagePart.getImageInfo().getSize().getHeightPx() * (cx / imagePart.getImageInfo().getSize().getWidthPx());

                    }
                    inline = imagePart.createImageInline(null, e.getAttribute(ALT), 0, 1, cx, cy, false);

					/*
                     * That sets text wrapping distance from text to 0.
					 *
					 *   <wp:anchor distT="457200" distB="118745" distL="457200"  distR="0"
					 *
					 * would set distance from text top 0.5", bottom 0.13", left 0.5", right 0.
					 *
					 */
                }

                // Now add the inline in w:p/w:r/w:drawing
                R run = getRun(p);
                org.docx4j.wml.Drawing drawing = Context.getWmlObjectFactory().createDrawing();
                run.getContent().add(drawing);
                drawing.getAnchorOrInline().add(inline);
            }
        } catch (Exception ex) {
            log.error(MessageFormat.format("Error during image processing: ''{0}'', insert default text.", new Object[] { e.getAttribute(ALT) }), ex);
            isError = true;
        }

        if (isError) {
            writeError(p, "[MISSING IMAGE: " + e.getAttribute(ALT) + ", " + e.getAttribute(ALT) + " ]");
        }
    }

    private R getRun(P p) {
        R run = null;
        if (p.getContent().size() > 0) {
            Object o = p.getContent().get(p.getContent().size() - 1);
            if (o instanceof R) {
                run = (R) o;
            }
        }
        if (run == null) {
            run = Context.getWmlObjectFactory().createR();
            p.getContent().add(run);
        }
        return run;
    }

    private byte[] readImageFromUrl(Docx4jUserAgent docx4jUserAgent, String url) {
        // Workaround for cannot resolve the URL C:\... with base URL file:/C:/...
        // where @src points to a raw file path
        if (url.substring(1, 2).equals(":")) {
            url = "file:/" + url;
        }

        Docx4JFSImage docx4JFSImage = docx4jUserAgent.getDocx4JImageResource(url);
        // in case of wrong URL - docx4JFSImage will be null
        return docx4JFSImage != null ? docx4JFSImage.getBytes() : null;
    }

    private void writeError(P p, String src) {
        org.docx4j.wml.R run = Context.getWmlObjectFactory().createR();
        p.getContent().add(run);

        org.docx4j.wml.Text text = Context.getWmlObjectFactory().createText();
        text.setValue(src);

        run.getContent().add(text);
    }

    /**
     * Get table cell margins from table style.
     * <br>Parameter tableStyle can be null - 0 will be returned.
     *
     * @return left margin plus right margin (twips)
     */
    private long getTblCellMargins(String tableStyle) {
        Style s = null;
        if (tableStyle != null && !tableStyle.isEmpty()) {
            s = importer.getStyleByIdOrName(tableStyle);
        }
        if (s != null && importer.getTableHelper().isTableStyle(s)) {
            CTTblCellMar cellMar = getTblCellMar(s);
            if (cellMar == null) {
                //try "based on" style
                CTTblCellMar bsCellMar = getBasedOnTblCellMar(s);
                if (bsCellMar != null) {
                    return getLeftPlusRightMarginsValue(bsCellMar);
                }
            } else {
                return getLeftPlusRightMarginsValue(cellMar);
            }
        }
        return 0;
    }

    private long getLeftPlusRightMarginsValue(CTTblCellMar cellMar) {
        return cellMar.getLeft().getW().longValue() + cellMar.getRight().getW().longValue();
    }

    /**
     * Get cell margins from "based on" style.
     * <br>Search recursively while possible.
     */
    private CTTblCellMar getBasedOnTblCellMar(Style s) {
        BasedOn bo = s.getBasedOn();
        if (bo != null) {
            String basedOn = bo.getVal();
            if (basedOn != null && !basedOn.isEmpty()) {
                Style bs = importer.getStyleByIdOrName(basedOn);
                if (bs != null) {
                    CTTblCellMar bsCellMar = getTblCellMar(bs);
                    if (bsCellMar != null) {
                        return bsCellMar;
                    } else {
                        return getBasedOnTblCellMar(bs);
                    }
                }
            }
        }
        return null;
    }

    private CTTblCellMar getTblCellMar(Style s) {
        CTTblPrBase tpb = s.getTblPr();
        if (tpb != null) {
            return tpb.getTblCellMar();
        }
        return null;
    }

    public int getMaxWidth() {
        return maxWidth;
    }

    @Override
    public void setMaxWidth(int maxWidth, String tableStyle) {
        this.maxWidth = maxWidth;
        this.tableStyle = tableStyle;
    }

    public Part getSourcePart() {
        return sourcePart;
    }

    public void setSourcePart(Part sourcePart) {
        this.sourcePart = sourcePart;
    }
}
