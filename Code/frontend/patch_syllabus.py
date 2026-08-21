import re

with open('E:/Luận Văn Tốt Nghiệp/Code/frontend/src/components/syllabus/SyllabusForm.tsx', 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Add cn import
if 'import { cn }' not in content:
    content = content.replace('import { MAJORS }', 'import { cn } from "@/lib/utils"\nimport { MAJORS }')

# 2. Change validationErrors type
content = content.replace('const [validationErrors, setValidationErrors] = useState<string[]>([])', 'const [validationErrors, setValidationErrors] = useState<{id: string, name: string}[]>([])')

# 3. Update the handleSubmit error pushes
replacements = [
    ('errors.push("Khóa (Academic Year)");', 'errors.push({ id: "academicYear", name: "Khóa (Academic Year)" });'),
    ('errors.push("Chuyên ngành (Major)");', 'errors.push({ id: "major", name: "Chuyên ngành (Major)" });'),
    ('errors.push("Course Name (English)");', 'errors.push({ id: "courseNameEn", name: "Course Name (English)" });'),
    ('errors.push("Course Name (Vietnamese)");', 'errors.push({ id: "courseNameVn", name: "Course Name (Vietnamese)" });'),
    ('errors.push("Course Code");', 'errors.push({ id: "courseCode", name: "Course Code" });'),
    ('errors.push("Instructor");', 'errors.push({ id: "instructor", name: "Instructor" });'),
    ('errors.push("Credits (Theory)");', 'errors.push({ id: "creditsTheory", name: "Credits (Theory)" });'),
    ('errors.push("Credits (Practice)");', 'errors.push({ id: "creditsPractice", name: "Credits (Practice)" });'),
    ('errors.push("ECTS");', 'errors.push({ id: "ects", name: "ECTS" });'),
    ('errors.push("Periods (Theory)");', 'errors.push({ id: "periodsTheory", name: "Periods (Theory)" });'),
    ('errors.push("Periods (Practice)");', 'errors.push({ id: "periodsPractice", name: "Periods (Practice)" });'),
    ('errors.push("Course Designation");', 'errors.push({ id: "courseDesignation", name: "Course Designation" });'),
    ('errors.push("Semester");', 'errors.push({ id: "semester", name: "Semester" });'),
    ('errors.push("Language");', 'errors.push({ id: "language", name: "Language" });'),
    ('errors.push("Relation (Previous courses)");', 'errors.push({ id: "relation", name: "Relation (Previous courses)" });'),
    ('errors.push("Teaching methods");', 'errors.push({ id: "teachingMethods", name: "Teaching methods" });'),
    ('errors.push("Total Workload");', 'errors.push({ id: "workloadTotal", name: "Total Workload" });'),
    ('errors.push("Contact hours");', 'errors.push({ id: "workloadContact", name: "Contact hours" });'),
    ('errors.push("Private study");', 'errors.push({ id: "workloadPrivate", name: "Private study" });'),
    ('errors.push("Prerequisites");', 'errors.push({ id: "prerequisites", name: "Prerequisites" });'),
    ('errors.push("Course Objectives");', 'errors.push({ id: "objectives", name: "Course Objectives" });'),
    ('errors.push("Examination forms");', 'errors.push({ id: "examForms", name: "Examination forms" });'),
    ('errors.push("Examination requirements");', 'errors.push({ id: "examReqs", name: "Examination requirements" });'),
    ('errors.push("Rubrics");', 'errors.push({ id: "rubrics", name: "Rubrics" });'),
    ('errors.push("Course Type (Cơ sở, Chuyên ngành...)");', 'errors.push({ id: "courseTypes", name: "Course Type (Cơ sở, Chuyên ngành...)" });'),
    ('errors.push("Course Learning Outcomes (CLOs) - Nội dung không được để trống");', 'errors.push({ id: "clos", name: "Course Learning Outcomes (CLOs) - Nội dung không được để trống" });'),
    ('errors.push("Content (Topics) - Phải có ít nhất 1 chủ đề, điền đủ Tên, Số giờ, Trình độ");', 'errors.push({ id: "topics", name: "Content (Topics) - Phải có ít nhất 1 chủ đề, điền đủ Tên, Số giờ, Trình độ" });'),
    ('errors.push("Assessment Plan - Phải có ít nhất 1 đánh giá, điền đủ Hình thức và Trọng số > 0");', 'errors.push({ id: "assessments", name: "Assessment Plan - Phải có ít nhất 1 đánh giá, điền đủ Hình thức và Trọng số > 0" });')
]

content = content.replace('const errors: string[] = [];', 'const errors: {id: string, name: string}[] = [];')

for old, new_s in replacements:
    content = content.replace(old, new_s)

# Update scroll logic
old_scroll = """    if (errors.length > 0) {
      setValidationErrors(errors);
      alert("Vui lòng điền đầy đủ thông tin! Kéo lên đầu trang để xem danh sách các trường còn thiếu.");
      window.scrollTo({ top: 0, behavior: 'smooth' });
      return;
    }"""

new_scroll = """    if (errors.length > 0) {
      setValidationErrors(errors);
      
      setTimeout(() => {
        const firstErrorEl = document.getElementById(errors[0].id);
        if (firstErrorEl) {
          firstErrorEl.scrollIntoView({ behavior: 'smooth', block: 'center' });
          firstErrorEl.focus();
        }
      }, 100);
      return;
    }"""

content = content.replace(old_scroll, new_scroll)

# Render validation alert
old_render_alert = """          <ul className="list-disc pl-5 space-y-1 text-sm text-red-600">
            {validationErrors.map((err, idx) => (
              <li key={idx}>{err}</li>
            ))}
          </ul>"""

new_render_alert = """          <ul className="list-disc pl-5 space-y-1 text-sm text-red-600">
            {validationErrors.map((err, idx) => (
              <li key={idx} className="cursor-pointer hover:underline" onClick={() => {
                const el = document.getElementById(err.id);
                if (el) {
                  el.scrollIntoView({ behavior: 'smooth', block: 'center' });
                  el.focus();
                }
              }}>{err.name}</li>
            ))}
          </ul>"""

content = content.replace(old_render_alert, new_render_alert)

# Now, add IDs and error classes to the inputs!
field_mapping = {
    'value={courseNameEn}': 'id="courseNameEn"',
    'value={courseNameVn}': 'id="courseNameVn"',
    'value={courseCode}': 'id="courseCode"',
    'value={instructor}': 'id="instructor"',
    'value={creditsTheory}': 'id="creditsTheory"',
    'value={creditsPractice}': 'id="creditsPractice"',
    'value={ects}': 'id="ects"',
    'value={periodsTheory}': 'id="periodsTheory"',
    'value={periodsPractice}': 'id="periodsPractice"',
    'value={courseDesignation}': 'id="courseDesignation"',
    'value={semester}': 'id="semester"',
    'value={language}': 'id="language"',
    'value={relation}': 'id="relation"',
    'value={teachingMethods}': 'id="teachingMethods"',
    'value={workloadTotal}': 'id="workloadTotal"',
    'value={workloadContact}': 'id="workloadContact"',
    'value={workloadPrivate}': 'id="workloadPrivate"',
    'value={prerequisites}': 'id="prerequisites"',
    'value={objectives}': 'id="objectives"',
    'value={examForms}': 'id="examForms"',
    'value={examReqs}': 'id="examReqs"',
    'value={rubrics}': 'id="rubrics"',
}

# Add helper function for dynamic class
helper_func = """  const getErrorClass = (id: string) => {
    return validationErrors.some(e => e.id === id) ? "border-red-500 ring-red-500 bg-red-50/50" : "";
  }
  
  return ("""

content = content.replace('  return (', helper_func)

for search_str, id_str in field_mapping.items():
    lines = content.split('\n')
    for i in range(len(lines)):
        if search_str in lines[i] and 'className={inputClasses}' in lines[i]:
            field_id = id_str.split('"')[1]
            lines[i] = lines[i].replace('className={inputClasses}', f'id="{field_id}" className={{cn(inputClasses, getErrorClass("{field_id}"))}}')
    content = '\n'.join(lines)

# For Textareas (inputClasses -> textareaClasses if there's any, else skip)
# For Selects: major, academicYear
content = content.replace(
    '<SelectTrigger className={inputClasses}>\n                    <SelectValue placeholder="Chọn khóa" />',
    '<SelectTrigger id="academicYear" className={cn(inputClasses, getErrorClass("academicYear"))}>\n                    <SelectValue placeholder="Chọn khóa" />'
)
content = content.replace(
    '<SelectTrigger className={inputClasses}>\n                    <SelectValue placeholder="Chọn chuyên ngành" />',
    '<SelectTrigger id="major" className={cn(inputClasses, getErrorClass("major"))}>\n                    <SelectValue placeholder="Chọn chuyên ngành" />'
)

# For CourseTypes (Checkbox group)
content = content.replace(
    '<div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">',
    '<div id="courseTypes" className={cn("grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3 p-2 rounded-lg transition-all", getErrorClass("courseTypes"))}>'
)

# For CLOs table
content = content.replace(
    '<table className="w-full text-sm text-left border-collapse">',
    '<table id="clos" className={cn("w-full text-sm text-left border-collapse transition-all", getErrorClass("clos") && "ring-2 ring-red-500")}>'
)

# For Topics table
content = content.replace(
    '<table className="w-full text-sm text-left">',
    '<table id="topics" className={cn("w-full text-sm text-left transition-all", getErrorClass("topics") && "ring-2 ring-red-500")}>'
)

# For Assessments table
content = content.replace(
    '<table className="w-full text-sm text-center border-collapse">',
    '<table id="assessments" className={cn("w-full text-sm text-center border-collapse transition-all", getErrorClass("assessments") && "ring-2 ring-red-500")}>'
)


with open('E:/Luận Văn Tốt Nghiệp/Code/frontend/src/components/syllabus/SyllabusForm.tsx', 'w', encoding='utf-8') as f:
    f.write(content)

print("Done")
