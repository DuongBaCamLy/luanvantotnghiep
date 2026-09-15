package com.scse.curriculum.syllabus.history;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.syllabus.repository.SyllabusRepository;
import com.scse.curriculum.syllabus.service.SyllabusAccessService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service @RequiredArgsConstructor
public class SyllabusHistoryService {
    private final SyllabusRevisionSnapshotRepository snapshots;
    private final SyllabusRepository syllabuses;
    private final SyllabusAccessService access;
    private final EntityManager entityManager;
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    @Transactional
    public void capture(Syllabus syllabus, String event, String actor) {
        entityManager.flush();
        Map<String,Object> content = new LinkedHashMap<>();
        content.put("syllabus", jdbc.queryForMap("select * from syllabus where id=?", syllabus.getId()));
        for (String table : List.of("clo","topic","assessment_component","syllabus_book","syllabus_import_history")) {
            content.put(table, jdbc.queryForList("select * from " + table + " where syllabus_id=?", syllabus.getId()));
        }
        for (String[] link : List.of(new String[]{"clo_plo_mapping","clo","clo_id"},
                new String[]{"topic_clo","topic","topic_id"},
                new String[]{"assessment_clo","assessment_component","assessment_component_id"},
                new String[]{"student_score","assessment_component","assessment_component_id"})) {
            content.put(link[0], jdbc.queryForList("select x.* from " + link[0] + " x join " + link[1]
                    + " p on p.id=x." + link[2] + " where p.syllabus_id=?", syllabus.getId()));
        }
        content.put("book", jdbc.queryForList("select b.* from book b join syllabus_book x on x.book_id=b.id where x.syllabus_id=?", syllabus.getId()));
        content.put("plo", jdbc.queryForList("select distinct p.* from plo p join clo_plo_mapping x on x.plo_id=p.id join clo c on c.id=x.clo_id where c.syllabus_id=?", syllabus.getId()));
        content.put("course", jdbc.queryForList("select * from course where id=?", syllabus.getCourse().getId()));
        try {
            snapshots.save(SyllabusRevisionSnapshot.builder().syllabusId(syllabus.getId())
                    .originalSyllabusId(syllabus.getId()).versionNumber(syllabus.getVersionNumber())
                    .versionLabel(syllabus.getVersionLabel()).eventType(event).actor(actor)
                    .capturedAt(LocalDateTime.now()).content(mapper.writeValueAsString(content)).build());
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Cannot preserve syllabus revision content",e);
        }
    }

    @Transactional(readOnly=true)
    public List<HistoryItem> history(Integer id) {
        Syllabus syllabus=syllabuses.findByIdWithRelations(id).orElseThrow();
        access.assertCanViewHistory(syllabus);
        return snapshots.findBySyllabusIdOrderByCapturedAtAscIdAsc(id).stream().map(snapshot -> {
            try {
                var content = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(snapshot.getContent());
                // Assessment scores are retained for migration/audit recovery, never exposed as syllabus content.
                content.remove("student_score");
                return new HistoryItem(snapshot.getId(), snapshot.getVersionNumber(), snapshot.getVersionLabel(),
                        snapshot.getEventType(), snapshot.getCapturedAt(), snapshot.getActor(), content);
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) { throw new IllegalStateException(e); }
        }).toList();
    }

    public record HistoryItem(Long id, Integer versionNumber, String versionLabel, String eventType,
            LocalDateTime capturedAt, String actor, com.fasterxml.jackson.databind.JsonNode content) {}

    public void assertDeletable(Integer id) {
        if (snapshots.existsBySyllabusId(id)) throw new IllegalStateException("A syllabus with revision history must be retained.");
    }
}
