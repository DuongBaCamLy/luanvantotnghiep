package com.scse.curriculum.syllabus.word;

import com.scse.curriculum.syllabus.dto.SyllabusResponse;
import com.scse.curriculum.syllabus.service.SyllabusService;
import com.scse.curriculum.syllabus.pdf.SyllabusPdfDataLoader;
import com.scse.curriculum.syllabus.pdf.SyllabusPdfDocument;
import com.scse.curriculum.syllabus.source.entity.SyllabusSourceSnapshot;
import com.scse.curriculum.syllabus.source.repository.SyllabusSourceSnapshotRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SyllabusWordServiceImplTest {
    private final SyllabusSourceSnapshotRepository snapshots = mock(SyllabusSourceSnapshotRepository.class);
    private final SyllabusService syllabuses = mock(SyllabusService.class);
    private final SyllabusPdfDataLoader loader = mock(SyllabusPdfDataLoader.class);
    private final SyllabusWordRenderer renderer = mock(SyllabusWordRenderer.class);
    private final SyllabusWordServiceImpl service = new SyllabusWordServiceImpl(snapshots, syllabuses, loader, renderer);

    @Test
    void currentUsesAuthorizedSharedSavedDataWithoutRequiringDocxSnapshot() {
        SyllabusResponse response = mock(SyllabusResponse.class);
        when(response.getCourseCode()).thenReturn("IT116IU"); when(response.getVersionNumber()).thenReturn(4);
        when(syllabuses.getById(7)).thenReturn(response);
        SyllabusPdfDocument saved = mock(SyllabusPdfDocument.class);
        when(loader.load(7)).thenReturn(saved);
        byte[] output = {1, 2, 3}; when(renderer.render(saved)).thenReturn(output);
        var result = service.current(7);
        assertArrayEquals(output, result.content());
        assertEquals("Syllabus_IT116IU_V4_Current.docx", result.filename());
        assertEquals(64, result.sha256().length());
        verifyNoInteractions(snapshots);
    }

    @Test
    void originalReturnsExactUnmodifiedSnapshot() {
        when(syllabuses.getById(7)).thenReturn(mock(SyllabusResponse.class));
        var snapshot = mock(SyllabusSourceSnapshot.class);
        when(snapshot.getContent()).thenReturn(new byte[]{9,8,7}); when(snapshot.getSha256()).thenReturn("original-sha");
        when(snapshots.findBySyllabus_Id(7)).thenReturn(Optional.of(snapshot));
        var result = service.original(7);
        assertArrayEquals(new byte[]{9,8,7}, result.content()); assertEquals("original-sha", result.sha256());
        verifyNoInteractions(loader, renderer);
    }

    @Test
    void deniedAccessNeverLoadsOrRendersExport() {
        when(syllabuses.getById(7)).thenThrow(new AccessDeniedException("denied"));
        assertThrows(AccessDeniedException.class, () -> service.current(7));
        assertThrows(AccessDeniedException.class, () -> service.original(7));
        verifyNoInteractions(loader, renderer, snapshots);
    }
}
