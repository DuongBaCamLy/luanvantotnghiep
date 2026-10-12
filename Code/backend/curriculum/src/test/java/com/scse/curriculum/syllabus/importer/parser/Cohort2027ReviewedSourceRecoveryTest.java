package com.scse.curriculum.syllabus.importer.parser;

import com.scse.curriculum.syllabus.importer.dto.SyllabusImportData;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class Cohort2027ReviewedSourceRecoveryTest {
    static final String HASH="b24a4b9a0d052f8decc79410b92150415126bb5aabce6fa550bf4706d940f68a";
    static SyllabusImportData reviewed(String code,int start,int end)throws Exception {
        var data=new SyllabusImportData();data.setSourceCourseCode(code);
        assertTrue(Cohort2027VerifiedSourceRecovery.applyReviewed(HASH,start,end,data));
        return data;
    }
    @Test void reviewedImageSourcesKeepContentWithoutInventedWeeksHoursOrPloRelations()throws Exception {
        for (var row : new Object[][]{{"PE015IU",54,62},{"PE016IU",63,77},{"PE017IU",78,91},{"PE018IU",92,103},{"PE019IU",104,117},{"MA001IU",162,167}}) {
            var data=reviewed((String)row[0],(Integer)row[1],(Integer)row[2]);
            Cohort2027ImportValidation.validate(data);
            assertFalse(data.getClos().isEmpty());assertFalse(data.getTopics().isEmpty());
            assertTrue(data.getTopics().stream().allMatch(t->t.getWeekNumber()==null&&t.getTeachingHours()==null&&t.getLabHours()==null&&t.getSelfStudyHours()==null));
            if (!data.getSourceCourseCode().equals("MA001IU")) assertTrue(data.getCloPloMappings().isEmpty());
            else assertTrue(data.getCloPloMappings().stream().allMatch(m -> m.getValue().matches("[34]")));
            assertTrue(data.getTopicCloMappings().isEmpty());
            assertFalse(Cohort2027VerifiedSourceRecovery.reviewedNotes(data).isEmpty());
        }
    }
    @Test void reviewedEvidenceCannotCertifyAnotherPdfSectionOrEditedAcademicValue()throws Exception {
        var untouched=new SyllabusImportData();untouched.setSourceCourseCode("PE017IU");
        assertFalse(Cohort2027VerifiedSourceRecovery.applyReviewed("other-pdf",78,91,untouched));
        assertFalse(Cohort2027VerifiedSourceRecovery.applyReviewed(HASH,78,92,untouched));
        var data=reviewed("PE017IU",78,91);data.getClos().get(0).setDescription("changed by client");
        assertThrows(IllegalArgumentException.class,()->Cohort2027VerifiedSourceRecovery.reviewedNotes(data));
    }
    @Test void ambiguousWeightsAndRepeatedSourceCodesStayRawAndNeverBecomeInventedRelations()throws Exception {
        var ma=reviewed("MA001IU",162,167);assertEquals(15,ma.getTopics().size());
        assertNull(ma.getTopics().get(1).getContentWeight());
        assertTrue(ma.getTopics().get(1).getNotes().contains("sourceWeightRaw"));
        var pe=reviewed("PE019IU",104,117);assertNull(pe.getAssessments().get(0).getWeightPercent());
        assertEquals(7,pe.getClos().size());
        var notes=new ObjectMapper().valueToTree(Cohort2027VerifiedSourceRecovery.reviewedNotes(pe));
        assertEquals(8,notes.path("sourceSupplementalSections").path("3").get(0).path("rows").size());
        assertTrue(notes.toString().contains("150%"));
        assertEquals(2,notes.path("sourceUnverifiedFields").size());
    }
    @Test void preservesReadingListContinuationAndSourcePlanInsteadOfDroppingCrossPageRows()throws Exception {
        var data=reviewed("PE017IU",78,91);
        assertTrue(data.getReadings().get(0).getTitle().contains("Hội đồng Trung ương (2008)"));
        var notes=new ObjectMapper().valueToTree(Cohort2027VerifiedSourceRecovery.reviewedNotes(data));
        assertEquals(8,notes.path("sourceSupplementalSections").path("9").get(0).path("rows").size());
        assertEquals(7,notes.path("sourceSupplementalSections").path("11").get(0).path("rows").size());
    }
}
