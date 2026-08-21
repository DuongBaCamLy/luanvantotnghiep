package com.scse.curriculum.syllabus.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfGState;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;

public class SyllabusPdfPageEvent extends PdfPageEventHelper {

    private static final Color MUTED = new Color(100, 116, 139);
    private static final Color BORDER = new Color(203, 213, 225);

    private final SyllabusPdfFontProvider fonts;
    private final String courseCode;
    private final String versionLabel;
    private final String watermark;
    private PdfTemplate totalPages;

    public SyllabusPdfPageEvent(
            SyllabusPdfFontProvider fonts,
            String courseCode,
            String versionLabel,
            String watermark) {
        this.fonts = fonts;
        this.courseCode = safe(courseCode);
        this.versionLabel = safe(versionLabel);
        this.watermark = watermark;
    }

    @Override
    public void onOpenDocument(PdfWriter writer, Document document) {
        totalPages = writer.getDirectContent().createTemplate(32, 12);
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
        PdfContentByte canvas = writer.getDirectContent();
        Rectangle page = document.getPageSize();

        canvas.saveState();
        canvas.setColorStroke(BORDER);
        canvas.setLineWidth(0.5f);
        canvas.moveTo(document.leftMargin(), page.getHeight() - 44);
        canvas.lineTo(page.getWidth() - document.rightMargin(), page.getHeight() - 44);
        canvas.stroke();
        canvas.restoreState();

        Font headerFont = fonts.regular(7.5f, MUTED);
        ColumnText.showTextAligned(
                canvas,
                Element.ALIGN_LEFT,
                new Phrase("SCSE - COURSE SYLLABUS", headerFont),
                document.leftMargin(),
                page.getHeight() - 36,
                0);
        ColumnText.showTextAligned(
                canvas,
                Element.ALIGN_RIGHT,
                new Phrase(courseCode + "  |  " + versionLabel, headerFont),
                page.getWidth() - document.rightMargin(),
                page.getHeight() - 36,
                0);

        canvas.saveState();
        canvas.setColorStroke(BORDER);
        canvas.setLineWidth(0.5f);
        canvas.moveTo(document.leftMargin(), 38);
        canvas.lineTo(page.getWidth() - document.rightMargin(), 38);
        canvas.stroke();
        canvas.restoreState();

        Font footerFont = fonts.regular(7.5f, MUTED);
        ColumnText.showTextAligned(
                canvas,
                Element.ALIGN_LEFT,
                new Phrase("International University - Vietnam National University HCMC", footerFont),
                document.leftMargin(),
                26,
                0);

        String pageText = "Page " + writer.getPageNumber() + " / ";
        float rightX = page.getWidth() - document.rightMargin();
        ColumnText.showTextAligned(
                canvas,
                Element.ALIGN_RIGHT,
                new Phrase(pageText, footerFont),
                rightX - 16,
                26,
                0);
        canvas.addTemplate(totalPages, rightX - 16, 26);

        if (watermark != null && !watermark.isBlank()) {
            PdfGState state = new PdfGState();
            state.setFillOpacity(0.075f);
            canvas.saveState();
            canvas.setGState(state);
            canvas.beginText();
            canvas.setColorFill(new Color(190, 24, 93));
            canvas.setFontAndSize(fonts.regularBaseFont(), 46);
            canvas.showTextAligned(
                    Element.ALIGN_CENTER,
                    watermark,
                    page.getWidth() / 2,
                    page.getHeight() / 2,
                    38);
            canvas.endText();
            canvas.restoreState();
        }
    }

    @Override
    public void onCloseDocument(PdfWriter writer, Document document) {
        totalPages.beginText();
        totalPages.setFontAndSize(fonts.regularBaseFont(), 7.5f);
        totalPages.setColorFill(MUTED);
        totalPages.setTextMatrix(0, 0);
        totalPages.showText(String.valueOf(Math.max(1, writer.getPageNumber() - 1)));
        totalPages.endText();
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }
}
