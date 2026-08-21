package com.scse.curriculum.syllabus.pdf;

public interface SyllabusPdfService {

    SyllabusPdfResult generate(Integer syllabusId, SyllabusPdfMode mode);
}
