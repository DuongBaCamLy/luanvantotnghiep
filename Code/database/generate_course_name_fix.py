import re
from pathlib import Path

source = Path("seed_data.sql")
output = Path("fix_course_name_vn.sql")

text = source.read_text(encoding="utf-8")

# Chỉ lấy phần INSERT INTO course(...)
start = text.find("INSERT INTO course (")
if start == -1:
    raise RuntimeError("Không tìm thấy INSERT INTO course trong seed_data.sql")

end = text.find(";", start)
course_block = text[start:end]

# Bắt:
# (id, 'COURSE_CODE', 'English name', 'Vietnamese name', department_id, ...)
pattern = re.compile(
    r"\(\s*\d+\s*,\s*'((?:''|[^'])*)'\s*,\s*'((?:''|[^'])*)'\s*,\s*'((?:''|[^'])*)'\s*,",
    re.MULTILINE
)

rows = pattern.findall(course_block)

if not rows:
    raise RuntimeError("Không đọc được course nào từ seed_data.sql")

sql = [
    "SET NAMES utf8mb4;",
    "USE curriculum_iu;",
    "",
    "START TRANSACTION;",
    ""
]

for course_code, name, name_vn in rows:
    course_code = course_code.replace("''", "'")
    name_vn = name_vn.replace("''", "'")

    # escape lại cho SQL
    course_code_sql = course_code.replace("'", "''")
    name_vn_sql = name_vn.replace("'", "''")

    sql.append(
        f"UPDATE course "
        f"SET name_vn = '{name_vn_sql}' "
        f"WHERE course_code = '{course_code_sql}';"
    )

sql.extend([
    "",
    "COMMIT;",
    ""
])

output.write_text("\n".join(sql), encoding="utf-8")

print(f"Đã đọc {len(rows)} course.")
print(f"Đã tạo: {output.resolve()}")