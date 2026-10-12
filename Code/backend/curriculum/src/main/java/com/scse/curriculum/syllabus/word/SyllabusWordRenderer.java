package com.scse.curriculum.syllabus.word;

import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.scse.curriculum.syllabus.pdf.SyllabusPdfDocument;
import com.scse.curriculum.syllabus.pdf.SyllabusPdfMode;
import com.scse.curriculum.syllabus.pdf.SyllabusPdfRenderer;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;

/** Converts the shared syllabus layout to native OOXML, never page images. */
@Component
public class SyllabusWordRenderer {
    private final SyllabusPdfRenderer layout;

    public SyllabusWordRenderer(SyllabusPdfRenderer layout) { this.layout = layout; }

    public byte[] render(SyllabusPdfDocument data) {
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            styles(doc);
            doc.getProperties().getCoreProperties().setTitle("Course Syllabus - " + text(data.courseCode()));
            doc.getProperties().getCoreProperties().setCreator(text(data.createdBy()));
            Rectangle page = com.lowagie.text.PageSize.A4;
            for (var block : layout.layout(data, SyllabusPdfMode.EXPORT)) {
                if (page.getWidth() != block.pageSize().getWidth()) {
                    var section = doc.createParagraph().getCTP().addNewPPr().addNewSectPr();
                    geometry(section, page);
                    page = block.pageSize();
                }
                if (block.element() instanceof PdfPTable table) {
                    table(doc, table, Math.round((page.getWidth() - 76) * 20));
                } else if (block.element() instanceof Paragraph paragraph) {
                    paragraph(doc.createParagraph(), paragraph);
                }
            }
            CTSectPr section = doc.getDocument().getBody().isSetSectPr()
                    ? doc.getDocument().getBody().getSectPr() : doc.getDocument().getBody().addNewSectPr();
            geometry(section, page);
            XWPFParagraph header = doc.createHeader(HeaderFooterType.DEFAULT).createParagraph();
            header.setAlignment(ParagraphAlignment.RIGHT);
            XWPFRun heading = header.createRun();
            heading.setFontFamily("Arial"); heading.setFontSize(8);
            heading.setText(text(data.courseCode()) + " · "
                    + com.scse.curriculum.syllabus.entity.SyllabusVersion.display(data.versionNumber(), data.versionLabel())
                    + ("APPROVED".equals(data.status()) ? "" : " · " + text(data.status()).replace('_', ' ')));
            XWPFParagraph footer = doc.createFooter(HeaderFooterType.DEFAULT).createParagraph();
            footer.setAlignment(ParagraphAlignment.CENTER);
            footer.createRun().setText("Page ");
            footer.getCTP().addNewFldSimple().setInstr("PAGE");
            for (XWPFParagraph paragraph : doc.getParagraphs()) {
                if (paragraph.getCTP().isSetPPr() && paragraph.getCTP().getPPr().isSetSectPr()) {
                    var previous = paragraph.getCTP().getPPr().getSectPr();
                    previous.setHeaderReferenceArray(section.getHeaderReferenceArray());
                    previous.setFooterReferenceArray(section.getFooterReferenceArray());
                }
            }
            doc.write(out);
            return out.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot render the current Word document", exception);
        }
    }

    private void geometry(CTSectPr section, Rectangle page) {
        CTPageSz size = section.addNewPgSz();
        size.setW(BigInteger.valueOf(Math.round(page.getWidth() * 20)));
        size.setH(BigInteger.valueOf(Math.round(page.getHeight() * 20)));
        size.setOrient(page.getWidth() > page.getHeight() ? STPageOrientation.LANDSCAPE : STPageOrientation.PORTRAIT);
        CTPageMar margins = section.addNewPgMar();
        margins.setLeft(BigInteger.valueOf(760)); margins.setRight(BigInteger.valueOf(760));
        margins.setTop(BigInteger.valueOf(1000)); margins.setBottom(BigInteger.valueOf(880));
        margins.setHeader(BigInteger.valueOf(400)); margins.setFooter(BigInteger.valueOf(400));
        margins.setGutter(BigInteger.ZERO);
    }

    private void paragraph(XWPFParagraph target, Phrase source) {
        target.setSpacingAfter(60);
        if (source instanceof Paragraph p) {
            float fontSize = p.getFont().getSize();
            if (fontSize >= 18) target.setStyle("SyllabusTitle");
            else if (fontSize >= 10.8f) target.setStyle("SyllabusHeading1");
            else if (fontSize >= 9.2f && p.getFont().isBold()) target.setStyle("SyllabusHeading2");
            target.setSpacingBefore(Math.round(p.getSpacingBefore() * 20));
            target.setSpacingAfter(Math.round(p.getSpacingAfter() * 20));
            target.setKeepNext(p.getKeepTogether());
            target.setAlignment(alignment(p.getAlignment()));
        }
        for (var element : source.getChunks()) {
            if (!(element instanceof com.lowagie.text.Chunk chunk)) continue;
            XWPFRun run = target.createRun();
            var font = chunk.getFont();
            run.setFontFamily("Arial");
            run.setFontSize(font.getSize() > 0 ? font.getSize() : 8);
            run.setBold(font.isBold()); run.setItalic(font.isItalic());
            if (font.getColor() != null) run.setColor(color(font.getColor()));
            String[] lines = chunk.getContent().split("\\R", -1);
            for (int i = 0; i < lines.length; i++) {
                if (i > 0) run.addBreak();
                run.setText(lines[i]);
            }
        }
    }

    private void table(XWPFDocument doc, PdfPTable source, int width) {
        int columns = source.getNumberOfColumns();
        XWPFTable target = doc.createTable(1, columns);
        target.removeRow(0);
        target.setWidth(width);
        target.setInsideHBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, "E2E8F0");
        target.setInsideVBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, "E2E8F0");
        target.setTopBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, "E2E8F0");
        target.setBottomBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, "E2E8F0");
        target.setLeftBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, "E2E8F0");
        target.setRightBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, "E2E8F0");
        target.setCellMargins(76, 76, 76, 76);
        var properties = target.getCTTbl().getTblPr();
        properties.addNewTblLayout().setType(STTblLayoutType.FIXED);
        var grid = target.getCTTbl().getTblGrid();
        if (grid == null) grid = target.getCTTbl().addNewTblGrid();
        while (grid.sizeOfGridColArray() > 0) grid.removeGridCol(0);
        float[] relative = source.getAbsoluteWidths();
        float sum = 0;
        for (float value : relative) sum += value;
        int[] widths = new int[columns];
        int remaining = width;
        for (int c = 0; c < columns; c++) {
            widths[c] = c == columns - 1 ? remaining : Math.round(width * relative[c] / sum);
            remaining -= widths[c];
            grid.addNewGridCol().setW(BigInteger.valueOf(widths[c]));
        }
        int[] merges = new int[columns];
        int rowIndex = 0;
        for (var sourceRow : source.getRows()) {
            XWPFTableRow row = target.createRow();
            while (!row.getTableCells().isEmpty()) row.removeCell(0);
            if (rowIndex++ < source.getHeaderRows()) row.setRepeatHeader(true);
            PdfPCell[] cells = sourceRow.getCells();
            int minimumHeight = 0;
            for (PdfPCell sourceCell : cells) if (sourceCell != null)
                minimumHeight = Math.max(minimumHeight, Math.round(sourceCell.getMinimumHeight() * 20));
            if (minimumHeight > 0) {
                row.setHeight(minimumHeight);
                row.setHeightRule(TableRowHeightRule.AT_LEAST);
            }
            for (int c = 0; c < columns;) {
                PdfPCell sourceCell = cells[c];
                int span = sourceCell == null ? 1 : sourceCell.getColspan();
                XWPFTableCell cell = row.createCell();
                CTTcPr cp = cell.getCTTc().addNewTcPr();
                int cellWidth = 0;
                for (int j = c; j < c + span; j++) cellWidth += widths[j];
                CTTblWidth cw = cp.addNewTcW(); cw.setType(STTblWidth.DXA); cw.setW(BigInteger.valueOf(cellWidth));
                if (span > 1) cp.addNewGridSpan().setVal(BigInteger.valueOf(span));
                if (sourceCell != null && sourceCell.getRowspan() > 1) {
                    merges[c] = sourceCell.getRowspan() - 1;
                    cp.addNewVMerge().setVal(STMerge.RESTART);
                } else if (merges[c] > 0) {
                    cp.addNewVMerge().setVal(STMerge.CONTINUE); merges[c]--;
                }
                cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.TOP);
                if (sourceCell != null) {
                    if (sourceCell.getBackgroundColor() != null) cell.setColor(color(sourceCell.getBackgroundColor()));
                    var p = cell.getParagraphs().get(0);
                    p.setAlignment(alignment(sourceCell.getHorizontalAlignment()));
                    if (sourceCell.getPhrase() != null) paragraph(p, sourceCell.getPhrase());
                    if (sourceCell.getCompositeElements() != null) {
                        boolean first = true;
                        for (Object element : sourceCell.getCompositeElements()) {
                            if (element instanceof Phrase phrase) {
                                paragraph(first ? p : cell.addParagraph(), phrase); first = false;
                            }
                        }
                    }
                }
                c += span;
            }
        }
        doc.createParagraph().setSpacingAfter(80);
    }

    private static ParagraphAlignment alignment(int value) {
        return switch (value) {
            case Element.ALIGN_CENTER -> ParagraphAlignment.CENTER;
            case Element.ALIGN_RIGHT -> ParagraphAlignment.RIGHT;
            default -> ParagraphAlignment.LEFT;
        };
    }
    private void styles(XWPFDocument doc) {
        XWPFStyles styles = doc.createStyles();
        String[] ids = {"SyllabusTitle", "SyllabusHeading1", "SyllabusHeading2"};
        for (int i = 0; i < ids.length; i++) {
            CTStyle style = CTStyle.Factory.newInstance();
            style.setStyleId(ids[i]); style.setType(STStyleType.PARAGRAPH);
            style.addNewName().setVal(i == 0 ? "Syllabus Title" : "Syllabus Heading " + i);
            var pp = style.addNewPPr(); pp.addNewKeepNext();
            if (i > 0) pp.addNewOutlineLvl().setVal(BigInteger.valueOf(i - 1));
            styles.addStyle(new XWPFStyle(style));
        }
    }
    private static String text(String value) { return value == null ? "" : value; }
    private static String color(java.awt.Color value) { return String.format("%02X%02X%02X", value.getRed(), value.getGreen(), value.getBlue()); }
}
