package com.scse.curriculum.syllabus.word;
public interface SyllabusWordService {
    SyllabusWordResult original(Integer syllabusId);
    SyllabusWordResult current(Integer syllabusId);
}
