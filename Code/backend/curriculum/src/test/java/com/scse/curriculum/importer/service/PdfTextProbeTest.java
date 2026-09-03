package com.scse.curriculum.importer.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

class PdfTextProbeTest {

    @Test
    void printPdfBoxText() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/syllabus-import/CS-IT116.pdf")) {
            if (input == null) {
                throw new IllegalStateException("fixture missing");
            }
            try (PDDocument document = Loader.loadPDF(input.readAllBytes())) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true);
                System.out.println(stripper.getText(document));
            }
        }
    }
}
