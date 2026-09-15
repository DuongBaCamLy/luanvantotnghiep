export interface SyllabusImportTemplateField {
  key: string
  label: string
}

export interface SyllabusImportTemplateSection {
  key:
    | "general"
    | "workloadCredit"
    | "requirements"
    | "clo"
    | "content"
    | "topicClo"
    | "cloPlo"
    | "plannedActivities"
    | "assessment"
    | "assessmentClo"
    | "examination"
    | "readings"
    | "revision"

  label: string
  fields: SyllabusImportTemplateField[]
}
export interface SyllabusImportTemplateField {
    key:string;
    label:string;
}

export interface SyllabusImportTemplateSection {
    key:
        | "general"
        | "workloadCredit"
        | "requirements"
        | "clo"
        | "content"
        | "topicClo"
        | "cloPlo"
        | "plannedActivities"
        | "assessment"
        | "assessmentClo"
        | "examination"
        | "readings"
        | "revision";
    label:string;
    fields:SyllabusImportTemplateField[];
}
export interface SyllabusImportTemplateField {
  key: string
  label: string
}

export interface SyllabusImportTemplateSection {
  key:
    | "general"
    | "workloadCredit"
    | "requirements"
    | "clo"
    | "content"
    | "topicClo"
    | "cloPlo"
    | "plannedActivities"
    | "assessment"
    | "assessmentClo"
    | "examination"
    | "readings"
    | "revision"

  label: string
  fields: SyllabusImportTemplateField[]
}
export interface CloImportData {

    code:string;

    description:string;

    descriptionVn?:string;

    bloomLevel?:string;

    competencyLevel?:string;

    orderIndex:number;

}



export interface TopicImportData {


    weekNumber:number;


    orderInWeek:number;


    name:string;


    nameVn?:string;


    teachingHours?:number;


    labHours?:number;


    selfStudyHours?:number;


    topicType?:string;


    teachingMethod?:string;


    learningActivity?:string;


    resources?:string;


    /** Content-table weight (for example one lecture session). */
    contentWeight?:string | number;


    /** Original I / T / U teaching level from the content table. */
    teachingLevel?:string;


    /** Backward-compatible parser name for the content-table level. */
    contentLevel?:string;


    notes?:string;

}



export interface AssessmentImportData {


    name:string;


    nameVn?:string;


    assessmentType:string;


    weightPercent:number;


    minScore?:number;


    maxScore?:number;


    orderIndex:number;

}




export interface WeeklyActivityItem {


    week:number;


    topic?:string;


    clo?:string;


    assessments?:string;


    learningActivities?:string;


    resources?:string;

}





export interface ReadingItem {


    title:string;


    author?:string;


    publisher?:string;


    year?:number;


    type?:string;

}




export interface CloPloMappingItem {


    cloCode:string;


    ploCode:string;


    value:string;


    contributionWeight?:number;

}



export interface TopicCloMappingItem {


    topicIndex:number;


    cloCode:string;

}



export interface AssessmentCloMappingItem {


    assessmentIndex:number;


    cloCode:string;


    percentage:number;

}




/**
 * Match backend:
 *
 * SyllabusImportData
 */
export interface SyllabusImportData {

    /**
     * Ordered section/field structure detected from the uploaded
     * syllabus template.
     *
     * Comparison must follow this structure instead of assuming
     * one fixed syllabus template.
     */
    templateSections?: SyllabusImportTemplateSection[];

    sourceCourseCode?:string;

    sourceCourseName?:string;



    courseDesignation?:string;


    courseTypes?:string;


    semester?:string;


    personResponsible?:string;


    language?:string;


    relation?:string;


    teachingMethods?:string;



    workloadTotal?:string;


    workloadContact?:string;


    workloadPrivate?:string;


    workloadStudentResponsibility?:string;



    creditPoints?:string;


    lectureCredits?:string;


    laboratoryCredits?:string;



    prerequisites?:string;


    objectives?:string;


    examForms?:string;


    examRequirements?:string;



    major?:string;


    contentNote?:string;


    assessmentPassNote?:string;


    dateRevised?:string;



    clos:CloImportData[];


    topics:TopicImportData[];


    weeklyActivities:WeeklyActivityItem[];


    assessments:AssessmentImportData[];


    readings:ReadingItem[];



    cloPloMappings:CloPloMappingItem[];


    topicCloMappings:TopicCloMappingItem[];


    assessmentCloMappings:AssessmentCloMappingItem[];


}





export interface SyllabusImportIssue {


    severity:string;


    section?:string;


    row?:number;


    field?:string;


    message:string;


}




export interface SyllabusImportPreviewResponse {


    fileName:string;


    fileType:string;


    valid:boolean;


    errorCount:number;


    warningCount:number;


    issues:SyllabusImportIssue[];


    data:SyllabusImportData | null;

}

export interface BulkSyllabusImportItem {
  startPage: number
  endPage: number
  sourceSnapshotId?: number
  sourceType?: "PDF" | "DOCX"
  preview: SyllabusImportPreviewResponse
}

export interface BulkSyllabusImportPreviewResponse {
  fileName: string
  pageCount: number
  syllabusCount: number
  sourceDocumentId?: number
  sourceType?: "PDF" | "DOCX"
  items: BulkSyllabusImportItem[]
}

export type ValidSyllabusImportPreviewResponse = SyllabusImportPreviewResponse & {
    data:SyllabusImportData;
}






export interface ConfirmSyllabusImportRequest {


    courseId:number;
    programId:number;
    cohortId:number;
    courseProgramId?:number;
    assignmentId?:number;


    data:SyllabusImportData;


    importMode:
        "CREATE"
        |
        "UPDATE";



    originalFileName?:string;


    originalFileType?:string;

}

export interface BulkConfirmSyllabusImportRequest {
    programId:number;
    cohortId:number;
    assignmentId?:number;
    sourceSnapshotId?:number;
    data:SyllabusImportData;
    importMode:"CREATE" | "UPDATE";
    originalFileName?:string;
    originalFileType?:string;
}

export type SyllabusImportMode =
    | "CREATE"
    | "UPDATE";