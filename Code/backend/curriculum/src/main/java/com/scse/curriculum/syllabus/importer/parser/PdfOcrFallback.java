package com.scse.curriculum.syllabus.importer.parser;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

final class PdfOcrFallback {

    private static final float OCR_DPI = 250f;

    String extract(
            PDDocument document,
            int startPage,
            int endPage) throws IOException {

        String executable =
                System.getenv()
                        .getOrDefault(
                                "TESSERACT_COMMAND",
                                "tesseract");

        Path tempDirectory =
                Files.createTempDirectory(
                        "syllabus-ocr-");

        StringBuilder result =
                new StringBuilder();

        PDFRenderer renderer =
                new PDFRenderer(document);

        try {

            for (int page = startPage;
                 page <= endPage;
                 page++) {

                BufferedImage image =
                        renderer.renderImageWithDPI(
                                page - 1,
                                OCR_DPI);

                Path imageFile =
                        tempDirectory.resolve(
                                "page-" + page + ".png");

                ImageIO.write(
                        image,
                        "png",
                        imageFile.toFile());

                Process process =
                        new ProcessBuilder(
                                executable,
                                imageFile.toString(),
                                "stdout",
                                "-l",
                                "vie+eng",
                                "--psm",
                                "6")
                                .redirectErrorStream(true)
                                .start();

                boolean finished;

                try {
                    finished =
                            process.waitFor(
                                    90,
                                    TimeUnit.SECONDS);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();

                    throw new IOException(
                            "OCR process interrupted",
                            exception);
                }

                if (!finished) {
                    process.destroyForcibly();

                    throw new IOException(
                            "OCR timed out on PDF page "
                                    + page);
                }

                String output =
                        new String(
                                process
                                        .getInputStream()
                                        .readAllBytes(),
                                StandardCharsets.UTF_8);

                if (process.exitValue() != 0) {
                    throw new IOException(
                            "Tesseract failed on PDF page "
                                    + page
                                    + ": "
                                    + output);
                }

                result.append(output)
                        .append('\n');
            }

            return result.toString();

        } finally {

            if (Files.exists(tempDirectory)) {

                try (var paths =
                             Files.walk(tempDirectory)) {

                    paths.sorted(
                                    Comparator.reverseOrder())
                            .forEach(path -> {
                                try {
                                    Files.deleteIfExists(path);
                                } catch (IOException ignored) {
                                    // best-effort cleanup
                                }
                            });
                }
            }
        }
    }
}