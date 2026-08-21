package com.scse.curriculum.book.service;

import com.scse.curriculum.book.dto.BookRequest;
import com.scse.curriculum.book.dto.BookResponse;
import com.scse.curriculum.book.entity.Book;
import com.scse.curriculum.book.repository.BookRepository;
import com.scse.curriculum.book.service.BookService;
import com.scse.curriculum.common.exception.ResourceNotFoundException;
import com.scse.curriculum.syllabus.entity.SyllabusStatus;
import com.scse.curriculum.syllabusbook.repository.SyllabusBookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final SyllabusBookRepository syllabusBookRepository;

    @Override
    public BookResponse create(BookRequest request) {

        Book book = Book.builder()
                .title(request.getTitle())
                .author(request.getAuthor())
                .publisher(request.getPublisher())
                .year(request.getYear())
                .edition(request.getEdition())
                .isbn(request.getIsbn())
                .url(request.getUrl())
                .bookType(request.getBookType())
                .build();

        return mapToResponse(
                bookRepository.save(book));
    }

    @Override
    public BookResponse getById(Integer id) {

        Book book = bookRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Book not found with id: " + id));

        return mapToResponse(book);
    }

    @Override
    public List<BookResponse> getAll() {

        return bookRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public BookResponse update(
            Integer id,
            BookRequest request) {

        Book book = bookRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Book not found with id: " + id));

        assertBookIsNotUsedByImmutableVersion(id);

        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setPublisher(request.getPublisher());
        book.setYear(request.getYear());
        book.setEdition(request.getEdition());
        book.setIsbn(request.getIsbn());
        book.setUrl(request.getUrl());
        book.setBookType(request.getBookType());

        return mapToResponse(
                bookRepository.save(book));
    }

    @Override
    public void delete(Integer id) {

        if (!bookRepository.existsById(id)) {

            throw new ResourceNotFoundException(
                    "Book not found with id: " + id);
        }

        assertBookIsNotUsedByImmutableVersion(id);
        bookRepository.deleteById(id);
    }

    private void assertBookIsNotUsedByImmutableVersion(Integer bookId) {
        if (syllabusBookRepository
                .existsByBook_IdAndSyllabus_StatusNot(
                        bookId,
                        SyllabusStatus.DRAFT)) {

            throw new IllegalStateException(
                    "Tài liệu này đang được dùng bởi một syllabus đã nộp. "
                            + "Hãy tạo Book mới thay vì sửa hoặc xóa Book cũ.");
        }
    }

    private BookResponse mapToResponse(
            Book book) {

        return BookResponse.builder()
                .id(book.getId())
                .title(book.getTitle())
                .author(book.getAuthor())
                .publisher(book.getPublisher())
                .year(book.getYear())
                .edition(book.getEdition())
                .isbn(book.getIsbn())
                .url(book.getUrl())
                .bookType(book.getBookType())
                .build();
    }
}