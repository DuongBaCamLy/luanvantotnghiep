package com.scse.curriculum.syllabusbook.entity;

import com.scse.curriculum.book.entity.Book;
import com.scse.curriculum.syllabus.entity.Syllabus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "syllabus_book")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyllabusBook {

    @EmbeddedId
    private SyllabusBookId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("syllabusId")
    @JoinColumn(name = "syllabus_id")
    private Syllabus syllabus;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("bookId")
    @JoinColumn(name = "book_id")
    private Book book;

    @Enumerated(EnumType.STRING)
    @Column(name = "usage_type")
    @Builder.Default
    private UsageType usageType = UsageType.RECOMMENDED;

    @Column(name = "order_index")
    @Builder.Default
    private Integer orderIndex = 1;

    @PrePersist
    private void initializeRequiredValues() {
        if (usageType == null) usageType = UsageType.RECOMMENDED;
        if (orderIndex == null) orderIndex = 1;
    }
}
