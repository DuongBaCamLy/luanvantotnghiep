package com.scse.curriculum.syllabus.pdf;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Locale;

@Service
public class SyllabusPdfServiceImpl implements SyllabusPdfService {

    private final SyllabusPdfDataLoader dataLoader;
    private final SyllabusPdfRenderer renderer;

    public SyllabusPdfServiceImpl(
            SyllabusPdfDataLoader dataLoader,
            SyllabusPdfRenderer renderer) {
        this.dataLoader = dataLoader;
        this.renderer = renderer;
    }

    @Override
    public SyllabusPdfResult generate(Integer syllabusId, SyllabusPdfMode mode) {
        SyllabusPdfDocument document = dataLoader.load(syllabusId);
        byte[] content = renderer.render(document, mode);
        return new SyllabusPdfResult(content, filename(document, mode));
    }

    private String filename(SyllabusPdfDocument document, SyllabusPdfMode mode) {
        String raw = String.join("_",
                "Syllabus",
                value(document.courseCode()),
                value(document.academicYear()),
                value(document.semester()),
                value(document.versionLabel()),
                mode == SyllabusPdfMode.PREVIEW ? "Preview" : "Official");

        String normalized = Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^A-Za-z0-9._-]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        return normalized + ".pdf";
    }

    private String value(String value) {
        return value == null || value.isBlank()
                ? "NA"
                : value.trim().toUpperCase(Locale.ROOT);
    }
}
