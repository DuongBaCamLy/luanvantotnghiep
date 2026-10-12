package com.scse.curriculum.syllabus.importer.parser;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import java.io.IOException;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

/** Source-derived NE/CE scope; parsing, validation and persistence remain shared. */
public final class SharedSpecializationPdfScope {
    // Catalogue prefixes used by these engineering curricula. Restrict matches
    // to course identifiers: laboratory labels and prose followed by row numbers
    // are not course codes. A terminal I may wrap its U into another table line.
    private static final Pattern CODE = Pattern.compile("(?<![A-Z0-9])((?:CHE|IT|EE|PE|MA|PH|CH|EN|PT|EL|CE|NE))\\s*[- ]?\\s*(\\d{2,3})\\s*(I(?:\\s*U)?|W\\s*E)?(?![A-Z0-9])");
    public record Scope(String specialization, Set<String> courseCodes, int firstPage, int lastPage) {}
    private SharedSpecializationPdfScope() {}

    public static boolean supports(String programCode) {
        return Set.of("NE", "CE").contains(programKey(programCode));
    }

    public static String programKey(String code) {
        return code == null ? "" : code.trim().toUpperCase(Locale.ROOT).replaceFirst("[-_]?\\d{4}$", "");
    }

    public static Scope extract(byte[] bytes, String programCode) throws IOException {
        try (var document = Loader.loadPDF(bytes)) {
            var stripper = new PDFTextStripper(); stripper.setSortByPosition(true);
            List<String> pages = new ArrayList<>();
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page); stripper.setEndPage(page); pages.add(stripper.getText(document));
            }
            return fromPageTexts(pages, programCode);
        }
    }

    public static Scope fromPageTexts(List<String> pages, String programCode) {
        String target = programKey(programCode);
        if (!supports(target)) throw new IllegalArgumentException("Shared specialization scope supports NE and CE only.");
        Map<String,StringBuilder> lists = new LinkedHashMap<>();
        lists.put("NE", new StringBuilder()); lists.put("CE", new StringBuilder());
        String active = null; boolean englishStructure = false;
        // Course lists precede appendices; never use syllabus references as membership evidence.
        for (String page : pages.subList(0, Math.min(140, pages.size()))) {
            for (String line : page.split("\\R")) {
                String text = normalize(line);
                if (text.matches("\\d+\\.?\\s*program structure")) englishStructure = true;
                if (text.matches("\\d+\\.?\\s*curriculum") || text.contains("du kien ke hoach giang day")
                        || text.matches("bang \\d+[.:]?.*ke hoach giang day.*")) {
                    active = null; englishStructure = false;
                }
                boolean courseList = text.contains("cac mon hoc thuoc ctdt") || text.contains("cac hoc phan thuoc ctdt");
                String role = specialization(text);
                if (role != null && (courseList || (englishStructure && text.contains("bachelor of engineering")))) active = role;
                if (active != null) lists.get(active).append(line).append('\n');
            }
        }
        Set<String> codes = extractCodes(lists.get(target).toString());
        if (codes.size() < 10) throw new IllegalArgumentException(
                "Cannot verify the " + target + " course list in this shared PDF; no syllabus is imported from the other specialization.");

        // The 2021 source has standalone appendix divider pages. Their locations
        // are discovered from titles, never from target cohort or fixed page numbers.
        Map<String,Integer> dividers = new LinkedHashMap<>();
        for (int index = 0; index < pages.size(); index++) {
            String text = normalize(pages.get(index).replaceAll("(?m)^\\s*\\d+(?:\\s*/\\s*\\d+)?\\s*$", ""));
            String role = specialization(text);
            if (text.length() < 260 && text.startsWith("bachelor of engineering") && text.endsWith("major") && role != null) {
                if (dividers.putIfAbsent(role, index + 1) != null) throw new IllegalArgumentException("Ambiguous specialization appendix dividers in the PDF.");
            }
        }
        int first = 1, last = pages.size();
        if (!dividers.isEmpty()) {
            if (!dividers.containsKey(target)) throw new IllegalArgumentException("No " + target + " syllabus appendix was found in this PDF.");
            first = dividers.get(target) + 1;
            final int selectedDivider = dividers.get(target);
            last = dividers.values().stream().filter(page -> page > selectedDivider).min(Integer::compareTo).map(page -> page - 1).orElse(pages.size());
        }
        return new Scope(target, Set.copyOf(codes), first, last);
    }

    private static String specialization(String text) {
        boolean network = text.contains("network engineering") || text.contains("ky thuat mang");
        boolean computer = text.contains("computer engineering") || text.contains("ky thuat may tinh");
        return network == computer ? null : network ? "NE" : "CE";
    }
    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .replace('đ','d').replace('Đ','D').toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }
    private static Set<String> extractCodes(String text) {
        Set<String> codes = new LinkedHashSet<>(); var matcher = CODE.matcher(text);
        while (matcher.find()) {
            String code = (matcher.group(1) + matcher.group(2) + (matcher.group(3) == null ? "" : matcher.group(3)))
                    .replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
            codes.add(code.endsWith("IU") ? code.substring(0,code.length()-2) : code.endsWith("I") ? code.substring(0,code.length()-1) : code);
        }
        return codes;
    }
}
