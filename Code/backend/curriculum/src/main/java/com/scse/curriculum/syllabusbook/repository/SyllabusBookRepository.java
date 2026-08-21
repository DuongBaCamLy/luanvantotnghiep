package com.scse.curriculum.syllabusbook.repository;

import com.scse.curriculum.syllabus.entity.SyllabusStatus;
import com.scse.curriculum.syllabusbook.entity.SyllabusBook;
import com.scse.curriculum.syllabusbook.entity.SyllabusBookId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SyllabusBookRepository
        extends JpaRepository<SyllabusBook, SyllabusBookId> {

    List<SyllabusBook> findBySyllabus_Id(Integer syllabusId);

    List<SyllabusBook> findByBook_Id(Integer bookId);

    boolean existsByBook_IdAndSyllabus_StatusNot(
            Integer bookId,
            SyllabusStatus status);

    @Query("""
            SELECT syllabusBook
            FROM SyllabusBook syllabusBook
            JOIN FETCH syllabusBook.book book
            WHERE syllabusBook.syllabus.id = :syllabusId
            ORDER BY syllabusBook.orderIndex, book.title
            """)
    List<SyllabusBook> findForPdfBySyllabusId(
            @Param("syllabusId") Integer syllabusId);
}
