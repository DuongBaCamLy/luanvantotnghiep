package com.scse.curriculum.book.repository;

import com.scse.curriculum.book.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository
        extends JpaRepository<Book, Integer> {
}