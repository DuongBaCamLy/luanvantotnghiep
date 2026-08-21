# Best Practices — Implementation Guide
## Curriculum Management & Compliance Dashboard
### SCSE, International University VNU-HCM

> **Phiên bản:** 1.0 | **Áp dụng cho:** Sprint 1–5 | **Stack:** Spring Boot 3.x · React 18 · MySQL 8

---

## Mục lục

1. [DTO Layer — Tại sao bắt buộc](#1-dto-layer)
2. [Spring Boot Best Practices](#2-spring-boot-best-practices)
3. [React Best Practices](#3-react-best-practices)
4. [AI-Assisted Development — Copilot & Claude](#4-ai-assisted-development)

---

## 1. DTO Layer — Tại sao bắt buộc

### 1.1 Vấn đề khi không dùng DTO

Trả Entity JPA trực tiếp ra API response là anti-pattern phổ biến nhất trong Spring Boot:

| Vấn đề | Hậu quả thực tế |
|---|---|
| Lộ field nhạy cảm | `password_hash`, internal FK bị trả ra JSON |
| Lazy loading | `LazyInitializationException` hoặc N+1 query |
| Circular reference | `syllabus → clo → syllabus` → StackOverflow khi serialize |
| Coupling DB ↔ API | Thay tên cột DB → vỡ contract với frontend |
| Không validate input riêng | Business rule lẫn với DB constraint |

### 1.2 Cấu trúc DTO layer cho project này

```
dto/
├── request/                    ← Input validation (@Valid)
│   ├── CreateSyllabusRequest
│   ├── UpdateSyllabusRequest
│   ├── CreateCloRequest
│   ├── CloPloMappingRequest
│   └── ApprovalDecisionRequest
│
├── response/                   ← Output (chọn lọc field)
│   ├── SyllabusDto             (list view: id, courseCode, status, version)
│   ├── SyllabusDetailDto       (full editor: + clos, topics, assessments)
│   ├── CloDto
│   ├── MatrixRowDto            (heatmap: courseCode + mappings[])
│   ├── ComplianceSummaryDto    (dashboard KPIs)
│   └── ApprovalQueueItemDto
│
└── mapper/                     ← MapStruct (generate code, zero reflection)
    └── SyllabusMapper
```

### 1.3 Ví dụ — Request vs Response DTO

```java
// REQUEST: có validation annotation
public class CreateCloRequest {
    @NotBlank(message = "CLO code is required")
    @Pattern(regexp = "CLO\\d+", message = "Format: CLO1, CLO2...")
    private String code;

    @NotBlank
    @Size(max = 500)
    private String description;

    @NotNull
    private BloomTaxonomy bloomLevel;    // enum: KNOWLEDGE/COMPREHENSION/APPLICATION...

    @NotEmpty(message = "At least one PLO mapping required")
    private List<CloPloMappingRequest> ploMappings;
}

// RESPONSE: chỉ expose field cần thiết
public record CloDto(
    Long   id,
    String code,
    String description,
    String bloomLevel,
    List<CloPloMappingDto> mappings,
    boolean hasPloGap          // computed field — không có trong DB
) {}
```

---

## 2. Spring Boot Best Practices

### 2.1 Project Structure — Package by Feature (không phải by Layer)

**❌ Sai — Package by Layer (anti-pattern cho project lớn):**
```
controller/SyllabusController, CourseController, ApprovalController...
service/SyllabusService, CourseService...
repository/SyllabusRepository...
```

**✅ Đúng — Package by Feature:**
```
syllabus/
  controller/SyllabusController
  service/SyllabusService, SyllabusVersionService
  repository/SyllabusRepository
  dto/request/CreateSyllabusRequest
  dto/response/SyllabusDetailDto
  mapper/SyllabusMapper
  entity/Syllabus
approval/
  controller/ApprovalController
  service/ApprovalWorkflowService
  ...
```

> **Lý do:** Khi cần sửa tính năng Syllabus, chỉ mở 1 folder — không nhảy qua lại 4 layer.

---

### 2.2 Entity Design

```java
// ✅ Dùng @MappedSuperclass cho audit fields — tránh lặp lại
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}

// ✅ Entity Syllabus — đúng cách
@Entity
@Table(name = "syllabus")
public class Syllabus extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)   // LUÔN LAZY cho @ManyToOne
    @JoinColumn(name = "course_id")
    private Course course;

    @Enumerated(EnumType.STRING)          // Dùng STRING, không dùng ORDINAL
    private SyllabusStatus status;

    @OneToMany(mappedBy = "syllabus",
               cascade = CascadeType.ALL,
               orphanRemoval = true,
               fetch = FetchType.LAZY)   // LAZY cho collections
    private List<Clo> clos = new ArrayList<>();

    // ❌ KHÔNG dùng: @Data của Lombok trên Entity (gây vòng lặp equals/hashCode)
    // ✅ DÙNG: @Getter @Setter @NoArgsConstructor
}
```

---

### 2.3 Service Layer — Transaction Management

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)   // Default read-only cho toàn class
public class SyllabusService {

    private final SyllabusRepository syllabusRepo;
    private final ApprovalWorkflowService approvalService;
    private final EmailNotificationService emailService;
    private final SyllabusMapper mapper;

    // Read operations kế thừa readOnly = true từ class
    public SyllabusDetailDto getById(Long id) {
        Syllabus s = syllabusRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Syllabus", id));
        return mapper.toDetailDto(s);
    }

    // Write operations phải ghi đè @Transactional riêng
    @Transactional                // Override: readOnly = false
    public SyllabusDto submitForApproval(Long id, UserPrincipal actor) {
        Syllabus s = syllabusRepo.findByIdWithClos(id)  // custom query tránh N+1
            .orElseThrow(() -> new ResourceNotFoundException("Syllabus", id));

        validateForSubmission(s);       // business validation
        s.setStatus(SyllabusStatus.SUBMITTED);
        approvalService.initWorkflow(s);
        emailService.notifyDeptHead(s); // gọi sau save, trong cùng transaction

        return mapper.toDto(syllabusRepo.save(s));
    }

    private void validateForSubmission(Syllabus s) {
        List<String> errors = new ArrayList<>();
        if (s.getClos().isEmpty()) errors.add("CLOs are required");
        if (s.getTopics().size() < 14) errors.add("Weekly plan: minimum 14 weeks");
        double totalWeight = s.getAssessments().stream()
            .mapToDouble(AssessmentComponent::getWeight).sum();
        if (Math.abs(totalWeight - 100.0) > 0.01)
            errors.add("Assessment weights must sum to 100%");
        s.getClos().stream()
            .filter(c -> c.getMappings().isEmpty())
            .forEach(c -> errors.add("CLO " + c.getCode() + " has no PLO mapping"));

        if (!errors.isEmpty())
            throw new ValidationException("SYLLABUS_VALIDATION_FAILED", errors);
    }
}
```

---

### 2.4 Repository — Tránh N+1 Query

```java
public interface SyllabusRepository extends JpaRepository<Syllabus, Long> {

    // ✅ Fetch clos cùng lúc để tránh N+1
    @Query("""
        SELECT DISTINCT s FROM Syllabus s
        LEFT JOIN FETCH s.clos c
        LEFT JOIN FETCH c.mappings
        WHERE s.id = :id
        """)
    Optional<Syllabus> findByIdWithClos(@Param("id") Long id);

    // ✅ Dùng Projection cho list view — chỉ lấy field cần thiết
    @Query("""
        SELECT new edu.iu.scse.curriculum.syllabus.dto.response.SyllabusDto(
            s.id, s.status, s.versionNumber, s.versionLabel,
            c.code, c.name, i.fullName
        )
        FROM Syllabus s
        JOIN s.course c
        JOIN s.assignedInstructor i
        WHERE s.status = :status
        ORDER BY s.updatedAt DESC
        """)
    List<SyllabusDto> findAllByStatus(@Param("status") SyllabusStatus status);

    // ✅ Cho view phân tích — query thẳng vào MySQL view
    @Query(value = "SELECT * FROM v_program_compliance_summary WHERE program_id = :id",
           nativeQuery = true)
    List<ComplianceSummaryProjection> getComplianceSummary(@Param("id") Long programId);
}
```

---

### 2.5 Global Exception Handling

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Lỗi resource không tồn tại → 404
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(ex.getErrorCode(), ex.getMessage()));
    }

    // Lỗi validation business logic → 422
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<List<String>>> handleValidation(ValidationException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(ApiResponse.error(ex.getErrorCode(), ex.getDetails()));
    }

    // Lỗi @Valid (Jakarta Validation) → 400
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleBadRequest(
            MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                fe -> Objects.requireNonNullElse(fe.getDefaultMessage(), "Invalid value")
            ));
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("VALIDATION_ERROR", fieldErrors));
    }

    // Lỗi phân quyền → 403
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbidden(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error("ACCESS_DENIED", "Insufficient permissions"));
    }
}
```

---

### 2.6 Security — Data-Level RBAC

```java
// Không chỉ check role, còn phải check ownership của data
@Service
public class SyllabusSecurityService {

    // INSTRUCTOR chỉ sửa syllabus được phân công
    public boolean isAssignedInstructor(Long syllabusId, Authentication auth) {
        return syllabusRepo.existsByIdAndAssignedInstructorEmail(
            syllabusId, auth.getName()
        );
    }

    // DEPT_HEAD chỉ approve syllabus thuộc bộ môn mình
    public boolean isResponsibleDeptHead(Long approvalId, Authentication auth) {
        ApprovalRequest req = approvalRepo.findById(approvalId).orElseThrow();
        Long deptId = req.getSyllabus().getCourse().getDepartment().getId();
        return instructorRepo.existsByEmailAndDepartmentId(auth.getName(), deptId);
    }
}

// Dùng trong Controller:
@PutMapping("/{id}")
@PreAuthorize("hasRole('INSTRUCTOR') and " +
              "@syllabusSecurityService.isAssignedInstructor(#id, authentication)")
public ResponseEntity<SyllabusDetailDto> update(@PathVariable Long id,
                                                 @Valid @RequestBody UpdateSyllabusRequest req) {
    return ResponseEntity.ok(syllabusService.update(id, req));
}
```

---

### 2.7 Các quy tắc bổ sung

| # | Quy tắc | Lý do |
|---|---|---|
| BP-01 | Dùng `ddl-auto: validate` — **không bao giờ** `create` hay `update` | Schema đã có sẵn, tránh Hibernate tự sửa DB |
| BP-02 | Dùng `@Transactional` ở **Service**, không ở Controller hay Repository | Controller không có business logic; JpaRepository đã tự transactional |
| BP-03 | Dùng `record` cho DTO response (Java 17+) | Immutable, concise, auto-generates equals/hashCode |
| BP-04 | Dùng **MapStruct** cho mapping Entity ↔ DTO | Compile-time, zero reflection, type-safe |
| BP-05 | Paginate mọi list endpoint | `Pageable` + `Page<T>` — tránh trả 500 rows một lúc |
| BP-06 | Không log thông tin nhạy cảm | Password, token không được xuất hiện trong log |
| BP-07 | Tách `application.yml` thành `dev` / `prod` profiles | Config DB, email khác nhau giữa môi trường |
| BP-08 | Viết Swagger/OpenAPI annotation đầy đủ | `@Operation`, `@ApiResponse` — tài liệu API tự động |
| BP-09 | Unit test Service layer với MockMvc | Không cần khởi động Spring context đầy đủ |
| BP-10 | Dùng `@Slf4j` (Lombok) cho logging | Consistent, không cần khởi tạo Logger thủ công |

---

## 3. React Best Practices

### 3.1 Project Structure — Feature-based

```
src/
├── features/                   ← Tổ chức theo domain, mirror backend
│   ├── auth/
│   │   ├── components/LoginForm.tsx
│   │   ├── hooks/useAuth.ts
│   │   └── authSlice.ts        (Redux Toolkit hoặc Zustand)
│   ├── syllabus/
│   │   ├── components/
│   │   │   ├── SyllabusEditor/
│   │   │   │   ├── index.tsx
│   │   │   │   ├── CloTab.tsx
│   │   │   │   ├── TopicTab.tsx
│   │   │   │   └── AssessmentTab.tsx
│   │   │   ├── SyllabusCard.tsx
│   │   │   └── DiffViewer.tsx
│   │   ├── hooks/
│   │   │   ├── useSyllabus.ts      (React Query)
│   │   │   └── useSyllabusForm.ts  (React Hook Form)
│   │   └── syllabusApi.ts          (axios calls)
│   ├── matrix/
│   │   ├── components/MatrixHeatmap.tsx
│   │   └── hooks/useMatrix.ts
│   ├── approval/
│   │   └── components/ApprovalQueue.tsx
│   └── dashboard/
│       └── components/ComplianceCard.tsx
│
├── shared/                     ← Shared UI components
│   ├── components/
│   │   ├── ApiTable.tsx        (reusable paginated table)
│   │   ├── StatusBadge.tsx     (DRAFT/SUBMITTED/APPROVED...)
│   │   ├── ConfirmDialog.tsx
│   │   └── LoadingOverlay.tsx
│   └── hooks/
│       └── useToast.ts
│
├── services/
│   ├── apiClient.ts            (axios instance + interceptors)
│   └── authService.ts
│
└── types/                      ← TypeScript types mirror backend DTOs
    ├── syllabus.types.ts
    ├── matrix.types.ts
    └── approval.types.ts
```

---

### 3.2 API Layer — Axios + React Query

```typescript
// services/apiClient.ts — Cấu hình một lần, dùng toàn app
import axios from 'axios';

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL,   // /api/v1
  headers: { 'Content-Type': 'application/json' },
});

// Request interceptor: tự đính JWT vào mọi request
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// Response interceptor: auto refresh token khi 401
apiClient.interceptors.response.use(
  (response) => response.data,             // unwrap ApiResponse wrapper
  async (error) => {
    if (error.response?.status === 401) {
      await refreshTokenAndRetry(error.config);
    }
    return Promise.reject(parseApiError(error));  // normalize error
  }
);
```

```typescript
// features/syllabus/syllabusApi.ts
export const syllabusApi = {
  getDetail: (id: number) =>
    apiClient.get<SyllabusDetailDto>(`/syllabi/${id}`),

  update: (id: number, data: UpdateSyllabusRequest) =>
    apiClient.put<SyllabusDetailDto>(`/syllabi/${id}`, data),

  submit: (id: number) =>
    apiClient.post<SyllabusDto>(`/syllabi/${id}/submit`),

  clone: (id: number) =>
    apiClient.post<SyllabusDto>(`/syllabi/${id}/clone`),

  getDiff: (id: number) =>
    apiClient.get<DiffResponse>(`/syllabi/${id}/diff`),
};

// features/syllabus/hooks/useSyllabus.ts — React Query hooks
export function useSyllabusDetail(id: number) {
  return useQuery({
    queryKey: ['syllabus', id],
    queryFn: () => syllabusApi.getDetail(id),
    staleTime: 30_000,                     // cache 30 giây
  });
}

export function useSubmitSyllabus() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => syllabusApi.submit(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: ['syllabus', id] });
      queryClient.invalidateQueries({ queryKey: ['approvals'] });
      toast.success('Syllabus submitted successfully');
    },
    onError: (error: ApiError) => {
      toast.error(error.message);
    },
  });
}
```

---

### 3.3 Form Management — React Hook Form + Zod

```typescript
// Validation schema mirror backend validation
const cloSchema = z.object({
  code: z.string().regex(/^CLO\d+$/, 'Format: CLO1, CLO2...'),
  description: z.string().min(10).max(500),
  bloomLevel: z.enum(['KNOWLEDGE','COMPREHENSION','APPLICATION','ANALYSIS','SYNTHESIS','EVALUATION']),
  ploMappings: z.array(z.object({
    ploId: z.number(),
    level: z.enum(['I', 'D', 'A']),
  })).min(1, 'At least one PLO mapping required'),
});

// Component dùng RHF
function CloForm({ syllabusId, onSuccess }: Props) {
  const { register, handleSubmit, control, formState: { errors } } = useForm<CloInput>({
    resolver: zodResolver(cloSchema),
  });

  const { mutate, isPending } = useCreateClo(syllabusId);

  return (
    <form onSubmit={handleSubmit((data) => mutate(data, { onSuccess }))}>
      <input {...register('code')} />
      {errors.code && <ErrorMsg>{errors.code.message}</ErrorMsg>}
      {/* ... */}
      <Button type="submit" loading={isPending}>Save CLO</Button>
    </form>
  );
}
```

---

### 3.4 TypeScript — Type Safety nghiêm chỉnh

```typescript
// types/syllabus.types.ts — Mirror backend DTOs
export type SyllabusStatus =
  | 'DRAFT' | 'SUBMITTED' | 'UNDER_REVIEW'
  | 'REVISION_REQUESTED' | 'APPROVED' | 'REJECTED' | 'ARCHIVED';

export interface SyllabusDetailDto {
  id: number;
  courseCode: string;
  courseName: string;
  versionNumber: number;
  versionLabel: string;
  status: SyllabusStatus;
  isCurrent: boolean;
  semesterLabel: string;
  assignedInstructorName: string;
  clos: CloDto[];
  topics: TopicDto[];
  assessments: AssessmentComponentDto[];
  books: BookDto[];
  updatedAt: string;   // ISO 8601
}

// Type guard — kiểm tra status an toàn
export function isEditable(status: SyllabusStatus): boolean {
  return status === 'DRAFT' || status === 'REVISION_REQUESTED';
}

export function canSubmit(status: SyllabusStatus): boolean {
  return status === 'DRAFT';
}
```

---

### 3.5 State Management — Zustand cho Auth, React Query cho Server State

```typescript
// Không dùng Redux cho toàn bộ — overkill
// Chỉ dùng Zustand cho client state (auth, UI preferences)
// Dùng React Query cho server state (data từ API)

// store/authStore.ts
interface AuthState {
  user: UserPrincipal | null;
  accessToken: string | null;
  setAuth: (user: UserPrincipal, token: string) => void;
  logout: () => void;
  hasRole: (role: Role) => boolean;
}

export const useAuthStore = create<AuthState>((set, get) => ({
  user: null,
  accessToken: null,
  setAuth: (user, token) => set({ user, accessToken: token }),
  logout: () => {
    set({ user: null, accessToken: null });
    localStorage.removeItem('accessToken');
  },
  hasRole: (role) => get().user?.roles.includes(role) ?? false,
}));

// Dùng trong component:
function ApproveButton({ approvalId }: { approvalId: number }) {
  const hasRole = useAuthStore(s => s.hasRole);
  if (!hasRole('DEPT_HEAD') && !hasRole('DEAN')) return null;
  // ...
}
```

---

### 3.6 Các quy tắc bổ sung

| # | Quy tắc | Lý do |
|---|---|---|
| BP-R01 | **Không fetch trong component** — luôn dùng custom hook | Tách data logic khỏi UI, dễ test |
| BP-R02 | Dùng **React Query** cho mọi server state | Auto cache, refetch, loading/error state |
| BP-R03 | Dùng **React Hook Form** cho form | Uncontrolled inputs, performant, tích hợp Zod |
| BP-R04 | Dùng **Zod** validation schema | Type-safe, reusable, mirror backend rules |
| BP-R05 | **Lazy load** route-level components | `React.lazy()` + `Suspense` — giảm bundle size |
| BP-R06 | Không dùng `any` trong TypeScript | Bật `strict: true` trong tsconfig |
| BP-R07 | Đặt tên component theo **PascalCase**, hook theo **useXxx** | Convention nhất quán |
| BP-R08 | Dùng **Tailwind CSS** với class variants rõ ràng | Không viết inline style, không global CSS |
| BP-R09 | Dùng **Axios interceptor** để xử lý auth token | Không lặp lại logic trong mỗi API call |
| BP-R10 | Dùng **env variable** (`VITE_API_URL`) | Không hardcode URL — khác nhau giữa dev/prod |

---

## 4. AI-Assisted Development — Copilot & Claude

> Sinh viên sẽ sử dụng AI (GitHub Copilot hoặc Claude) trong quá trình phát triển. Phần này hướng dẫn cách sử dụng **hiệu quả và có trách nhiệm**.

---

### 4.1 Nguyên tắc chung — AI là Junior Developer, bạn là Tech Lead

```
AI tốt ở:                           AI kém ở:
✅ Sinh code boilerplate            ❌ Hiểu business logic phức tạp
✅ Viết unit test từ function có sẵn ❌ Đảm bảo security (phải review kỹ)
✅ Refactor code theo pattern        ❌ Thiết kế kiến trúc tổng thể
✅ Giải thích lỗi và suggest fix     ❌ Biết context đặc thù của project
✅ Viết Javadoc / JSDoc             ❌ Tự kiểm tra đúng sai business rule
```

**Quy tắc vàng:** Không commit code AI sinh ra mà không đọc hiểu và review.

---

### 4.2 GitHub Copilot — Best Practices

#### Kỹ thuật 1: Context Comment trước khi code

Copilot sinh code tốt hơn khi có context rõ ràng. Viết comment mô tả **trước** khi gõ code:

```java
// Service: Submit syllabus for approval
// Pre-conditions:
//   - Syllabus must be in DRAFT or REVISION_REQUESTED status
//   - Must have at least 1 CLO
//   - All CLOs must have PLO mappings
//   - Assessment weights must sum to 100%
// Post-conditions:
//   - status changes to SUBMITTED
//   - ApprovalRequest created for STEP1_DEPT_HEAD
//   - Email sent to responsible dept head
// Throws: ValidationException if pre-conditions not met
public SyllabusDto submitForApproval(Long syllabusId, UserPrincipal actor) {
    // Copilot sẽ sinh code phù hợp với comment trên
}
```

#### Kỹ thuật 2: Test-first với Copilot

Viết tên test method rõ ràng → Copilot sinh implementation:

```java
@Test
void submitSyllabus_whenCloHasNoPloMapping_shouldThrowValidationException() {
    // Copilot sẽ sinh arrange/act/assert phù hợp
}

@Test
void approveSyllabusStep1_whenUserIsNotDeptHead_shouldThrowAccessDeniedException() {
}

@Test
void cloneSyllabus_shouldDeepCopyAllCloAndTopics() {
}
```

#### Kỹ thuật 3: Copilot Chat cho Review

Dùng `Ctrl+I` (inline chat) hoặc Copilot Chat để:

```
Prompt hiệu quả:
"Review this service method for potential N+1 query issues"
"Does this code handle the case where syllabus is already APPROVED?"
"Suggest test cases I might have missed for this approval workflow"
"Refactor this to follow Spring Boot transaction best practices"
```

#### Kỹ thuật 4: Copilot cho TypeScript types

```typescript
// Chỉ cần viết interface name + gợi ý, Copilot sẽ sinh đủ fields
// dựa trên API response mẫu paste vào comment:

// API response example:
// { "id": 1, "status": "DRAFT", "versionNumber": 2, "clos": [...] }
interface SyllabusDetailDto {
  // Copilot tự điền từ comment trên
}
```

---

### 4.3 Claude — Best Practices với Project này

Claude mạnh hơn Copilot ở reasoning và kiến trúc. Dưới đây là các prompt pattern hiệu quả.

#### Pattern 1: Cung cấp Schema làm Context

Luôn paste schema liên quan vào đầu prompt:

```
[Schema context]
Table syllabus: id, course_id, version_number, status ENUM(...), is_current BOOLEAN
Table approval_request: syllabus_id, step ENUM('STEP1_DEPT_HEAD','STEP2_PROG_COORDINATOR','STEP3_DEAN'), status ENUM('PENDING','APPROVED','REJECTED')

[Request]
Viết ApprovalWorkflowService.approveStep() đảm bảo:
1. Validate đúng reviewer có quyền approve step đó
2. Khi STEP3 approve: flip is_current đúng cách trong transaction
3. Tạo ApprovalRequest cho step tiếp theo
4. Không tạo bước tiếp nếu đã là STEP3
```

#### Pattern 2: Yêu cầu Review Code với tiêu chí cụ thể

```
Review đoạn code này theo các tiêu chí:
1. Spring Security — có lỗ hổng RBAC nào không?
2. Transaction — có vấn đề gì nếu email gửi fail sau khi DB commit?
3. N+1 query — có lazy loading problem không?
4. Error handling — có case nào chưa được xử lý?

[paste code]
```

#### Pattern 3: Generate Test Cases từ Business Rules

```
Dựa trên business rules sau của Approval Workflow:
- DEPT_HEAD chỉ approve STEP1 của syllabus thuộc department mình
- Sau STEP3 approve, is_current của version cũ phải = FALSE
- Nếu REJECT, syllabus.status = REVISION_REQUESTED (không phải REJECTED)
- Reviewer không thể approve chính syllabus của mình (nếu là instructor)

Sinh ra danh sách test cases đầy đủ (happy path + edge cases + security cases)
dưới dạng JUnit 5 method names với mô tả ngắn.
```

#### Pattern 4: Ask for Trade-offs, không chỉ "best solution"

```
Tôi đang thiết kế CLO-PLO matrix query cho heatmap.
Có 2 cách:
A) Query từ MySQL view v_clo_plo_matrix
B) Tính toán trong Java từ raw tables

Phân tích trade-offs của 2 cách với context:
- ~100 courses, 6 PLOs, ~500 CLO-PLO mappings
- Query phục vụ heatmap dashboard của Dean (không cần realtime)
- Có thể cache
Khuyến nghị cách nào và tại sao?
```

---

### 4.4 CLAUDE.md — Project Context File

Tạo file `CLAUDE.md` ở root project để Claude (và Copilot) tự động nhận context mỗi khi làm việc với project. Đây là **thực hành chuẩn** khi dùng AI trên codebase lớn.

```markdown
# CLAUDE.md — Project Context for AI Assistants

## Project Overview
Curriculum Management & Compliance Dashboard for SCSE, IU VNU-HCM.
Manages syllabi and program outcomes for ASIIN accreditation.
4 programs: CS / IT / DS / Network Engineering

## Tech Stack
- Backend: Spring Boot 3.2, Java 17, Spring Security (JWT), JPA/Hibernate
- Database: MySQL 8, 28 tables, utf8mb4
- Frontend: React 18, TypeScript, Vite, Tailwind CSS, React Query, React Hook Form
- Testing: JUnit 5, Mockito, Testcontainers (for integration tests)

## Key Domain Concepts
- **Syllabus**: Versioned. Status: DRAFT→SUBMITTED→UNDER_REVIEW→APPROVED/REJECTED
- **CLO**: Belongs to Syllabus (not Course). Has Bloom taxonomy level.
- **CLO-PLO Mapping**: Level I (Introduce) / D (Develop) / A (Apply) per ASIIN standard
- **Approval Workflow**: 3 steps — STEP1: Dept Head → STEP2: Prog Coordinator → STEP3: Dean
- **is_current**: Only ONE syllabus per course can have is_current = TRUE

## RBAC Roles
ADMIN > DEAN > DEPT_HEAD / PROGRAM_COORDINATOR > INSTRUCTOR
Data-level: Instructor → own syllabi only; Dept Head → own department only

## Package Convention
Package by feature: `edu.iu.scse.curriculum.{feature}.{layer}`
Features: auth, user, organization, program, plo, course, syllabus, approval, compliance, export

## API Convention
Base: /api/v1/{resource}
Response wrapper: ApiResponse<T> { success, message, data, errorCode, timestamp }
Pagination: Pageable param, return Page<T>

## Database Conventions
- snake_case column names
- ENUM stored as STRING (not ORDINAL)
- ALL tables have created_at, updated_at
- DO NOT use ddl-auto: create or update — schema managed manually

## What NOT to do
- Do NOT return JPA Entity directly from Controller
- Do NOT use @Transactional on Controller
- Do NOT use FetchType.EAGER
- Do NOT use Lombok @Data on Entity classes
- Do NOT hardcode any URL or credential

## Testing Conventions
- Unit tests: Service layer with Mockito mocks
- Integration tests: Use @SpringBootTest + Testcontainers MySQL
- Test method naming: methodName_whenCondition_shouldOutcome
```

---

### 4.5 Copilot Instructions File (`.github/copilot-instructions.md`)

GitHub Copilot hỗ trợ custom instructions file — tương tự CLAUDE.md:

```markdown
# Copilot Instructions

## Code Style
- Java 17: Use records for DTOs, text blocks for SQL, switch expressions
- Always use @RequiredArgsConstructor (Lombok) instead of field injection
- Use SLF4J @Slf4j for logging

## Patterns to Follow
- Service methods: validate → business logic → save → notify
- All list endpoints must accept Pageable parameter
- All mutations must be @Transactional at Service layer

## Patterns to AVOID
- Never use field injection @Autowired — use constructor injection
- Never return Entity from Controller
- Never catch generic Exception — use specific exception types
- Never use System.out.println — use log.info/warn/error

## Testing
- Test class naming: {ClassName}Test
- Use @ExtendWith(MockitoExtension.class) for unit tests
- Mock all dependencies, test only the class under test

## Database
- Schema is fixed — never modify ddl-auto
- Always use JPQL or native query with named parameters
- Avoid N+1: use JOIN FETCH or @EntityGraph
```

---

### 4.6 Workflow đề xuất — Kết hợp AI trong development cycle

```
1. DESIGN (Claude)
   Thảo luận kiến trúc → Claude đề xuất trade-offs → bạn quyết định

2. SCAFFOLD (Copilot)
   Tạo class/interface skeleton → Copilot điền boilerplate

3. IMPLEMENT LOGIC (Developer + Copilot)
   Developer viết business logic → Copilot hoàn chỉnh boilerplate
   Developer đọc và verify mọi dòng Copilot sinh ra

4. REVIEW (Claude)
   Paste code → Claude review security, transaction, N+1, edge cases

5. TEST (Developer + Copilot)
   Developer viết test method names → Copilot sinh arrange/act/assert
   Developer verify test assertions đúng business rules

6. DEBUG (Claude)
   Paste error + stack trace + code → Claude phân tích root cause

7. DOCUMENT (Copilot)
   Comment trên method → Copilot sinh Javadoc/JSDoc
```

---

### 4.7 Checklist trước khi commit code AI-generated

```
□ Đã đọc hiểu toàn bộ code (không commit blindly)
□ Không có hardcoded credential, URL, hoặc magic string
□ Exception handling đầy đủ (không catch rồi bỏ qua)
□ Transaction boundary đúng (Service, không phải Controller)
□ Không có FetchType.EAGER hoặc N+1 tiềm ẩn
□ Security: đã check cả role lẫn data ownership
□ Đã có ít nhất 1 unit test cho happy path
□ Swagger annotation đầy đủ nếu là endpoint mới
□ Không có System.out.println — đã dùng log
□ DTO được dùng (không return Entity)
```

---

*Tài liệu này là phần của luận văn: Curriculum Management & Compliance Dashboard — SCSE IU VNU-HCM*
*Cập nhật theo tiến độ dự án. Version 1.0 — 2025*
