package com.scse.curriculum.syllabus.service;


import com.scse.curriculum.syllabus.dto.SyllabusCatalogResponse;
import com.scse.curriculum.syllabus.dto.SyllabusCreateContextResponse;
import com.scse.curriculum.syllabus.dto.SyllabusListContextResponse;



/**
 * =====================================================
 * Syllabus Management Service
 *
 * Responsible for:
 *
 * - Syllabus Catalog
 * - Admin syllabus list
 * - Create syllabus context
 *
 * This service does NOT handle:
 *
 * - Create syllabus
 * - Update syllabus
 * - Submit syllabus
 * - Approval workflow
 *
 * Those belong to SyllabusService
 *
 * =====================================================
 */
public interface SyllabusManagementService {



    /**
     * =====================================================
     * SYLLABUS CATALOG
     *
     * Used by:
     *
     * Syllabus Catalog page
     *
     *
     * Display columns:
     *
     * Course
     * Version
     * Program
     * Created / Imported By
     * Status
     * Final Approval Date
     * Actions
     *
     *
     * Flow:
     *
     * Import PDF/DOCX
     *          |
     *          v
     * Create Syllabus
     *          |
     *          v
     * Catalog displays record
     *
     * =====================================================
     */
    SyllabusCatalogResponse getCatalog();





    /**
     * =====================================================
     * ADMIN LIST CONTEXT
     *
     * Includes:
     *
     * - syllabus table data
     * - filters
     *
     *
     * Used for admin management page
     *
     * =====================================================
     */
    SyllabusListContextResponse getListContext();







    /**
     * =====================================================
     * CREATE SYLLABUS CONTEXT
     *
     * Used when creating a new syllabus
     *
     *
     * Priority:
     *
     * Latest Approved Syllabus
     *
     *          |
     *          v
     *
     * Latest Version
     *
     *          |
     *          v
     *
     * Course Data
     *
     *          |
     *          v
     *
     * Default Values
     *
     *
     * Note:
     *
     * Semester mapping will be handled
     * in future curriculum planning module.
     *
     * =====================================================
     */
    SyllabusCreateContextResponse getCreateContext(

            Integer courseId,

            String academicYear,

            String semester

    );

}