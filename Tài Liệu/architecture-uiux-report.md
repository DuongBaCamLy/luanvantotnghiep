# System Architecture & UI/UX Design Report
## Curriculum Management & Compliance Dashboard
### SCSE — International University, VNU-HCM

> **Document type:** Technical Design Report — Chapter 3  
> **Version:** 1.0 | **Stack:** Spring Boot 3.x · React 18 · MySQL 8  
> **Accreditation target:** ASIIN | **Roles:** ADMIN · DEAN · DEPT_HEAD · PROGRAM_COORDINATOR · INSTRUCTOR

---

## Mục lục

- [3.1 System Overview](#31-system-overview)
- [3.2 System Architecture](#32-system-architecture)
  - [3.2.1 Architectural Pattern](#321-architectural-pattern)
  - [3.2.2 Layered Architecture Diagram](#322-layered-architecture-diagram)
  - [3.2.3 Module Breakdown](#323-module-breakdown)
  - [3.2.4 REST API Design](#324-rest-api-design)
  - [3.2.5 Authentication & Authorization](#325-authentication--authorization)
  - [3.2.6 Data Flow Diagram](#326-data-flow-diagram)
  - [3.2.7 Deployment Architecture](#327-deployment-architecture)
- [3.3 UI/UX Design Specification](#33-uiux-design-specification)
  - [3.3.1 Design Philosophy](#331-design-philosophy)
  - [3.3.2 Design System — Color Palette](#332-design-system--color-palette)
  - [3.3.3 Typography](#333-typography)
  - [3.3.4 Spacing & Layout Grid](#334-spacing--layout-grid)
  - [3.3.5 Component Library](#335-component-library)
  - [3.3.6 Screen Designs by Role](#336-screen-designs-by-role)
  - [3.3.7 Navigation Architecture](#337-navigation-architecture)
  - [3.3.8 Responsive Design](#338-responsive-design)
  - [3.3.9 Accessibility](#339-accessibility)

---

## 3.1 System Overview

**SCSE Curriculum & Syllabus Management System (CSMS)** là một web application quản lý toàn bộ vòng đời của syllabus và chương trình đào tạo, phục vụ quy trình kiểm định ASIIN cho 4 ngành CS · IT · DS · Network Engineering tại International University, VNU-HCM.

### Bối cảnh nghiệp vụ

```
Trước hệ thống                          Sau hệ thống
──────────────────────────────────────  ────────────────────────────────────
Syllabus lưu rải rác trên Google Drive  Tập trung, searchable, versioned
Ma trận CLO-PLO tổng hợp thủ công Excel Auto-generate & export 1-click
Nhắc deadline qua Zalo/email riêng lẻ   Hệ thống tự nhắc, có audit trail
Không có approval workflow chuẩn        3-step workflow với full history
2 tuần tổng hợp báo cáo ASIIN          < 30 phút với dashboard + export
```

### Phạm vi hệ thống

| Trong phạm vi | Ngoài phạm vi |
|---|---|
| Quản lý syllabus (CRUD + versioning) | Hệ thống quản lý điểm |
| CLO-PLO mapping & compliance matrix | Thời khóa biểu |
| Approval workflow 3 bước | Đăng ký môn học của sinh viên |
| Dashboard KPI theo role | Mobile native app |
| Export Excel/PDF báo cáo ASIIN | Tích hợp LMS (Moodle) |

---

## 3.2 System Architecture

### 3.2.1 Architectural Pattern

**Quyết định: Layered Monolith** với tổ chức nội bộ theo domain (Package-by-Feature).

#### Phân tích lựa chọn kiến trúc

| Tiêu chí | Monolith ✅ Chọn | Microservices ❌ | Modular Monolith |
|---|---|---|---|
| Team size | 2–3 sinh viên | Cần 5+ người | Phù hợp nhưng phức tạp |
| Timeline | 1 học kỳ | 2–3 học kỳ | 1.5 học kỳ |
| Domain coupling | CLO ↔ Syllabus ↔ Approval rất chặt | Overhead giao tiếp | Cần interface rõ ràng |
| Infra complexity | Deploy 1 JAR | Docker + K8s + API GW | Tương tự Monolith |
| Debug/trace | Dễ — stack trace đơn giản | Distributed tracing phức tạp | Dễ |
| Phù hợp luận văn | ✅ Đủ sâu, dễ demo | Over-engineering | Có thể xem xét |

**Lý do chọn Monolith:**
- 11 domain trong DB có liên kết chặt chẽ (Syllabus → CLO → CLO-PLO → Assessment)
- Approval workflow cần transaction cross-domain (Syllabus + ApprovalRequest + Email)
- Team nhỏ, timeline ngắn — tránh phân tán effort vào infrastructure
- Vẫn áp dụng clean architecture nội bộ để code maintainable

---

### 3.2.2 Layered Architecture Diagram

```
╔═══════════════════════════════════════════════════════════════════╗
║                   CLIENT LAYER (Browser)                         ║
║                                                                   ║
║   ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐  ║
║   │ React SPA    │  │ Vite Builder │  │ Tailwind CSS         │  ║
║   │ TypeScript   │  │ React Query  │  │ shadcn/ui Components │  ║
║   └──────┬───────┘  └──────────────┘  └──────────────────────┘  ║
╚══════════╪══════════════════════════════════════════════════════╝
           │  HTTPS · REST/JSON · JWT Bearer Token
           │  Port 443 (Prod) / 3000 (Dev)
╔══════════╪══════════════════════════════════════════════════════╗
║          │         APPLICATION LAYER (Spring Boot)              ║
║          │                                                       ║
║  ┌───────▼─────────────────────────────────────────────────┐   ║
║  │              SECURITY LAYER                              │   ║
║  │  JwtAuthenticationFilter → SecurityContext              │   ║
║  │  @PreAuthorize (Method Security) → RBAC check           │   ║
║  └───────┬─────────────────────────────────────────────────┘   ║
║          │                                                       ║
║  ┌───────▼─────────────────────────────────────────────────┐   ║
║  │              CONTROLLER LAYER  (@RestController)         │   ║
║  │  Request mapping · DTO validation · Response wrapping    │   ║
║  │                                                          │   ║
║  │  AuthCtrl  SyllabusCtrl  MatrixCtrl  ApprovalCtrl        │   ║
║  │  CourseCtrl  PloCtrl  ExportCtrl  DashboardCtrl          │   ║
║  └───────┬─────────────────────────────────────────────────┘   ║
║          │                                                       ║
║  ┌───────▼─────────────────────────────────────────────────┐   ║
║  │              SERVICE LAYER  (@Service)                   │   ║
║  │  Business logic · Transaction management · Validation    │   ║
║  │                                                          │   ║
║  │  SyllabusService      SyllabusVersionService            │   ║
║  │  ApprovalWorkflowService   CloMappingService            │   ║
║  │  ComplianceReportService   MatrixService                │   ║
║  │  ExcelExportService        EmailNotificationService      │   ║
║  └───────┬─────────────────────────────────────────────────┘   ║
║          │                                                       ║
║  ┌───────▼─────────────────────────────────────────────────┐   ║
║  │              REPOSITORY LAYER  (Spring Data JPA)         │   ║
║  │  JPQL queries · Native queries → MySQL Views             │   ║
║  │  Pagination (Pageable) · Custom projections              │   ║
║  └───────┬─────────────────────────────────────────────────┘   ║
║          │                                                       ║
║  ┌───────▼─────────────────────────────────────────────────┐   ║
║  │         CROSS-CUTTING CONCERNS                           │   ║
║  │  GlobalExceptionHandler · AuditLogService                │   ║
║  │  ApiResponse<T> envelope · Swagger/OpenAPI docs          │   ║
║  └─────────────────────────────────────────────────────────┘   ║
╚══════════╪══════════════════════════════════════════════════════╝
           │  HikariCP Connection Pool · JDBC
           │  Port 3306
╔══════════╪══════════════════════════════════════════════════════╗
║          │         DATA LAYER (MySQL 8)                         ║
║          │                                                       ║
║  ┌───────▼─────────────────────────────────────────────────┐   ║
║  │  28 Tables (utf8mb4) · 11 Domain                        │   ║
║  │  4 Analytical Views · Stored constraints                 │   ║
║  │  ~1,900 rows seed data từ Program Specification PDF      │   ║
║  └─────────────────────────────────────────────────────────┘   ║
╚═══════════════════════════════════════════════════════════════════╝

EXTERNAL SERVICES:
  ┌───────────────┐    ┌───────────────┐    ┌─────────────────┐
  │  SMTP Server  │    │  File Storage │    │  (Future) SSO   │
  │  (Email notif)│    │  (Export tmp) │    │  Google Worksp. │
  └───────────────┘    └───────────────┘    └─────────────────┘
```

---

### 3.2.3 Module Breakdown

Toàn bộ backend được tổ chức theo **Package-by-Feature**, mỗi feature tự chứa đầy đủ các layer của mình:

```
src/main/java/edu/iu/scse/curriculum/
│
├── CurriculumApplication.java              ← Entry point
│
├── auth/                                   ← DOMAIN 1 (Auth)
│   ├── controller/   AuthController.java
│   ├── service/      AuthService.java, JwtService.java
│   ├── dto/          LoginRequest, LoginResponse, TokenRefreshRequest
│   ├── security/     JwtAuthFilter.java, SecurityConfig.java
│   └── entity/       (uses user_account table)
│
├── user/                                   ← DOMAIN 1 (User Management)
│   ├── controller/   UserController.java
│   ├── service/      UserService.java
│   ├── repository/   UserRepository.java
│   ├── dto/          UserDto, CreateUserRequest, UpdateUserRequest
│   ├── mapper/       UserMapper.java       (MapStruct)
│   └── entity/       UserAccount.java
│
├── organization/                           ← DOMAIN 2 (Dept + Instructor)
│   ├── controller/   DepartmentController, InstructorController
│   ├── service/      DepartmentService, InstructorService
│   ├── repository/   DepartmentRepository, InstructorRepository
│   ├── dto/          DepartmentDto, InstructorDto, InstructorSummaryDto
│   ├── mapper/       DepartmentMapper, InstructorMapper
│   └── entity/       Department, Instructor
│
├── program/                                ← DOMAIN 3 (Major/Program/Cohort)
│   ├── controller/   ProgramController, CohortController
│   ├── service/      ProgramService, CohortService
│   ├── repository/   ProgramRepository, CohortRepository
│   ├── dto/          ProgramDto, ProgramDetailDto, CohortDto
│   ├── mapper/       ProgramMapper
│   └── entity/       Major, ProgramType, Program, Cohort
│
├── plo/                                    ← DOMAIN 4 (PLO với versioning)
│   ├── controller/   PloController
│   ├── service/      PloService
│   ├── repository/   PloRepository
│   ├── dto/          PloDto, CreatePloRequest
│   ├── mapper/       PloMapper
│   └── entity/       Plo
│
├── course/                                 ← DOMAIN 5 (Course + Relationships)
│   ├── controller/   CourseController
│   ├── service/      CourseService
│   ├── repository/   CourseRepository, CourseRelationshipRepository
│   ├── dto/          CourseDto, CourseDetailDto, CourseInProgramDto
│   ├── mapper/       CourseMapper
│   └── entity/       Course, CourseType, CourseProgram, CourseRelationship
│
├── syllabus/                               ← DOMAIN 6+7+8+9 (CORE MODULE)
│   ├── controller/
│   │   ├── SyllabusController.java         (CRUD + versioning + submit)
│   │   ├── CloController.java              (CLO management)
│   │   ├── TopicController.java            (weekly plan)
│   │   └── AssessmentController.java       (assessment components)
│   ├── service/
│   │   ├── SyllabusService.java            (business logic chính)
│   │   ├── SyllabusVersionService.java     (clone, diff, archive)
│   │   ├── CloService.java
│   │   └── CloMappingService.java          (CLO-PLO logic)
│   ├── repository/
│   │   ├── SyllabusRepository.java         (+ custom JPQL queries)
│   │   ├── CloRepository.java
│   │   ├── CloPloMappingRepository.java
│   │   ├── TopicRepository.java
│   │   └── AssessmentComponentRepository.java
│   ├── dto/
│   │   ├── request/  CreateSyllabusRequest, UpdateSyllabusRequest,
│   │   │             CreateCloRequest, CloPloMappingRequest
│   │   └── response/ SyllabusDto, SyllabusDetailDto, CloDto,
│   │                 TopicDto, AssessmentDto, DiffResponse
│   ├── mapper/       SyllabusMapper, CloMapper
│   └── entity/       Syllabus, Clo, CloPloMapping, Topic, TopicClo,
│                     AssessmentComponent, AssessmentClo, Book, SyllabusBook
│
├── approval/                               ← DOMAIN 10 (3-step workflow)
│   ├── controller/   ApprovalController
│   ├── service/      ApprovalWorkflowService
│   ├── repository/   ApprovalRepository
│   ├── dto/          ApprovalQueueItemDto, ApprovalDecisionRequest,
│   │                 ApprovalHistoryDto
│   ├── mapper/       ApprovalMapper
│   └── entity/       ApprovalRequest
│
├── compliance/                             ← Cross-domain: Dashboard + Matrix
│   ├── controller/   ComplianceController, DashboardController
│   ├── service/
│   │   ├── MatrixService.java              (query v_clo_plo_matrix view)
│   │   └── ComplianceReportService.java    (query v_program_compliance_summary)
│   ├── repository/   ComplianceViewRepository
│   └── dto/          MatrixRowDto, ComplianceSummaryDto, DashboardKpiDto,
│                     GapAnalysisDto
│
├── export/                                 ← Cross-cutting: File generation
│   ├── controller/   ExportController
│   ├── service/
│   │   ├── ExcelExportService.java         (Apache POI)
│   │   └── PdfExportService.java           (iText 7)
│   └── dto/          ExportRequest
│
├── notification/                           ← Cross-cutting: Email
│   └── service/      EmailNotificationService.java   (Spring Mail)
│
└── common/                                 ← Shared infrastructure
    ├── exception/    GlobalExceptionHandler, ResourceNotFoundException,
    │                 ValidationException, AccessDeniedException
    ├── dto/          ApiResponse<T>, PageResponse<T>, ErrorDetail
    ├── enums/        Role, SyllabusStatus, ApprovalStep, ApprovalStatus,
    │                 BloomTaxonomy, CloPloLevel, CourseRelationType
    ├── audit/        AuditLogService, BaseEntity (MappedSuperclass)
    └── config/       SwaggerConfig, MailConfig, JpaAuditingConfig
```

---

### 3.2.4 REST API Design

#### Endpoint Map — Toàn hệ thống

```
BASE URL: /api/v1

══════════════════════════════════════════════════════════
 AUTH
══════════════════════════════════════════════════════════
POST   /auth/login                     Đăng nhập → JWT
POST   /auth/refresh                   Refresh access token
POST   /auth/logout                    Revoke token

══════════════════════════════════════════════════════════
 PROGRAMS & PLOs
══════════════════════════════════════════════════════════
GET    /programs                       Danh sách chương trình
GET    /programs/{id}                  Chi tiết chương trình
GET    /programs/{id}/courses          Môn học trong CTĐT
GET    /programs/{id}/plos             Danh sách PLO
GET    /programs/{id}/compliance       Compliance summary (dashboard)

══════════════════════════════════════════════════════════
 COURSES
══════════════════════════════════════════════════════════
GET    /courses                        Danh sách môn học (filterable)
GET    /courses/{id}                   Chi tiết môn học
GET    /courses/{id}/syllabi           Tất cả version syllabus
GET    /courses/{id}/syllabi/current   Version đang active

══════════════════════════════════════════════════════════
 SYLLABI  (CORE)
══════════════════════════════════════════════════════════
GET    /syllabi                        Danh sách (filter: status, program, semester)
POST   /syllabi                        Tạo draft mới
GET    /syllabi/{id}                   Chi tiết đầy đủ (for editor)
PUT    /syllabi/{id}                   Cập nhật draft
DELETE /syllabi/{id}                   Xóa draft (chỉ DRAFT status)
POST   /syllabi/{id}/clone             Clone từ version này
POST   /syllabi/{id}/submit            Submit for review
POST   /syllabi/{id}/withdraw          Rút lại (SUBMITTED → DRAFT)
GET    /syllabi/{id}/diff              So sánh với version trước
GET    /syllabi/{id}/history           Lịch sử tất cả versions
GET    /syllabi/{id}/clos              CLOs của syllabus này
POST   /syllabi/{id}/clos              Thêm CLO
PUT    /syllabi/{id}/clos/{cloId}      Cập nhật CLO
DELETE /syllabi/{id}/clos/{cloId}      Xóa CLO
GET    /syllabi/{id}/topics            Weekly topics
PUT    /syllabi/{id}/topics            Batch update weekly plan
GET    /syllabi/{id}/assessments       Assessment components
PUT    /syllabi/{id}/assessments       Batch update assessments

══════════════════════════════════════════════════════════
 CLO-PLO MATRIX
══════════════════════════════════════════════════════════
GET    /matrix/program/{id}            Full matrix (for heatmap)
GET    /matrix/program/{id}/gaps       PLO gaps analysis
GET    /matrix/course/{id}             Matrix của 1 môn
GET    /matrix/program/{id}/coverage   Coverage % per PLO

══════════════════════════════════════════════════════════
 APPROVAL WORKFLOW
══════════════════════════════════════════════════════════
GET    /approvals                      Queue (filtered by role)
GET    /approvals/{id}                 Chi tiết approval request
POST   /approvals/{id}/approve         Approve bước hiện tại
POST   /approvals/{id}/reject          Reject (kèm comment)
POST   /approvals/{id}/request-revision  Yêu cầu chỉnh sửa
GET    /approvals/history              Lịch sử đã xử lý (by reviewer)

══════════════════════════════════════════════════════════
 DASHBOARD
══════════════════════════════════════════════════════════
GET    /dashboard/overview             KPIs tổng thể (cho Dean)
GET    /dashboard/pending-approvals    Queue size by step
GET    /dashboard/compliance-trend     Compliance over time
GET    /dashboard/my-syllabi           Syllabi của GV hiện tại

══════════════════════════════════════════════════════════
 EXPORT
══════════════════════════════════════════════════════════
GET    /export/matrix/{programId}?format=excel|pdf
GET    /export/syllabus/{id}?format=pdf
GET    /export/compliance/{programId}?format=excel
GET    /export/approval-report?semester=HK1-2024
```

#### Response Envelope chuẩn

```json
// Success response
{
  "success": true,
  "message": "Syllabus submitted successfully",
  "data": { "id": 87, "status": "SUBMITTED", "version": 3 },
  "timestamp": "2025-01-15T10:30:00"
}

// Error response
{
  "success": false,
  "message": "Syllabus validation failed",
  "errorCode": "SYLLABUS_VALIDATION_FAILED",
  "data": {
    "errors": [
      "CLO3 has no PLO mapping",
      "Assessment weights sum to 95%, must be 100%"
    ]
  },
  "timestamp": "2025-01-15T10:31:00"
}

// Paginated list response
{
  "success": true,
  "data": {
    "content": [...],
    "totalElements": 84,
    "totalPages": 5,
    "currentPage": 0,
    "pageSize": 20
  }
}
```

---

### 3.2.5 Authentication & Authorization

#### JWT Flow

```
1. LOGIN
   Client ──POST /auth/login {email, password}──► Server
   Server validates credential, generates:
     accessToken  (JWT, expires 24h)
     refreshToken (opaque, expires 7d, stored in DB)
   Server ◄── { accessToken, refreshToken, user: {id, name, roles} }

2. AUTHENTICATED REQUEST
   Client ──GET /api/v1/syllabi
           Authorization: Bearer <accessToken> ──► Server
   JwtAuthFilter:
     → parse token → validate signature & expiry
     → load UserDetails → set SecurityContext
     → proceed to Controller

3. TOKEN REFRESH
   Client ──POST /auth/refresh {refreshToken} ──► Server
   Server validates refreshToken in DB → issues new accessToken

4. ACCESS DENIED
   Server returns 403 with errorCode: ACCESS_DENIED
```

#### RBAC Permission Matrix

```
                      ADMIN  DEAN  DEPT_HEAD  PROG_COORD  INSTRUCTOR
─────────────────────────────────────────────────────────────────────
VIEW dashboard          ✅     ✅      ✅          ✅          ✅*
VIEW compliance matrix  ✅     ✅      ✅          ✅          ✅
CREATE syllabus         ✅     ❌      ❌          ❌          ✅*
EDIT syllabus           ✅     ❌      ❌          ❌          ✅*
SUBMIT syllabus         ✅     ❌      ❌          ❌          ✅*
APPROVE step 1          ✅     ❌      ✅*         ❌          ❌
APPROVE step 2          ✅     ❌      ❌          ✅          ❌
APPROVE step 3 (final)  ✅     ✅      ❌          ❌          ❌
MANAGE users            ✅     ❌      ❌          ❌          ❌
EXPORT reports          ✅     ✅      ✅          ✅          ❌
MANAGE programs/PLOs    ✅     ✅      ❌          ✅          ❌

* = scoped to own data (own syllabus / own department)
```

---

### 3.2.6 Data Flow Diagram

#### Luồng Syllabus Submission & Approval

```
INSTRUCTOR              SYSTEM                 DEPT_HEAD        PROG_COORD      DEAN
    │                     │                        │                │              │
    │──create draft──────►│                        │                │              │
    │◄──syllabusId=87─────│                        │                │              │
    │                     │                        │                │              │
    │──clone(87)──────────►│                        │                │              │
    │◄──syllabusId=88(v3)──│                        │                │              │
    │                     │                        │                │              │
    │──edit CLOs, topics──►│                        │                │              │
    │──edit assessments───►│                        │                │              │
    │                     │                        │                │              │
    │──submit(88)─────────►│ validate()             │                │              │
    │                     │ status→SUBMITTED        │                │              │
    │                     │ create ApprovalReq(S1)  │                │              │
    │                     │──────email notification──►│               │              │
    │◄──202 Accepted───────│                        │                │              │
    │                     │                        │                │              │
    │                     │                        │──review──────► │              │
    │                     │                        │──approve(S1)───►│              │
    │                     │ status→UNDER_REVIEW    │                │              │
    │                     │ create ApprovalReq(S2)──────────email────►│              │
    │                     │                        │                │              │
    │                     │                        │                │─approve(S2)──►│
    │                     │ create ApprovalReq(S3)──────────────────────────email───►│
    │                     │                        │                │              │
    │                     │                        │                │              │─approve(S3)
    │                     │ status→APPROVED        │                │              │
    │                     │ is_current=TRUE (88)   │                │              │
    │                     │ is_current=FALSE (old) │                │              │
    │◄──email: APPROVED───│                        │                │              │
```

---

### 3.2.7 Deployment Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    Production Environment                        │
│                                                                 │
│  ┌────────────────┐         ┌──────────────────────────────┐   │
│  │   Nginx        │         │  Spring Boot App Server      │   │
│  │ (Reverse Proxy)│────────►│  Port 8080                   │   │
│  │  Port 80/443   │         │  JVM: OpenJDK 17             │   │
│  │  SSL termination│        │  RAM: 512MB min              │   │
│  │  Static files  │         │  curriculum-app.jar          │   │
│  └────────────────┘         └────────────┬─────────────────┘   │
│                                          │                       │
│                             ┌────────────▼─────────────────┐   │
│                             │  MySQL 8 Server              │   │
│                             │  Port 3306                   │   │
│                             │  DB: curriculum_iu           │   │
│                             │  utf8mb4_unicode_ci          │   │
│                             │  Daily backup → /backup/     │   │
│                             └──────────────────────────────┘   │
│                                                                 │
│  Development Environment:                                        │
│  localhost:3000 (React Vite) ──► localhost:8080 (Spring Boot)  │
│  CORS configured for dev origin                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 3.3 UI/UX Design Specification

### 3.3.1 Design Philosophy

Hệ thống phục vụ người dùng có background học thuật (giảng viên, trưởng khoa) — **không phải kỹ sư phần mềm**. Design cần ưu tiên:

| Nguyên tắc | Ứng dụng cụ thể |
|---|---|
| **Clarity over cleverness** | Label rõ ràng, không viết tắt khó hiểu |
| **Progressive disclosure** | Form syllabus chia tab — không hiện tất cả một lúc |
| **Status always visible** | Badge trạng thái syllabus luôn hiển thị |
| **Actionable errors** | "CLO3 chưa có PLO mapping — click để sửa" thay vì "Error 422" |
| **Academic familiarity** | Layout quen thuộc với table/form, không quá flashy |
| **Data density** | Dashboard cần dense information — người dùng là quản lý |

**Design system reference:** Tetradic giữa tông xanh học thuật (IU brand) và trắng/xám trung tính — tham khảo Notion, Linear, Vercel Dashboard.

---

### 3.3.2 Design System — Color Palette

#### Primary Brand Colors

```
┌─────────────────────────────────────────────────────────────────┐
│  PRIMARY — IU Blue (Academic authority)                         │
│                                                                 │
│  ████  --primary-900   #0C2340   Darkest — sidebar bg, headers  │
│  ████  --primary-800   #1A3A5C   Dark — hover states           │
│  ████  --primary-700   #1E4976   Main brand color              │
│  ████  --primary-600   #2563A8   Button default                │
│  ████  --primary-500   #3B82D4   Link color, focus ring        │
│  ████  --primary-400   #60A5F8   Light — selected state bg     │
│  ████  --primary-100   #DBEAFE   Very light — chip bg          │
│  ████  --primary-50    #EFF6FF   Tint — row hover, panel bg    │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  NEUTRALS — Slate (Professional, readable)                      │
│                                                                 │
│  ████  --slate-900   #0F172A   Body text primary               │
│  ████  --slate-700   #334155   Body text secondary             │
│  ████  --slate-500   #64748B   Placeholder, label muted        │
│  ████  --slate-300   #CBD5E1   Border, divider                 │
│  ████  --slate-100   #F1F5F9   Background panels               │
│  ████  --slate-50    #F8FAFC   Page background                 │
│  ████  --white       #FFFFFF   Card background                 │
└─────────────────────────────────────────────────────────────────┘
```

#### Semantic / Status Colors

```
┌─────────────────────────────────────────────────────────────────┐
│  STATUS COLORS — Syllabus & Approval States                     │
│                                                                 │
│  DRAFT            ████ bg #F1F5F9  text #475569  border #94A3B8│
│  SUBMITTED        ████ bg #EFF6FF  text #1D4ED8  border #93C5FD│
│  UNDER_REVIEW     ████ bg #FFF7ED  text #C2410C  border #FDBA74│
│  REVISION_REQ     ████ bg #FFFBEB  text #B45309  border #FCD34D│
│  APPROVED         ████ bg #F0FDF4  text #166534  border #86EFAC│
│  REJECTED         ████ bg #FEF2F2  text #991B1B  border #FCA5A5│
│  ARCHIVED         ████ bg #F8FAFC  text #94A3B8  border #CBD5E1│
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  CLO-PLO LEVEL COLORS — Heatmap (ASIIN standard)               │
│                                                                 │
│  Level I (Introduce)  ████  #FEF9C3  bg / #A16207 text        │
│  Level D (Develop)    ████  #FED7AA  bg / #C2410C text        │
│  Level A (Apply)      ████  #FCA5A5  bg / #991B1B text        │
│  No mapping           ████  #F8FAFC  bg / #CBD5E1 border      │
│  Gap (warning)        ████  #FEE2E2  bg with ⚠️ icon           │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  COMPLIANCE INDICATOR — Dashboard KPI                           │
│                                                                 │
│  High  (≥ 80%)   ████  #22C55E  (Green 500)                   │
│  Med   (50–79%)  ████  #EAB308  (Yellow 500)                  │
│  Low   (< 50%)   ████  #EF4444  (Red 500)                     │
│                                                                 │
│  Progress bar background:  #E2E8F0                             │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  FEEDBACK COLORS                                                │
│                                                                 │
│  Success  ████  #22C55E  bg: #F0FDF4  border: #86EFAC         │
│  Warning  ████  #EAB308  bg: #FEFCE8  border: #FDE047         │
│  Error    ████  #EF4444  bg: #FEF2F2  border: #FCA5A5         │
│  Info     ████  #3B82F6  bg: #EFF6FF  border: #93C5FD         │
└─────────────────────────────────────────────────────────────────┘
```

#### Tailwind CSS Configuration

```javascript
// tailwind.config.js
module.exports = {
  theme: {
    extend: {
      colors: {
        brand: {
          900: '#0C2340',
          800: '#1A3A5C',
          700: '#1E4976',
          600: '#2563A8',
          500: '#3B82D4',
          400: '#60A5F8',
          100: '#DBEAFE',
          50:  '#EFF6FF',
        },
        // Status colors mapped to Tailwind utilities
        status: {
          draft:     { bg: '#F1F5F9', text: '#475569', border: '#94A3B8' },
          submitted: { bg: '#EFF6FF', text: '#1D4ED8', border: '#93C5FD' },
          approved:  { bg: '#F0FDF4', text: '#166534', border: '#86EFAC' },
          rejected:  { bg: '#FEF2F2', text: '#991B1B', border: '#FCA5A5' },
        },
        matrix: {
          I: { bg: '#FEF9C3', text: '#A16207' },  // Introduce
          D: { bg: '#FED7AA', text: '#C2410C' },  // Develop
          A: { bg: '#FCA5A5', text: '#991B1B' },  // Apply
        }
      }
    }
  }
}
```

---

### 3.3.3 Typography

#### Font Stack

```
PRIMARY FONT:    Inter (Google Fonts)
  — Sans-serif, neutral, excellent legibility at small sizes
  — Hỗ trợ tiếng Việt đầy đủ với dấu đọc rõ ràng
  — Được dùng bởi: Linear, Vercel, Notion, Figma

MONOSPACE FONT:  JetBrains Mono
  — Dùng cho: course codes (IT013IU), version labels (v2024.1),
    CLO codes (CLO1), PLO codes (PLO3)

FALLBACK STACK:
  font-family: 'Inter', 'Segoe UI', system-ui, -apple-system, sans-serif;
```

#### Type Scale

```
Size Token    px    rem     Weight    Usage
──────────────────────────────────────────────────────────────────
text-xs       12    0.75    400       Helper text, timestamps, caption
text-sm       14    0.875   400/500   Table data, form labels, badges
text-base     16    1       400       Body text, descriptions
text-lg       18    1.125   500/600   Card titles, section headers
text-xl       20    1.25    600       Page sub-headers
text-2xl      24    1.5     700       Page titles
text-3xl      30    1.875   700       Dashboard KPI numbers
text-4xl      36    2.25    800       Hero metrics (compliance %)

LINE HEIGHT:
  text-xs → text-sm : leading-4 (16px)
  text-base          : leading-6 (24px)
  text-lg → text-2xl : leading-tight (1.25)
  KPI numbers        : leading-none (1)

LETTER SPACING:
  Uppercase labels   : tracking-wide (0.025em)
  KPI numbers        : tracking-tight (-0.025em)
```

#### Typography Usage Examples

```
Page Title:        text-2xl font-bold text-slate-900
Section Header:    text-lg font-semibold text-slate-800
Table Header:      text-xs font-semibold uppercase tracking-wide text-slate-500
Table Cell Data:   text-sm text-slate-700
Course Code:       text-sm font-mono font-medium text-brand-700
Status Badge:      text-xs font-semibold uppercase tracking-wide
KPI Number:        text-3xl font-bold tabular-nums
KPI Label:         text-sm font-medium text-slate-500
Help Text:         text-xs text-slate-400 italic
Error Message:     text-sm text-red-600
```

---

### 3.3.4 Spacing & Layout Grid

#### Spacing Scale (Tailwind default — 4px base unit)

```
Token    px    Usage
─────────────────────────────────────────────────
p-1      4px   Icon padding inside badge
p-2      8px   Tight padding (chip, small button)
p-3      12px  Button padding (horizontal)
p-4      16px  Card inner padding default
p-5      20px  Form field padding
p-6      24px  Section padding, card padding
p-8      32px  Page section vertical rhythm
p-10     40px  Large section gaps
p-16     64px  Page top/bottom padding

BORDER RADIUS:
rounded       4px    Input fields, small buttons
rounded-md    6px    Cards, dropdowns, badges
rounded-lg    8px    Modals, large cards
rounded-xl    12px   Feature cards, banners
rounded-full  999px  Avatar, pill badges
```

#### Layout Grid

```
SIDEBAR LAYOUT (Admin / Dean / Manager roles):
┌──────────────────────────────────────────────────────────┐
│ Sidebar 240px │ Main Content (fluid)                      │
│ (fixed)       │ max-w: 1280px, auto-center                │
│               │ padding: px-6 py-8                        │
└───────────────┴──────────────────────────────────────────┘

CONTENT GRID:
12-column grid, gap-6 (24px)
Card layout:  col-span-4 (1/3) or col-span-6 (1/2) or col-span-12

BREAKPOINTS:
sm: 640px   → Tablet: sidebar collapses to icon-only
md: 768px   → Tablet landscape: full sidebar
lg: 1024px  → Desktop: standard layout
xl: 1280px  → Wide desktop: max content width

TABLE LAYOUT:
Sticky header, scrollable body
Min column width: 120px
Action column: 160px (right-aligned)
```

---

### 3.3.5 Component Library

Sử dụng **shadcn/ui** làm base component library (built on Radix UI primitives + Tailwind). Các component custom thêm cho domain-specific:

#### Base Components (shadcn/ui)

```
Button          → variant: default | outline | ghost | destructive | link
Input           → với error state và helper text
Select          → searchable dropdown (Combobox pattern)
Table           → với sorting, pagination header
Dialog/Modal    → confirmation, review modal
Tabs            → syllabus editor sections
Badge           → status display
Card            → dashboard panels
Toast           → success/error notifications
Skeleton        → loading states
Tooltip         → help text on hover
Breadcrumb      → navigation context
```

#### Custom Domain Components

```typescript
// <StatusBadge status="APPROVED" />
// Renders color-coded badge cho syllabus status

// <SyllabusStatusBadge />
const statusConfig = {
  DRAFT:              { label: 'Draft',           class: 'bg-slate-100 text-slate-600' },
  SUBMITTED:          { label: 'Submitted',        class: 'bg-blue-50 text-blue-700' },
  UNDER_REVIEW:       { label: 'Under Review',     class: 'bg-orange-50 text-orange-700' },
  REVISION_REQUESTED: { label: 'Needs Revision',   class: 'bg-yellow-50 text-yellow-700' },
  APPROVED:           { label: 'Approved',         class: 'bg-green-50 text-green-700' },
  REJECTED:           { label: 'Rejected',         class: 'bg-red-50 text-red-700' },
  ARCHIVED:           { label: 'Archived',         class: 'bg-slate-50 text-slate-400' },
}

// <MatrixCell level="D" />
// Renders heatmap cell với màu theo I/D/A

// <ComplianceBar value={51.8} total={100} />
// Progress bar với color thresholds: red/yellow/green

// <ApprovalStepIndicator currentStep={2} />
// Step 1 ✅ → Step 2 🔵 → Step 3 ⬜

// <ValidationBanner errors={[...]} />
// Banner cảnh báo lỗi trước khi submit

// <DiffHighlight before="..." after="..." />
// Hiển thị thay đổi giữa 2 version với màu xanh/đỏ
```

---

### 3.3.6 Screen Designs by Role

#### Screen 1: Dean/Admin Dashboard

**URL:** `/dashboard` | **Role:** DEAN, ADMIN

```
┌──────────────────────────────────────────────────────────────────┐
│  ■ SCSE  Curriculum Dashboard    Semester: HK1 2024-25 [▼]      │
│                                             [🔔 8]  [Dr. Sinh ▼]│
├──────────┬───────────────────────────────────────────────────────┤
│          │  Good morning, Assoc. Prof. Nguyen Van Sinh           │
│  🏠 Home │  ASIIN Compliance Overview                            │
│          │                                                        │
│  📚 Programs│  ┌───────────────┐ ┌───────────────┐ ┌──────────┐│
│          │  │    CS 2021     │ │    IT 2021     │ │ DS 2021  ││
│  📝 Syllabi│ │   ██████░░░░  │ │   ████░░░░░░  │ │ ████░░░░ ││
│          │  │    51.8%       │ │    34.5%       │ │  36.5%   ││
│  ✅ Queue │  │  29 / 56      │ │  29 / 84      │ │ 19 / 52  ││
│  [8]     │  │  🟡 Medium    │ │  🔴 Low       │ │ 🔴 Low   ││
│          │  └───────────────┘ └───────────────┘ └──────────┘│
│  📊 Matrix│                                                      │
│          │  ─────────────────────────────────────────────────── │
│  📤 Export│  Pending Your Action (Step 3 — Dean Approval)       │
│          │  ┌─────────────────────────────────────────────────┐ │
│  ⚙️ Admin │  │ IT013IU  DSA      Dr. Nguyen A   2d ago  [Review]│
│          │  │ CS001IU  SE       Dr. Le B        1d ago  [Review]│
│          │  │ DS012IU  ML       Dr. Tran C      3d ago [⚠Urgent]│
│          │  └─────────────────────────────────────────────────┘ │
│          │  [See all 3 pending →]                               │
│          │                                                        │
│          │  ─────────────────────────────────────────────────── │
│          │  📅 Deadline: Syllabus Submission — Jan 20, 2025     │
│          │  3 days remaining                                      │
│          │  CS:  ████████████░░░░  18/56 not yet submitted      │
│          │  IT:  ██████░░░░░░░░░░  55/84 not yet submitted      │
└──────────┴───────────────────────────────────────────────────────┘
```

---

#### Screen 2: Syllabus Editor (Instructor View)

**URL:** `/syllabi/{id}/edit` | **Role:** INSTRUCTOR

```
┌──────────────────────────────────────────────────────────────────┐
│  ← Back to My Syllabi                                           │
│  IT013IU — Data Structures and Algorithms     v3 · DRAFT       │
│                                    [Save Draft] [Preview] [Submit ▶]│
├──────────────────────────────────────────────────────────────────┤
│  ① General Info  ② CLOs ●  ③ Weekly Plan  ④ Assessment  ⑤ Books │
│  ─────────────── tab underline on active ─────────────────────── │
├──────────────────────────────────────────────────────────────────┤
│  [TAB: ② CLOs — Active]                                         │
│                                                                  │
│  ⚠️  1 CLO has no PLO mapping — cannot submit yet               │
│                                                        [+ Add CLO]│
│  ┌───┬───────────────────────────────┬──────────┬──────────────┐│
│  │ # │ Description                   │ Bloom    │ PLO Mappings ││
│  ├───┼───────────────────────────────┼──────────┼──────────────┤│
│  │ 1 │ Understand basic data struct..│ Comprehension│ PLO2·D    ││
│  │   │                               │          │ PLO3·A   [✏️]││
│  ├───┼───────────────────────────────┼──────────┼──────────────┤│
│  │ 2 │ Analyze time/space complexity │ Analysis │ PLO1·D   [✏️]││
│  ├───┼───────────────────────────────┼──────────┼──────────────┤│
│  │ 3 │ Implement algorithms for real │ Application│ ⚠️ None   ││
│  │   │ world problems                │          │ [+ Map PLO]  ││
│  └───┴───────────────────────────────┴──────────┴──────────────┘│
│                                                                  │
│  ─── PLO Mapping Editor (inline modal) ─────────────────────── │
│  CLO 3: Implement algorithms...                                  │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  PLO1 ○I ○D ●A   PLO2 ○I ●D ○A   PLO3 ○I ○D ○A       │    │
│  │  PLO4 ○I ○D ○A   PLO5 ○I ○D ○A   PLO6 ○I ○D ○A       │    │
│  │                                      [Cancel] [Save]    │    │
│  └─────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────┘
```

---

#### Screen 3: CLO-PLO Matrix Heatmap

**URL:** `/matrix` | **Role:** DEAN, ADMIN, DEPT_HEAD, PROGRAM_COORD

```
┌──────────────────────────────────────────────────────────────────┐
│  CLO-PLO Compliance Matrix                                       │
│  Program: [CS 2021 ▼]  Semester: [All ▼]  Type: [Compulsory ▼] │
│                               [Export Excel] [Export PDF]        │
├──────────────────────────────────────────────────────────────────┤
│  Legend:  [I] Introduce  [D] Develop  [A] Apply  [⚠] Gap       │
│           ████ Yellow     ████ Orange   ████ Red   ░░░ None     │
├────────────────────┬──────┬──────┬──────┬──────┬──────┬────────┤
│ Course             │ PLO1 │ PLO2 │ PLO3 │ PLO4 │ PLO5 │ PLO6  │
├────────────────────┼──────┼──────┼──────┼──────┼──────┼────────┤
│ IT013IU  DSA  S3   │  ░D░ │  ░A░ │      │      │  ░I░ │       │
│ IT079IU  DB   S4   │  ░I░ │      │  ░D░ │      │      │  ░A░  │
│ IT076IU  SE   S6   │      │  ░D░ │  ░A░ │  ░D░ │      │  ░I░  │
│ CS001IU  Algo S3   │  ░A░ │  ░D░ │      │      │      │       │
│ IT094IU  ISM  S5   │      │  ░I░ │      │  ░A░ │  ░D░ │       │
│ ...                │  ... │  ... │  ... │  ... │  ... │  ...  │
├────────────────────┼──────┼──────┼──────┼──────┼──────┼────────┤
│ Coverage           │ 100% │  92% │  71% │  85% │  64% │ ⚠48%  │
└────────────────────┴──────┴──────┴──────┴──────┴──────┴────────┘

  ⚠️ PLO6 coverage is below 50% — only covered by Apply level in 2 courses
     Consider adding PLO6 mapping to elective courses in Semester 6-7.
```

---

#### Screen 4: Approval Queue

**URL:** `/approvals` | **Role:** DEPT_HEAD, PROGRAM_COORD, DEAN

```
┌──────────────────────────────────────────────────────────────────┐
│  Review Queue                                                    │
│  Your role: Department Head — SCSE                              │
│  Step 1 of 3 — Dept Head Review                                 │
├──────────────────────────────────────────────────────────────────┤
│  [All ▼]  [Pending ●5]  [Reviewed]    Search: [______________]  │
│                                            Sort: [Oldest first ▼]│
├──────────────────────────────────────────────────────────────────┤
│  ⚠️ 2 items overdue (> 5 business days)                          │
├────────┬─────────────────────────┬──────────┬──────────┬────────┤
│ Course │ Instructor              │ Submitted│ Waiting  │ Action │
├────────┼─────────────────────────┼──────────┼──────────┼────────┤
│IT013IU │ Dr. Nguyen Van A        │ Jan 10   │ 7d 🔴    │[Review]│
│CS001IU │ Dr. Le Thi B           │ Jan 12   │ 5d 🔴    │[Review]│
│IT079IU │ Dr. Tran Van C         │ Jan 14   │ 3d 🟡    │[Review]│
│DS012IU │ Dr. Pham D             │ Jan 15   │ 2d       │[Review]│
│IT076IU │ Dr. Hoang E            │ Jan 16   │ 1d       │[Review]│
└────────┴─────────────────────────┴──────────┴──────────┴────────┘

── Review Modal (opens on [Review]) ──────────────────────────────
│  IT013IU — Data Structures and Algorithms                       │
│  Instructor: Dr. Nguyen Van A  |  Version 3  |  Submitted Jan 10│
│  ─────────────────────────────────────────────────────────────  │
│  [View Full Syllabus ↗]   [View Changes from v2 →]             │
│                                                                 │
│  Changes from previous version:                                 │
│  • CLO3 description updated                                     │
│  • Assessment: Final exam weight 40% → 35%, Project 10% → 15% │
│  • Week 14 topic added: "Advanced Graph Algorithms"             │
│                                                                 │
│  Your comment (optional):                                       │
│  ┌────────────────────────────────────────────────────────┐    │
│  │                                                        │    │
│  └────────────────────────────────────────────────────────┘    │
│                                                                 │
│  [Reject]  [Request Revision]         [✅ Approve & Forward]   │
└──────────────────────────────────────────────────────────────────┘
```

---

#### Screen 5: Instructor — My Syllabi

**URL:** `/my-syllabi` | **Role:** INSTRUCTOR

```
┌──────────────────────────────────────────────────────────────────┐
│  My Syllabi — Semester HK1 2024-2025                            │
│                                         Deadline: Jan 20 (3 days)│
├──────────────────────────────────────────────────────────────────┤
│ Assigned courses (3)                                             │
│                                                                  │
│ ┌────────────────────────────────────────────────────────────┐  │
│ │ IT013IU — Data Structures and Algorithms       4 credits   │  │
│ │ Status: SUBMITTED  |  v3  |  Submitted Jan 13             │  │
│ │ → Waiting: Dept Head Review (Step 1/3)                    │  │
│ │                                          [View] [History]  │  │
│ └────────────────────────────────────────────────────────────┘  │
│                                                                  │
│ ┌────────────────────────────────────────────────────────────┐  │
│ │ IT079IU — Principles of Database Management   4 credits    │  │
│ │ Status: DRAFT  |  v2  |  Last saved Jan 14 10:30am        │  │
│ │ ⚠️ 2 issues: CLO2 no PLO mapping, Assessment ≠ 100%        │  │
│ │                                 [Continue Editing] [History]│  │
│ └────────────────────────────────────────────────────────────┘  │
│                                                                  │
│ ┌────────────────────────────────────────────────────────────┐  │
│ │ IT076IU — Software Engineering                4 credits    │  │
│ │ Status: REVISION_REQUESTED  |  v3  |  Returned Jan 12     │  │
│ │ 💬 "Please add more detail to CLO3 and update Week 15 topic"│  │
│ │                                          [Revise] [History] │  │
│ └────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────┘
```

---

### 3.3.7 Navigation Architecture

```
SIDEBAR NAVIGATION — hiển thị theo role

ALL ROLES:
  🏠  Dashboard
  📝  Syllabi / My Syllabi    (label khác theo role)
  📊  CLO-PLO Matrix
  🔔  Notifications           (badge count)

INSTRUCTOR thêm:
  📚  My Courses

DEPT_HEAD / PROG_COORD / DEAN thêm:
  ✅  Approval Queue          (badge count)
  👥  Instructors
  📈  Reports

DEAN / ADMIN thêm:
  🎓  Programs & PLOs
  📤  Export Center

ADMIN thêm:
  ⚙️  System Settings
  👤  User Management
  📋  Audit Log

HEADER (top bar):
  Left:   Logo + System Name
  Center: Current semester selector
  Right:  🔔 Notifications badge | User avatar + name + role | Logout
```

---

### 3.3.8 Responsive Design

```
BREAKPOINTS & BEHAVIOR:

Desktop (≥1024px):  Full sidebar (240px) + content area
  → All features fully accessible
  → Matrix hiển thị full table

Tablet (768-1023px): Sidebar thu lại thành icon-only (64px)
  → Hover/click icon → expand as overlay
  → Matrix: horizontal scroll
  → Forms: full width single column

Mobile (<768px): NOT primary target — nhưng vẫn cần usable
  → Sidebar ẩn → hamburger menu
  → Dashboard cards stack vertically
  → Tables: hide secondary columns, swipe for more
  → Forms: full screen modals
  → Matrix: scroll + pinned first column

PRIORITY: Desktop first (users are office workers, not mobile)
NFR yêu cầu: "dùng được trên máy tính và tablet" (SRS NFR-02.4)
```

---

### 3.3.9 Accessibility

| Tiêu chí | Yêu cầu | Thực hiện |
|---|---|---|
| Color contrast | WCAG AA (4.5:1 text) | Tất cả color pair được kiểm tra |
| Keyboard navigation | Tab order logic | Sidebar, form tabs, modal |
| Screen reader | ARIA labels | Badge, icon button, status |
| Error identification | Không chỉ dùng màu | Icon + text kèm màu |
| Focus indicator | Visible focus ring | `ring-2 ring-brand-500` |
| Form labels | Tất cả input có label | Không dùng placeholder thay label |
| Language | `lang="en"` on HTML | Hỗ trợ screen reader đọc đúng |

```html
<!-- Ví dụ StatusBadge accessible -->
<span
  role="status"
  aria-label="Syllabus status: Approved"
  class="badge badge-approved"
>
  <span aria-hidden="true">✅</span>
  Approved
</span>

<!-- Matrix cell accessible -->
<td
  role="cell"
  aria-label="PLO2 mapped at Develop level"
  class="matrix-cell matrix-cell-D"
>
  D
</td>
```

---

## Tổng kết

### Architecture Decision Summary

| Quyết định | Lựa chọn | Lý do |
|---|---|---|
| Kiến trúc | Layered Monolith | Team nhỏ, domain coupling cao |
| Tổ chức code | Package by Feature | Maintainability, mirror domain |
| Auth | JWT (stateless) | SPA-friendly, scalable |
| Authorization | RBAC + Data-level check | 5 roles với scoped data access |
| DB access | JPA + custom JPQL + Native Views | Linh hoạt, tận dụng MySQL views |
| DTO | MapStruct | Type-safe, compile-time, no reflection |
| Export | Apache POI (Excel) + iText (PDF) | Mature libraries, production-ready |

### UI/UX Design Summary

| Token | Value |
|---|---|
| Primary Font | Inter (Google Fonts) |
| Mono Font | JetBrains Mono |
| Brand Color | `#1E4976` (IU Blue) |
| Background | `#F8FAFC` (Slate 50) |
| Text Primary | `#0F172A` (Slate 900) |
| Border | `#CBD5E1` (Slate 300) |
| Radius base | `rounded-md` (6px) |
| Grid | 12-column, gap-6 (24px) |
| Sidebar width | 240px (desktop) |
| Component base | shadcn/ui + Radix UI |
| Heatmap I | `#FEF9C3` yellow-tint |
| Heatmap D | `#FED7AA` orange-tint |
| Heatmap A | `#FCA5A5` red-tint |

---

*Tài liệu: Curriculum Management & Compliance Dashboard — SCSE IU VNU-HCM*  
*Chapter 3: System Architecture & UI/UX Design | Version 1.0 | 2025*
