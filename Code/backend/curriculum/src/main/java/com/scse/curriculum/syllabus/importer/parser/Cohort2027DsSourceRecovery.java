package com.scse.curriculum.syllabus.importer.parser;

/** DS2026 uses filled polygon rules; this opt-in cannot change CS/IT or legacy imports. */
final class Cohort2027DsSourceRecovery {
    static final String SOURCE_SHA256 = "fd279731316e301fce93cd25c9231959b1f73631f62c7d38e883d87bfaba1f32";
    static final String REVIEW_RESOURCE = "/syllabus-import/verified-ds2026-reviewed-syllabi.json";
    private Cohort2027DsSourceRecovery() {}
    static boolean supports(String sha256) { return SOURCE_SHA256.equals(sha256); }

    static void warnOnUnverifiedWeeklyReferences(com.scse.curriculum.syllabus.importer.dto.SyllabusImportData data,
            java.util.List<com.scse.curriculum.syllabus.importer.dto.SyllabusImportIssue> issues) {
        var cloCodes=new java.util.HashSet<String>();
        for(var clo:data.getClos())if(clo.getCode()!=null)cloCodes.add(clo.getCode().replaceAll("\\s+","").toUpperCase(java.util.Locale.ROOT));
        for(var row:data.getWeeklyActivities()) {
            if(row.getClo()==null || row.getClo().isBlank())continue;
            var codes=java.util.regex.Pattern.compile("(?i)(?:CLO\\s*)?(\\d+)").matcher(row.getClo());
            var unresolved=new java.util.LinkedHashSet<String>();
            while(codes.find())if(!cloCodes.contains("CLO"+codes.group(1)))unresolved.add("CLO"+codes.group(1));
            if(!unresolved.isEmpty())issues.add(com.scse.curriculum.syllabus.importer.dto.SyllabusImportIssue.builder()
                    .severity("WARNING").section("Planned Activities").field("clo").row(row.getWeek())
                    .message("SOURCE_UNVERIFIED: Weekly CLO text '"+row.getClo()+"' refers to "+String.join(", ",unresolved)
                            +" outside the source CLO definitions. The original text is retained; no Topic–CLO relationship is inferred.").build());
        }
    }
}
