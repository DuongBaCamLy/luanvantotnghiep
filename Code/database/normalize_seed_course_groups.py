from pathlib import Path
import re

path = Path("seed_data.sql")

text = path.read_text(encoding="utf-8")

# ============================================================
# 1. Replace course_type seed with exactly 3 SRS groups
# ============================================================

old_block_pattern = re.compile(
    r"INSERT INTO course_type \(id, code, name, name_vn\) VALUES\s*"
    r".*?;",
    re.DOTALL,
)

new_block = """INSERT INTO course_type (id, code, name, name_vn) VALUES
  (1, 'GENERAL',     'General',     'Giáo dục đại cương'),
  (4, 'ELECTIVE',    'Elective',    'Môn tự chọn'),
  (7, 'COMPULSORY',  'Compulsory',  'Môn bắt buộc');"""

text, count = old_block_pattern.subn(
    new_block,
    text,
    count=1,
)

if count != 1:
    raise RuntimeError(
        "Không tìm thấy đúng 1 block INSERT INTO course_type."
    )

# ============================================================
# 2. Normalize course_program.course_type_id
#
# Old:
# 1 GENERAL      -> 1
# 2 FOUNDATION   -> 7
# 3 CORE         -> 7
# 4 ELECTIVE     -> 4
# 5 THESIS       -> 7
# 6 INTERNSHIP   -> 7
# ============================================================

start_marker = "INSERT INTO course_program"
start = text.find(start_marker)

if start == -1:
    raise RuntimeError(
        "Không tìm thấy INSERT INTO course_program."
    )

end = text.find(";", start)

if end == -1:
    raise RuntimeError(
        "Không tìm thấy dấu ; kết thúc course_program."
    )

block = text[start:end + 1]

mapping = {
    1: 1,
    2: 7,
    3: 7,
    4: 4,
    5: 7,
    6: 7,
}

tuple_pattern = re.compile(
    r"\(\s*"
    r"(\d+)\s*,\s*"      # id
    r"(\d+)\s*,\s*"      # course_id
    r"(\d+)\s*,\s*"      # program_id
    r"(NULL|\d+)\s*,\s*" # cohort_id
    r"(\d+)\s*,"         # course_type_id
)

def replace_tuple(match):
    row_id = match.group(1)
    course_id = match.group(2)
    program_id = match.group(3)
    cohort_id = match.group(4)
    old_type = int(match.group(5))

    if old_type not in mapping:
        raise RuntimeError(
            f"CourseProgram {row_id} có course_type_id "
            f"không mong đợi: {old_type}"
        )

    new_type = mapping[old_type]

    return (
        f"({row_id}, {course_id}, {program_id}, "
        f"{cohort_id}, {new_type},"
    )

new_course_program_block, changed = tuple_pattern.subn(
    replace_tuple,
    block,
)

if changed == 0:
    raise RuntimeError(
        "Không sửa được course_program nào."
    )

text = (
    text[:start]
    + new_course_program_block
    + text[end + 1:]
)

path.write_text(
    text,
    encoding="utf-8",
)

print("SUCCESS")
print(f"Normalized {changed} course_program rows.")
print("Course groups:")
print("  1 = GENERAL")
print("  4 = ELECTIVE")
print("  7 = COMPULSORY")