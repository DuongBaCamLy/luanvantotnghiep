package com.scse.curriculum.syllabus.source.repository;
import com.scse.curriculum.syllabus.source.entity.SyllabusSourceSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface SyllabusSourceSnapshotRepository extends JpaRepository<SyllabusSourceSnapshot, Long> {
    Optional<SyllabusSourceSnapshot> findBySyllabus_Id(Integer syllabusId);
}
