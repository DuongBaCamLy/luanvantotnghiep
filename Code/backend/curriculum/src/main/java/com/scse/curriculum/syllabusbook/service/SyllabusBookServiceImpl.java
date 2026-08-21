package com.scse.curriculum.syllabusbook.service;

import com.scse.curriculum.book.entity.Book;
import com.scse.curriculum.book.repository.BookRepository;
import com.scse.curriculum.common.exception.ResourceNotFoundException;
import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.syllabus.repository.SyllabusRepository;
import com.scse.curriculum.syllabus.service.SyllabusContentGuard;
import com.scse.curriculum.syllabusbook.dto.SyllabusBookRequest;
import com.scse.curriculum.syllabusbook.dto.SyllabusBookResponse;
import com.scse.curriculum.syllabusbook.entity.SyllabusBook;
import com.scse.curriculum.syllabusbook.entity.SyllabusBookId;
import com.scse.curriculum.syllabusbook.repository.SyllabusBookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SyllabusBookServiceImpl implements SyllabusBookService {

    private final SyllabusBookRepository syllabusBookRepository;
    private final SyllabusRepository syllabusRepository;
    private final BookRepository bookRepository;
    private final SyllabusContentGuard syllabusContentGuard;

    @Override
    public SyllabusBookResponse create(
            SyllabusBookRequest request) {

        Syllabus syllabus = syllabusRepository.findById(
                        request.getSyllabusId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Syllabus not found with id: "
                                        + request.getSyllabusId()));

        syllabusContentGuard.assertMutable(syllabus.getId());

        Book book = bookRepository.findById(
                        request.getBookId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Book not found with id: "
                                        + request.getBookId()));

        SyllabusBookId id = new SyllabusBookId(
                syllabus.getId(),
                book.getId());

        SyllabusBook syllabusBook = SyllabusBook.builder()
                .id(id)
                .syllabus(syllabus)
                .book(book)
                .usageType(request.getUsageType())
                .orderIndex(request.getOrderIndex())
                .build();

        syllabusBook =
                syllabusBookRepository.save(syllabusBook);

        return mapToResponse(syllabusBook);
    }

    @Override
    public List<SyllabusBookResponse> getBySyllabus(
            Integer syllabusId) {

        syllabusContentGuard.assertCanView(syllabusId);
        return syllabusBookRepository
                .findBySyllabus_Id(syllabusId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<SyllabusBookResponse> getByBook(
            Integer bookId) {

        return syllabusBookRepository
                .findByBook_Id(bookId)
                .stream()
                .filter(mapping -> syllabusContentGuard.canView(
                        mapping.getSyllabus().getId()))
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public void delete(
            Integer syllabusId,
            Integer bookId) {

        SyllabusBookId id =
                new SyllabusBookId(
                        syllabusId,
                        bookId);

        SyllabusBook syllabusBook =
                syllabusBookRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "SyllabusBook mapping not found"));

        syllabusContentGuard.assertMutable(syllabusId);
        syllabusBookRepository.delete(syllabusBook);
    }

    private SyllabusBookResponse mapToResponse(
        SyllabusBook syllabusBook) {

    return SyllabusBookResponse.builder()
            .syllabusId(
                    syllabusBook.getSyllabus().getId())
            .bookId(
                    syllabusBook.getBook().getId())
            .bookTitle(
                    syllabusBook.getBook().getTitle())
            .author(
                    syllabusBook.getBook().getAuthor())
            .publisher(
                    syllabusBook.getBook().getPublisher())
            .year(
                    syllabusBook.getBook().getYear())
            .edition(
                    syllabusBook.getBook().getEdition())
            .isbn(
                    syllabusBook.getBook().getIsbn())
            .url(
                    syllabusBook.getBook().getUrl())
            .usageType(
                    syllabusBook.getUsageType())
            .orderIndex(
                    syllabusBook.getOrderIndex())
            .build();
}
}