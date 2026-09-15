package com.scse.curriculum.syllabus.controller;


import com.scse.curriculum.syllabus.dto.CloneSyllabusRequest;
import com.scse.curriculum.syllabus.dto.CreateSyllabusRequest;
import com.scse.curriculum.syllabus.dto.SyllabusCatalogResponse;
import com.scse.curriculum.syllabus.dto.SyllabusCreateContextResponse;
import com.scse.curriculum.syllabus.dto.SyllabusDiffResponse;
import com.scse.curriculum.syllabus.dto.SyllabusListContextResponse;
import com.scse.curriculum.syllabus.dto.SyllabusResponse;
import com.scse.curriculum.syllabus.dto.SubmissionValidationResponse;

import com.scse.curriculum.syllabus.service.SyllabusManagementService;
import com.scse.curriculum.syllabus.service.SyllabusService;

import com.scse.curriculum.syllabus.pdf.SyllabusPdfMode;
import com.scse.curriculum.syllabus.pdf.SyllabusPdfResult;
import com.scse.curriculum.syllabus.pdf.SyllabusPdfService;
import com.scse.curriculum.syllabus.word.SyllabusWordResult;
import com.scse.curriculum.syllabus.word.SyllabusWordService;


import jakarta.validation.Valid;


import lombok.RequiredArgsConstructor;


import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;


import org.springframework.security.access.prepost.PreAuthorize;


import org.springframework.web.bind.annotation.*;
import com.scse.curriculum.syllabus.comparison.dto.SemanticSyllabusDiffResponse;

import java.nio.charset.StandardCharsets;
import java.util.List;



@RestController
@RequestMapping("/api/syllabuses")
@RequiredArgsConstructor
public class SyllabusController {



    private final SyllabusService service;


    private final SyllabusManagementService syllabusManagementService;


    private final SyllabusPdfService syllabusPdfService;
    private final SyllabusWordService syllabusWordService;




    /*
     * =====================================================
     * SYLLABUS CATALOG
     *
     * GET /api/syllabuses/catalog
     *
     * Hiển thị:
     *
     * Course
     * Version
     * Program
     * Created / Imported By
     * Status
     * Final Approval Date
     * Actions
     *
     * =====================================================
     */


    @GetMapping("/catalog")
    @PreAuthorize(
            "hasAnyRole('ADMIN','INSTRUCTOR','DEPT_HEAD','DEAN')"
    )
    public SyllabusCatalogResponse getCatalog(){


        return syllabusManagementService
                .getCatalog();

    }






    /*
     * =====================================================
     * ADMIN LIST CONTEXT
     *
     * GET /api/syllabuses/list-context
     *
     * =====================================================
     */


    @GetMapping("/list-context")
    @PreAuthorize("hasRole('ADMIN')")
    public SyllabusListContextResponse getListContext(){


        return syllabusManagementService
                .getListContext();

    }






    /*
     * =====================================================
     * CREATE CONTEXT
     *
     * GET /api/syllabuses/create-context
     *
     * =====================================================
     */


    @GetMapping("/create-context")
    @PreAuthorize(
            "hasAnyRole('INSTRUCTOR','ADMIN')"
    )
    public SyllabusCreateContextResponse createContext(

            @RequestParam Integer courseId,

            @RequestParam(required = false)
            String academicYear,


            @RequestParam(required = false)
            String semester

    ){


        // Use the full create context (course + course-program + PLO + defaults).
        // The management projection is catalog-oriented and its historical
        // ORDER BY Optional query can return more than one syllabus version.
        return service.getCreateContext(courseId);

    }







    /*
     * =====================================================
     * CREATE MANUAL SYLLABUS
     * =====================================================
     */


    @PostMapping
    @PreAuthorize(
            "hasAnyRole('INSTRUCTOR','ADMIN')"
    )
    public SyllabusResponse create(

            @Valid
            @RequestBody
            CreateSyllabusRequest request

    ){

        return service.create(request);

    }







    /*
     * =====================================================
     * BASIC CRUD
     * =====================================================
     */


    @GetMapping
    public List<SyllabusResponse> getAll(){


        return service.getAll();

    }






    @GetMapping("/{id}")
    public SyllabusResponse getById(

            @PathVariable Integer id,

            @RequestParam(defaultValue = "false") boolean forEdit

    ){

        return forEdit
                ? service.getByIdForEdit(id)
                : service.getById(id);

    }







    @GetMapping("/course/{courseId}")
    public List<SyllabusResponse> getByCourse(

            @PathVariable Integer courseId

    ){

        return service.getByCourse(courseId);

    }






    @GetMapping("/status/{status}")
    public List<SyllabusResponse> getByStatus(

            @PathVariable String status

    ){

        return service.getByStatus(status);

    }







    @PutMapping("/{id}")
    @PreAuthorize(
            "hasAnyRole('INSTRUCTOR','ADMIN')"
    )
    public SyllabusResponse update(

            @PathVariable Integer id,

            @Valid
            @RequestBody
            CreateSyllabusRequest request

    ){

        return service.update(
                id,
                request
        );

    }







    @DeleteMapping("/{id:[0-9]+}")
    @PreAuthorize(
            "hasAnyRole('INSTRUCTOR','ADMIN')"
    )
    public ResponseEntity<Void> delete(

            @PathVariable Integer id

    ){

        service.delete(id);


        return ResponseEntity
                .noContent()
                .build();

    }









    /*
     * =====================================================
     * SUBMISSION
     * =====================================================
     */


    @GetMapping("/{id}/submission-validation")
    @PreAuthorize(
            "hasAnyRole('INSTRUCTOR','ADMIN')"
    )
    public SubmissionValidationResponse validateForSubmit(

            @PathVariable Integer id

    ){

        return service.validateForSubmit(id);

    }






    @PostMapping("/{id}/submit")
    @PreAuthorize(
            "hasAnyRole('INSTRUCTOR','ADMIN')"
    )
    public SyllabusResponse submit(

            @PathVariable Integer id

    ){

        return service.submit(id);

    }


@GetMapping("/{id}/previous-comparable")
@PreAuthorize(
        "hasAnyRole('DEPT_HEAD','DEAN','ADMIN','INSTRUCTOR')")
public ResponseEntity<SyllabusResponse>
getPreviousComparable(
        @PathVariable Integer id) {

    SyllabusResponse previous =
            service.getPreviousComparable(id);

    if (previous == null) {
        return ResponseEntity
                .noContent()
                .build();
    }

    return ResponseEntity.ok(previous);
}

/*
 * =====================================================
 * VERSION DIFF
 * =====================================================
 */

@GetMapping("/{id}/diff")
@PreAuthorize(
        "hasAnyRole('DEPT_HEAD','DEAN','ADMIN','INSTRUCTOR')"
)
public SyllabusDiffResponse getDiff(

        @PathVariable Integer id,

        @RequestParam Integer compareWith

) {

    return service.getDiff(
            compareWith,
            id
    );
}


/*
 * =====================================================
 * AI-ASSISTED SEMANTIC DIFF
 *
 * compareWith = OLD / baseline syllabus
 * id          = NEW / target syllabus
 * =====================================================
 */

@PostMapping("/{id}/diff/semantic")
@PreAuthorize(
        "hasAnyRole('DEPT_HEAD','DEAN','ADMIN','INSTRUCTOR')"
)
public SemanticSyllabusDiffResponse getSemanticDiff(

        @PathVariable Integer id,

        @RequestParam Integer compareWith

) {

    return service.getSemanticDiff(
            compareWith,
            id
    );
}

    /*
     * =====================================================
     * CLONE
     * =====================================================
     */


    @PostMapping("/{id}/clone")
    @PreAuthorize(
            "hasAnyRole('INSTRUCTOR','ADMIN')"
    )
    public SyllabusResponse clone(

            @PathVariable Integer id,

            @RequestBody CloneSyllabusRequest request

    ){

        return service.clone(
                id,
                request
        );

    }








    /*
     * =====================================================
     * PDF EXPORT
     * =====================================================
     */


    @GetMapping(
            value="/{id}/pdf/preview",
            produces=MediaType.APPLICATION_PDF_VALUE
    )
    @PreAuthorize(
            "hasAnyRole('INSTRUCTOR','ADMIN','DEPT_HEAD','DEAN')"
    )
    public ResponseEntity<byte[]> previewPdf(

            @PathVariable Integer id

    ){


        SyllabusPdfResult result =
                syllabusPdfService.generate(
                        id,
                        SyllabusPdfMode.PREVIEW
                );


        return pdfResponse(
                result,
                false
        );

    }






    @GetMapping(
            value="/{id}/pdf",
            produces=MediaType.APPLICATION_PDF_VALUE
    )
    @PreAuthorize(
            "hasAnyRole('INSTRUCTOR','ADMIN','DEPT_HEAD','DEAN')"
    )
    public ResponseEntity<byte[]> exportPdf(

            @PathVariable Integer id

    ){


        SyllabusPdfResult result =
                syllabusPdfService.generate(
                        id,
                        SyllabusPdfMode.EXPORT
                );


        return pdfResponse(
                result,
                true
        );

    }

    @GetMapping(value="/{id}/word/original",
            produces="application/vnd.openxmlformats-officedocument.wordprocessingml.document")
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN','DEPT_HEAD','DEAN')")
    public ResponseEntity<byte[]> exportOriginalWord(@PathVariable Integer id) {
        return wordResponse(syllabusWordService.original(id));
    }

    @GetMapping(value="/{id}/word/current",
            produces="application/vnd.openxmlformats-officedocument.wordprocessingml.document")
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN','DEPT_HEAD','DEAN')")
    public ResponseEntity<byte[]> exportCurrentWord(@PathVariable Integer id) {
        return wordResponse(syllabusWordService.current(id));
    }

    private ResponseEntity<byte[]> wordResponse(SyllabusWordResult result) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(result.filename(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-SHA256", result.sha256())
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(result.content());
    }








    private ResponseEntity<byte[]> pdfResponse(

            SyllabusPdfResult result,

            boolean attachment

    ){


        ContentDisposition disposition =

                (attachment
                        ?
                        ContentDisposition.attachment()
                        :
                        ContentDisposition.inline()
                )

                .filename(
                        result.filename(),
                        StandardCharsets.UTF_8
                )

                .build();



        return ResponseEntity.ok()

                .contentType(
                        MediaType.APPLICATION_PDF
                )

                .cacheControl(
                        CacheControl.noStore()
                                .mustRevalidate()
                )

                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        disposition.toString()
                )

                .header(
                        "X-Content-Type-Options",
                        "nosniff"
                )

                .body(
                        result.content()
                );

    }


}
