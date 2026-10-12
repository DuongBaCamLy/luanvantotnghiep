package com.scse.curriculum.syllabus.importer.parser;

import com.scse.curriculum.syllabus.importer.dto.SyllabusImportData;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class Cohort2027VerifiedSourceRecoveryTest {
    private static final String HASH="b24a4b9a0d052f8decc79410b92150415126bb5aabce6fa550bf4706d940f68a";
    @Test void matchesExactDocumentCourseAndPagesAndLeavesWeekHoursEmpty() throws Exception {
        var data=new SyllabusImportData();data.setSourceCourseCode("IT076IU");
        assertTrue(Cohort2027VerifiedSourceRecovery.apply(HASH,290,296,data));
        assertEquals(9,data.getTopics().size());
        assertEquals("7.5",data.getTopics().get(2).getContentWeight());
        assertTrue(data.getTopics().stream().allMatch(t->t.getWeekNumber()==null && t.getTeachingHours()==null && t.getTopicType()==null));
        assertTrue(data.getTopicCloMappings().isEmpty());
    }
    @Test void differentDocumentCourseOrSectionCannotReceiveAcademicContent() throws Exception {
        var data=new SyllabusImportData();data.setSourceCourseCode("IT076IU");
        assertFalse(Cohort2027VerifiedSourceRecovery.apply("other-document",290,296,data));
        assertFalse(Cohort2027VerifiedSourceRecovery.apply(HASH,290,297,data));
        data.setSourceCourseCode("IT017IU");
        assertFalse(Cohort2027VerifiedSourceRecovery.apply(HASH,290,296,data));
        assertTrue(data.getTopics().isEmpty());
    }
}
