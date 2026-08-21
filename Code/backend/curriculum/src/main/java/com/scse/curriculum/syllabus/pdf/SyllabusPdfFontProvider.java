package com.scse.curriculum.syllabus.pdf;

import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.BaseFont;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
public class SyllabusPdfFontProvider {

    private final BaseFont regularBaseFont;
    private final BaseFont boldBaseFont;
    private final boolean unicodeReady;

    public SyllabusPdfFontProvider(
            @Value("${app.pdf.font.regular:}") String configuredRegular,
            @Value("${app.pdf.font.bold:}") String configuredBold) {
        ResolvedFont regular = resolveFont(configuredRegular, false);
        ResolvedFont bold = resolveFont(configuredBold, true);
        this.regularBaseFont = regular.baseFont();
        this.boldBaseFont = bold.baseFont();
        this.unicodeReady = regular.unicodeEmbedded() && bold.unicodeEmbedded();
    }

    public Font regular(float size) {
        return regular(size, Color.BLACK);
    }

    public Font regular(float size, Color color) {
        return new Font(regularBaseFont, size, Font.NORMAL, color);
    }

    public Font bold(float size) {
        return bold(size, Color.BLACK);
    }

    public Font bold(float size, Color color) {
        return new Font(boldBaseFont, size, Font.BOLD, color);
    }

    public BaseFont regularBaseFont() {
        return regularBaseFont;
    }

    /**
     * True only when both regular and bold fonts use Identity-H and are embedded.
     * Matrix PDF export requires this to guarantee Vietnamese text integrity.
     */
    public boolean isUnicodeReady() {
        return unicodeReady;
    }

    private ResolvedFont resolveFont(String configuredPath, boolean bold) {
        List<String> candidates = new ArrayList<>();
        if (configuredPath != null && !configuredPath.isBlank()) {
            candidates.add(configuredPath.trim());
        }

        if (bold) {
            candidates.add("C:/Windows/Fonts/arialbd.ttf");
            candidates.add("C:/Windows/Fonts/calibrib.ttf");
            candidates.add("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf");
            candidates.add("/usr/share/fonts/truetype/liberation2/LiberationSans-Bold.ttf");
            candidates.add("/System/Library/Fonts/Supplemental/Arial Bold.ttf");
        } else {
            candidates.add("C:/Windows/Fonts/arial.ttf");
            candidates.add("C:/Windows/Fonts/calibri.ttf");
            candidates.add("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf");
            candidates.add("/usr/share/fonts/truetype/liberation2/LiberationSans-Regular.ttf");
            candidates.add("/System/Library/Fonts/Supplemental/Arial.ttf");
        }

        for (String candidate : candidates) {
            try {
                Path path = Path.of(candidate);
                if (Files.isRegularFile(path)) {
                    BaseFont baseFont = BaseFont.createFont(
                            path.toAbsolutePath().toString(),
                            BaseFont.IDENTITY_H,
                            BaseFont.EMBEDDED);
                    return new ResolvedFont(baseFont, true);
                }
            } catch (IOException | RuntimeException ignored) {
                // Try the next portable candidate.
            }
        }

        // Keep the legacy fallback for non-critical PDF features. The matrix PDF
        // exporter explicitly rejects it through isUnicodeReady().
        try {
            BaseFont fallback = BaseFont.createFont(
                    bold ? BaseFont.HELVETICA_BOLD : BaseFont.HELVETICA,
                    BaseFont.CP1252,
                    BaseFont.NOT_EMBEDDED);
            return new ResolvedFont(fallback, false);
        } catch (DocumentException | IOException exception) {
            throw new IllegalStateException("Không thể khởi tạo font PDF.", exception);
        }
    }

    private record ResolvedFont(BaseFont baseFont, boolean unicodeEmbedded) {
    }
}
