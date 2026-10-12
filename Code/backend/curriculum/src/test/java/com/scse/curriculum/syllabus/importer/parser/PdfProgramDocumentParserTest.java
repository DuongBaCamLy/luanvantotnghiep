package com.scse.curriculum.syllabus.importer.parser;

import com.scse.curriculum.syllabus.importer.dto.SyllabusImportData;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageFitDestination;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PdfProgramDocumentParserTest {
    @Test void snapshotsKeepVisiblePagesAndLocalLinksWithoutEmbeddingTheSourcePageTree() throws Exception {
        try(var source=new PDDocument()) {
            for(int index=0;index<3;index++) {
                var page=new PDPage();source.addPage(page);
                try(var stream=new PDPageContentStream(source,page)) {
                    stream.beginText();stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA),12);
                    stream.newLineAtOffset(50,700);stream.showText("Source page "+(index+1));stream.endText();
                    if(index==2) {
                        var image=new BufferedImage(600,600,BufferedImage.TYPE_INT_RGB);var random=new Random(2027);
                        for(int y=0;y<600;y++)for(int x=0;x<600;x++)image.setRGB(x,y,random.nextInt());
                        stream.drawImage(LosslessFactory.createFromImage(source,image),50,100,400,400);
                    }
                }
            }
            var local=link();var destination=new PDPageFitDestination();destination.setPage(source.getPage(1));
            local.setDestination(destination);local.setPage(source.getPage(0));source.getPage(0).getAnnotations().add(local);
            var outside=link();var outsideDestination=new PDPageFitDestination();outsideDestination.setPage(source.getPage(2));
            outside.setDestination(outsideDestination);source.getPage(0).getAnnotations().add(outside);
            var external=link();var uri=new PDActionURI();uri.setURI("https://example.com/source");external.setAction(uri);
            source.getPage(1).getAnnotations().add(external);
            byte[] sourceBytes;try(var output=new ByteArrayOutputStream()){source.save(output);sourceBytes=output.toByteArray();}
            byte[] originalBytes=sourceBytes.clone();var parser=mock(SyllabusPdfParser.class);var data=new SyllabusImportData();
            var issues=List.<com.scse.curriculum.syllabus.importer.dto.SyllabusImportIssue>of();
            when(parser.parse2027PdfBatchWithMetadata(any())).thenReturn(new SyllabusPdfParser.ParsedPdfBatch(3,
                    List.of(new SyllabusPdfParser.ParsedSyllabusSection(1,2,data,issues))));
            var result=new PdfProgramDocumentParser(parser).parseFor2027(sourceBytes);
            assertEquals(3,result.unitCount());assertEquals(1,result.items().size());
            var item=result.items().get(0);assertSame(data,item.data());assertSame(issues,item.issues());
            assertEquals(1,item.startBoundary());assertEquals(2,item.endBoundary());assertArrayEquals(originalBytes,sourceBytes);
            assertTrue(item.sourceSnapshot().length<sourceBytes.length/4,"Unselected source image was embedded");
            try(var snapshot=Loader.loadPDF(item.sourceSnapshot())) {
                assertEquals(2,snapshot.getNumberOfPages());
                var originalText=new PDFTextStripper();originalText.setStartPage(1);originalText.setEndPage(2);
                assertEquals(originalText.getText(source),new PDFTextStripper().getText(snapshot));
                var copiedLink=(PDAnnotationLink)snapshot.getPage(0).getAnnotations().get(0);
                assertSame(snapshot.getPage(1).getCOSObject(),
                        ((PDPageFitDestination)copiedLink.getDestination()).getPage().getCOSObject());
                assertEquals("https://example.com/source",((PDActionURI)
                        ((PDAnnotationLink)snapshot.getPage(1).getAnnotations().get(0)).getAction()).getURI());
                var originalRenderer=new PDFRenderer(source);var copiedRenderer=new PDFRenderer(snapshot);
                for(int page=0;page<2;page++) {
                    var expected=originalRenderer.renderImageWithDPI(page,72);var actual=copiedRenderer.renderImageWithDPI(page,72);
                    assertEquals(expected.getWidth(),actual.getWidth());assertEquals(expected.getHeight(),actual.getHeight());
                    assertArrayEquals(expected.getRGB(0,0,expected.getWidth(),expected.getHeight(),null,0,expected.getWidth()),
                            actual.getRGB(0,0,actual.getWidth(),actual.getHeight(),null,0,actual.getWidth()));
                }
            }
            verify(parser).parse2027PdfBatchWithMetadata(any());verifyNoMoreInteractions(parser);
        }
    }
    @Test void legacyPreviewKeepsItsExistingParserAndDoesNotCreateSnapshots() throws Exception {
        var parser=mock(SyllabusPdfParser.class);var data=new SyllabusImportData();
        when(parser.parsePdfBatchWithMetadata(any())).thenReturn(new SyllabusPdfParser.ParsedPdfBatch(9,
                List.of(new SyllabusPdfParser.ParsedSyllabusSection(2,5,data,List.of()))));
        var result=new PdfProgramDocumentParser(parser).parse(new byte[0]);
        assertEquals(9,result.unitCount());assertSame(data,result.items().get(0).data());assertNull(result.items().get(0).sourceSnapshot());
        verify(parser).parsePdfBatchWithMetadata(any());verifyNoMoreInteractions(parser);
    }
    private static PDAnnotationLink link() {
        var link=new PDAnnotationLink();link.setRectangle(new PDRectangle(10,10,10,10));return link;
    }
}
