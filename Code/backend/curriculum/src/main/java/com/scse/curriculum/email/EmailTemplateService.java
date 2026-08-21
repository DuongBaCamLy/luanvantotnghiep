package com.scse.curriculum.email;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

@Service
public class EmailTemplateService {

    public String renderHtml(
            WorkflowEmailMessage email,
            String recipientName,
            String absoluteActionUrl) {

        String[] palette = palette(email.tone());
        String commentBlock = isBlank(email.comment())
                ? ""
                : """
                  <tr><td style="padding:0 32px 24px">
                    <div style="border-left:4px solid {{ACCENT}};background:#f8fafc;border-radius:8px;padding:16px 18px">
                      <div style="font-size:12px;font-weight:700;letter-spacing:.04em;text-transform:uppercase;color:#64748b;margin-bottom:7px">Nhận xét</div>
                      <div style="font-size:14px;line-height:1.65;color:#334155;white-space:pre-wrap">{{COMMENT}}</div>
                    </div>
                  </td></tr>
                  """
                        .replace("{{ACCENT}}", palette[0])
                        .replace("{{COMMENT}}", esc(email.comment()));

        String actorRow = isBlank(email.actorName())
                ? ""
                : metadataRow("Người xử lý", email.actorName());

        String actionBlock = isBlank(absoluteActionUrl)
                ? ""
                : """
                  <tr><td style="padding:0 32px 30px;text-align:center">
                    <a href="{{ACTION_URL}}" style="display:inline-block;background:{{ACCENT}};color:#ffffff;text-decoration:none;font-size:14px;font-weight:700;padding:13px 22px;border-radius:9px;box-shadow:0 6px 18px {{SHADOW}}">{{ACTION_LABEL}}</a>
                  </td></tr>
                  """
                        .replace("{{ACTION_URL}}", escAttr(absoluteActionUrl))
                        .replace("{{ACCENT}}", palette[0])
                        .replace("{{SHADOW}}", palette[2])
                        .replace("{{ACTION_LABEL}}", esc(defaultText(email.actionLabel(), "Xem chi tiết")));

        Map<String, String> values = new LinkedHashMap<>();
        values.put("{{PREHEADER}}", esc(email.message()));
        values.put("{{RECIPIENT}}", esc(defaultText(recipientName, "Thầy/Cô")));
        values.put("{{HEADING}}", esc(email.heading()));
        values.put("{{STATUS}}", esc(email.statusLabel()));
        values.put("{{MESSAGE}}", esc(email.message()));
        values.put("{{COURSE_CODE}}", esc(defaultText(email.courseCode(), "-")));
        values.put("{{COURSE_NAME}}", esc(defaultText(email.courseName(), "-")));
        values.put("{{VERSION}}", esc(defaultText(email.versionLabel(), "-")));
        values.put("{{ACADEMIC_YEAR}}", esc(defaultText(email.academicYear(), "-")));
        values.put("{{SEMESTER}}", esc(defaultText(email.semester(), "-")));
        values.put("{{ACTOR_ROW}}", actorRow);
        values.put("{{COMMENT_BLOCK}}", commentBlock);
        values.put("{{ACTION_BLOCK}}", actionBlock);
        values.put("{{ACCENT}}", palette[0]);
        values.put("{{ACCENT_BG}}", palette[1]);

        String template = """
                <!doctype html>
                <html lang="vi">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width,initial-scale=1">
                  <title>{{HEADING}}</title>
                </head>
                <body style="margin:0;padding:0;background:#eef2f7;font-family:Inter,Segoe UI,Arial,sans-serif;color:#0f172a">
                  <div style="display:none;max-height:0;overflow:hidden;opacity:0">{{PREHEADER}}</div>
                  <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="background:#eef2f7;padding:28px 12px">
                    <tr><td align="center">
                      <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="max-width:640px;background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 12px 34px rgba(15,23,42,.10)">
                        <tr><td style="background:linear-gradient(135deg,#0f2747,#173f70);padding:24px 32px">
                          <table role="presentation" width="100%"><tr>
                            <td>
                              <div style="color:#93c5fd;font-size:12px;font-weight:700;letter-spacing:.12em;text-transform:uppercase">SCSE · IU</div>
                              <div style="color:#ffffff;font-size:20px;font-weight:750;margin-top:5px">Curriculum Management System</div>
                            </td>
                            <td align="right"><div style="width:42px;height:42px;border-radius:12px;background:rgba(255,255,255,.12);line-height:42px;text-align:center;color:#fff;font-weight:800">CS</div></td>
                          </tr></table>
                        </td></tr>
                        <tr><td style="padding:30px 32px 14px">
                          <div style="font-size:14px;color:#64748b;margin-bottom:10px">Kính gửi <strong style="color:#334155">{{RECIPIENT}}</strong>,</div>
                          <h1 style="margin:0;font-size:24px;line-height:1.3;color:#0f172a">{{HEADING}}</h1>
                          <div style="margin-top:14px;display:inline-block;padding:7px 12px;border-radius:999px;background:{{ACCENT_BG}};color:{{ACCENT}};font-size:12px;font-weight:800">{{STATUS}}</div>
                          <p style="margin:18px 0 0;font-size:15px;line-height:1.7;color:#475569">{{MESSAGE}}</p>
                        </td></tr>
                        <tr><td style="padding:10px 32px 24px">
                          <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="border:1px solid #e2e8f0;border-radius:12px;overflow:hidden">
                            <tr><td colspan="2" style="background:#f8fafc;padding:12px 16px;font-size:12px;font-weight:800;letter-spacing:.05em;text-transform:uppercase;color:#64748b">Thông tin đề cương</td></tr>
                            {{COURSE_CODE_ROW}}
                            {{COURSE_NAME_ROW}}
                            {{VERSION_ROW}}
                            {{YEAR_ROW}}
                            {{SEMESTER_ROW}}
                            {{ACTOR_ROW}}
                          </table>
                        </td></tr>
                        {{COMMENT_BLOCK}}
                        {{ACTION_BLOCK}}
                        <tr><td style="border-top:1px solid #e2e8f0;background:#f8fafc;padding:18px 32px;color:#64748b;font-size:12px;line-height:1.6">
                          Đây là email tự động từ hệ thống quản lý chương trình đào tạo. Vui lòng không trả lời trực tiếp email này.
                        </td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """;

        values.put("{{COURSE_CODE_ROW}}", metadataRow("Mã môn", email.courseCode()));
        values.put("{{COURSE_NAME_ROW}}", metadataRow("Tên môn", email.courseName()));
        values.put("{{VERSION_ROW}}", metadataRow("Phiên bản", email.versionLabel()));
        values.put("{{YEAR_ROW}}", metadataRow("Năm học", email.academicYear()));
        values.put("{{SEMESTER_ROW}}", metadataRow("Học kỳ", email.semester()));

        for (Map.Entry<String, String> entry : values.entrySet()) {
            template = template.replace(entry.getKey(), entry.getValue());
        }
        return template;
    }

    public String renderText(
            WorkflowEmailMessage email,
            String recipientName,
            String absoluteActionUrl) {

        StringBuilder text = new StringBuilder();
        text.append("Kính gửi ")
                .append(defaultText(recipientName, "Thầy/Cô"))
                .append(",\n\n")
                .append(email.heading()).append("\n")
                .append("Trạng thái: ").append(email.statusLabel()).append("\n\n")
                .append(email.message()).append("\n\n")
                .append("Mã môn: ").append(defaultText(email.courseCode(), "-")).append("\n")
                .append("Tên môn: ").append(defaultText(email.courseName(), "-")).append("\n")
                .append("Phiên bản: ").append(defaultText(email.versionLabel(), "-")).append("\n")
                .append("Năm học: ").append(defaultText(email.academicYear(), "-")).append("\n")
                .append("Học kỳ: ").append(defaultText(email.semester(), "-")).append("\n");

        if (!isBlank(email.actorName())) {
            text.append("Người xử lý: ").append(email.actorName()).append("\n");
        }
        if (!isBlank(email.comment())) {
            text.append("\nNhận xét:\n").append(email.comment()).append("\n");
        }
        if (!isBlank(absoluteActionUrl)) {
            text.append("\n")
                    .append(defaultText(email.actionLabel(), "Xem chi tiết"))
                    .append(": ")
                    .append(absoluteActionUrl)
                    .append("\n");
        }
        text.append("\n--\nSCSE Curriculum Management System");
        return text.toString();
    }

    public String renderDeadlineHtml(
            DeadlineEmailMessage email,
            String recipientName,
            String absoluteActionUrl) {

        String[] palette = palette(email.tone());
        StringBuilder courseRows = new StringBuilder();
        if (email.missingCourses() != null) {
            for (DeadlineEmailMessage.MissingCourseLine course : email.missingCourses()) {
                courseRows.append("""
                        <tr>
                          <td style="padding:10px 14px;border-top:1px solid #e2e8f0;color:#0f172a;font-size:13px;font-weight:750">{{CODE}}</td>
                          <td style="padding:10px 14px;border-top:1px solid #e2e8f0;color:#475569;font-size:13px">{{NAME}}</td>
                        </tr>
                        """
                        .replace("{{CODE}}", esc(course.courseCode()))
                        .replace("{{NAME}}", esc(course.courseName())));
            }
        }

        String actionBlock = isBlank(absoluteActionUrl)
                ? ""
                : """
                  <tr><td style="padding:0 32px 30px;text-align:center">
                    <a href="{{ACTION_URL}}" style="display:inline-block;background:{{ACCENT}};color:#ffffff;text-decoration:none;font-size:14px;font-weight:700;padding:13px 24px;border-radius:10px">{{ACTION_LABEL}}</a>
                  </td></tr>
                  """
                        .replace("{{ACTION_URL}}", escAttr(absoluteActionUrl))
                        .replace("{{ACCENT}}", palette[0])
                        .replace("{{ACTION_LABEL}}", esc(defaultText(
                                email.actionLabel(),
                                "Mở danh sách đề cương")));

        String template = """
                <!doctype html>
                <html lang="vi">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width,initial-scale=1">
                  <title>{{HEADING}}</title>
                </head>
                <body style="margin:0;padding:0;background:#eef2f7;font-family:Inter,Segoe UI,Arial,sans-serif;color:#0f172a">
                  <div style="display:none;max-height:0;overflow:hidden;opacity:0">{{PREHEADER}}</div>
                  <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="background:#eef2f7;padding:28px 12px">
                    <tr><td align="center">
                      <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="max-width:640px;background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 12px 34px rgba(15,23,42,.10)">
                        <tr><td style="background:linear-gradient(135deg,#0f2747,#173f70);padding:24px 32px">
                          <div style="color:#93c5fd;font-size:12px;font-weight:700;letter-spacing:.12em;text-transform:uppercase">SCSE · IU</div>
                          <div style="color:#ffffff;font-size:20px;font-weight:750;margin-top:5px">Curriculum Management System</div>
                        </td></tr>
                        <tr><td style="padding:30px 32px 14px">
                          <div style="font-size:14px;color:#64748b;margin-bottom:10px">Kính gửi <strong style="color:#334155">{{RECIPIENT}}</strong>,</div>
                          <h1 style="margin:0;font-size:24px;line-height:1.3;color:#0f172a">{{HEADING}}</h1>
                          <div style="margin-top:14px;display:inline-block;padding:7px 12px;border-radius:999px;background:{{ACCENT_BG}};color:{{ACCENT}};font-size:12px;font-weight:800">{{TIMING}}</div>
                          <p style="margin:18px 0 0;font-size:15px;line-height:1.7;color:#475569">{{MESSAGE}}</p>
                        </td></tr>
                        <tr><td style="padding:10px 32px 18px">
                          <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="border:1px solid #e2e8f0;border-radius:12px;overflow:hidden">
                            <tr><td style="width:34%;padding:10px 16px;background:#f8fafc;color:#64748b;font-size:13px">Năm học</td><td style="padding:10px 16px;background:#f8fafc;color:#0f172a;font-size:13px;font-weight:700">{{ACADEMIC_YEAR}}</td></tr>
                            <tr><td style="width:34%;padding:10px 16px;border-top:1px solid #e2e8f0;color:#64748b;font-size:13px">Học kỳ</td><td style="padding:10px 16px;border-top:1px solid #e2e8f0;color:#0f172a;font-size:13px;font-weight:700">{{SEMESTER}}</td></tr>
                            <tr><td style="width:34%;padding:10px 16px;border-top:1px solid #e2e8f0;color:#64748b;font-size:13px">Hạn nộp</td><td style="padding:10px 16px;border-top:1px solid #e2e8f0;color:#0f172a;font-size:13px;font-weight:700">{{DEADLINE}}</td></tr>
                          </table>
                        </td></tr>
                        <tr><td style="padding:0 32px 24px">
                          <div style="font-size:12px;font-weight:800;letter-spacing:.05em;text-transform:uppercase;color:#64748b;margin-bottom:8px">Các môn chưa nộp</div>
                          <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="border:1px solid #e2e8f0;border-radius:12px;overflow:hidden">
                            <tr><th align="left" style="padding:10px 14px;background:#f8fafc;color:#64748b;font-size:12px">Mã môn</th><th align="left" style="padding:10px 14px;background:#f8fafc;color:#64748b;font-size:12px">Tên môn</th></tr>
                            {{COURSE_ROWS}}
                          </table>
                        </td></tr>
                        {{ACTION_BLOCK}}
                        <tr><td style="border-top:1px solid #e2e8f0;background:#f8fafc;padding:18px 32px;color:#64748b;font-size:12px;line-height:1.6">
                          Đây là email tự động nhắc deadline từ hệ thống quản lý chương trình đào tạo. Vui lòng không trả lời trực tiếp email này.
                        </td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """;

        return template
                .replace("{{PREHEADER}}", esc(email.message()))
                .replace("{{RECIPIENT}}", esc(defaultText(recipientName, "Thầy/Cô")))
                .replace("{{HEADING}}", esc(email.heading()))
                .replace("{{ACCENT}}", palette[0])
                .replace("{{ACCENT_BG}}", palette[1])
                .replace("{{TIMING}}", esc(deadlineTiming(email.daysRemaining())))
                .replace("{{MESSAGE}}", esc(email.message()))
                .replace("{{ACADEMIC_YEAR}}", esc(defaultText(email.academicYear(), "-")))
                .replace("{{SEMESTER}}", esc(defaultText(email.semester(), "-")))
                .replace("{{DEADLINE}}", esc(defaultText(email.deadlineText(), "-")))
                .replace("{{COURSE_ROWS}}", courseRows.toString())
                .replace("{{ACTION_BLOCK}}", actionBlock);
    }

    public String renderDeadlineText(
            DeadlineEmailMessage email,
            String recipientName,
            String absoluteActionUrl) {

        StringBuilder text = new StringBuilder();
        text.append("Kính gửi ")
                .append(defaultText(recipientName, "Thầy/Cô"))
                .append(",\n\n")
                .append(email.heading()).append("\n")
                .append(deadlineTiming(email.daysRemaining())).append("\n\n")
                .append(email.message()).append("\n\n")
                .append("Năm học: ").append(defaultText(email.academicYear(), "-")).append("\n")
                .append("Học kỳ: ").append(defaultText(email.semester(), "-")).append("\n")
                .append("Hạn nộp: ").append(defaultText(email.deadlineText(), "-")).append("\n\n")
                .append("Các môn chưa nộp:\n");

        if (email.missingCourses() != null) {
            for (DeadlineEmailMessage.MissingCourseLine course : email.missingCourses()) {
                text.append("- ")
                        .append(defaultText(course.courseCode(), "-"))
                        .append(" · ")
                        .append(defaultText(course.courseName(), "-"))
                        .append("\n");
            }
        }

        if (!isBlank(absoluteActionUrl)) {
            text.append("\n")
                    .append(defaultText(email.actionLabel(), "Mở danh sách đề cương"))
                    .append(": ")
                    .append(absoluteActionUrl)
                    .append("\n");
        }
        text.append("\n--\nSCSE Curriculum Management System");
        return text.toString();
    }


    public String renderEscalationHtml(
            DeadlineEscalationEmailMessage email,
            String recipientName,
            String absoluteActionUrl) {

        StringBuilder rows = new StringBuilder();
        if (email.overdueInstructors() != null) {
            for (DeadlineEscalationEmailMessage.OverdueInstructorLine instructor
                    : email.overdueInstructors()) {
                String courses = instructor.missingCourses() == null
                        ? "-"
                        : instructor.missingCourses().stream()
                                .map(course -> esc(defaultText(course.courseCode(), "-"))
                                        + " · " + esc(defaultText(course.courseName(), "-")))
                                .reduce((left, right) -> left + "<br>" + right)
                                .orElse("-");
                rows.append("""
                        <tr>
                          <td style="padding:12px 14px;border-top:1px solid #e2e8f0;vertical-align:top">
                            <div style="font-weight:800;color:#0f172a">{{DEPARTMENT}}</div>
                            <div style="font-size:12px;color:#64748b;margin-top:3px">{{DEPARTMENT_NAME}}</div>
                          </td>
                          <td style="padding:12px 14px;border-top:1px solid #e2e8f0;vertical-align:top">
                            <div style="font-weight:700;color:#0f172a">{{INSTRUCTOR}}</div>
                            <div style="font-size:12px;color:#64748b;margin-top:3px">{{EMAIL}}</div>
                          </td>
                          <td style="padding:12px 14px;border-top:1px solid #e2e8f0;vertical-align:top;color:#334155;font-size:13px;line-height:1.6">{{COURSES}}</td>
                        </tr>
                        """
                        .replace("{{DEPARTMENT}}", esc(defaultText(
                                instructor.departmentCode(), "Chưa gán")))
                        .replace("{{DEPARTMENT_NAME}}", esc(defaultText(
                                instructor.departmentName(), "-")))
                        .replace("{{INSTRUCTOR}}", esc(defaultText(
                                instructor.instructorName(), "-")))
                        .replace("{{EMAIL}}", esc(defaultText(
                                instructor.instructorEmail(), "-")))
                        .replace("{{COURSES}}", courses));
            }
        }

        String actionBlock = isBlank(absoluteActionUrl)
                ? ""
                : """
                  <tr><td style="padding:0 32px 30px;text-align:center">
                    <a href="{{ACTION_URL}}" style="display:inline-block;background:#9f1239;color:#ffffff;text-decoration:none;font-size:14px;font-weight:800;padding:13px 24px;border-radius:10px;box-shadow:0 8px 22px rgba(159,18,57,.25)">{{ACTION_LABEL}}</a>
                  </td></tr>
                  """
                        .replace("{{ACTION_URL}}", escAttr(absoluteActionUrl))
                        .replace("{{ACTION_LABEL}}", esc(defaultText(
                                email.actionLabel(),
                                "Mở danh sách đề cương quá hạn")));

        String template = """
                <!doctype html>
                <html lang="vi">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width,initial-scale=1">
                  <title>{{HEADING}}</title>
                </head>
                <body style="margin:0;padding:0;background:#eef2f7;font-family:Inter,Segoe UI,Arial,sans-serif;color:#0f172a">
                  <div style="display:none;max-height:0;overflow:hidden;opacity:0">{{PREHEADER}}</div>
                  <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="background:#eef2f7;padding:28px 12px">
                    <tr><td align="center">
                      <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="max-width:720px;background:#ffffff;border-radius:18px;overflow:hidden;box-shadow:0 14px 38px rgba(15,23,42,.12)">
                        <tr><td style="background:linear-gradient(135deg,#0b1f3a,#173f70 64%,#7f1d1d);padding:26px 34px">
                          <table role="presentation" width="100%"><tr>
                            <td>
                              <div style="color:#bfdbfe;font-size:12px;font-weight:800;letter-spacing:.14em;text-transform:uppercase">SCSE · INTERNATIONAL UNIVERSITY</div>
                              <div style="color:#ffffff;font-size:21px;font-weight:800;margin-top:6px">Curriculum Management System</div>
                            </td>
                            <td align="right"><div style="width:48px;height:48px;border-radius:14px;background:rgba(255,255,255,.12);line-height:48px;text-align:center;color:#fff;font-weight:900">!</div></td>
                          </tr></table>
                        </td></tr>
                        <tr><td style="padding:32px 34px 16px">
                          <div style="font-size:14px;color:#64748b;margin-bottom:10px">Kính gửi <strong style="color:#334155">{{RECIPIENT}}</strong>,</div>
                          <h1 style="margin:0;font-size:25px;line-height:1.3;color:#0f172a">{{HEADING}}</h1>
                          <div style="margin-top:14px;display:inline-block;padding:8px 13px;border-radius:999px;background:#ffe4e6;color:#9f1239;font-size:12px;font-weight:900">QUÁ HẠN {{DAYS_OVERDUE}}</div>
                          <p style="margin:18px 0 0;font-size:15px;line-height:1.75;color:#475569">{{MESSAGE}}</p>
                        </td></tr>
                        <tr><td style="padding:10px 34px 20px">
                          <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="border:1px solid #dbe3ee;border-radius:14px;overflow:hidden">
                            <tr>
                              <td style="padding:16px;background:#f8fafc;border-right:1px solid #e2e8f0"><div style="font-size:11px;font-weight:800;color:#64748b;text-transform:uppercase;letter-spacing:.06em">Phạm vi</div><div style="font-size:18px;font-weight:900;color:#0f172a;margin-top:5px">{{SCOPE}}</div></td>
                              <td style="padding:16px;background:#f8fafc;border-right:1px solid #e2e8f0"><div style="font-size:11px;font-weight:800;color:#64748b;text-transform:uppercase;letter-spacing:.06em">Giảng viên</div><div style="font-size:24px;font-weight:900;color:#9f1239;margin-top:3px">{{INSTRUCTOR_COUNT}}</div></td>
                              <td style="padding:16px;background:#f8fafc"><div style="font-size:11px;font-weight:800;color:#64748b;text-transform:uppercase;letter-spacing:.06em">Môn chưa nộp</div><div style="font-size:24px;font-weight:900;color:#9f1239;margin-top:3px">{{COURSE_COUNT}}</div></td>
                            </tr>
                          </table>
                        </td></tr>
                        <tr><td style="padding:0 34px 20px">
                          <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="border:1px solid #e2e8f0;border-radius:12px;overflow:hidden">
                            <tr><td style="width:34%;padding:10px 16px;background:#f8fafc;color:#64748b;font-size:13px">Năm học</td><td style="padding:10px 16px;background:#f8fafc;color:#0f172a;font-size:13px;font-weight:800">{{ACADEMIC_YEAR}}</td></tr>
                            <tr><td style="padding:10px 16px;border-top:1px solid #e2e8f0;color:#64748b;font-size:13px">Học kỳ</td><td style="padding:10px 16px;border-top:1px solid #e2e8f0;color:#0f172a;font-size:13px;font-weight:800">{{SEMESTER}}</td></tr>
                            <tr><td style="padding:10px 16px;border-top:1px solid #e2e8f0;color:#64748b;font-size:13px">Deadline</td><td style="padding:10px 16px;border-top:1px solid #e2e8f0;color:#0f172a;font-size:13px;font-weight:800">{{DEADLINE}}</td></tr>
                          </table>
                        </td></tr>
                        <tr><td style="padding:0 34px 26px">
                          <div style="font-size:12px;font-weight:900;letter-spacing:.06em;text-transform:uppercase;color:#64748b;margin-bottom:9px">Danh sách cần xử lý</div>
                          <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="border:1px solid #e2e8f0;border-radius:12px;overflow:hidden">
                            <tr><th align="left" style="padding:11px 14px;background:#0f2747;color:#fff;font-size:12px">Bộ môn</th><th align="left" style="padding:11px 14px;background:#0f2747;color:#fff;font-size:12px">Giảng viên</th><th align="left" style="padding:11px 14px;background:#0f2747;color:#fff;font-size:12px">Môn chưa nộp</th></tr>
                            {{ROWS}}
                          </table>
                        </td></tr>
                        {{ACTION_BLOCK}}
                        <tr><td style="border-top:1px solid #e2e8f0;background:#f8fafc;padding:19px 34px;color:#64748b;font-size:12px;line-height:1.65">
                          Đây là thông báo escalation tự động theo FR-05.7. Vui lòng kiểm tra tình trạng nộp đề cương và thực hiện điều phối theo thẩm quyền.
                        </td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """;

        return template
                .replace("{{PREHEADER}}", esc(email.message()))
                .replace("{{RECIPIENT}}", esc(defaultText(recipientName, "Thầy/Cô")))
                .replace("{{HEADING}}", esc(email.heading()))
                .replace("{{DAYS_OVERDUE}}", esc(overdueTiming(email.daysOverdue())))
                .replace("{{MESSAGE}}", esc(email.message()))
                .replace("{{SCOPE}}", esc(defaultText(email.scopeLabel(), "Toàn khoa")))
                .replace("{{INSTRUCTOR_COUNT}}", String.valueOf(email.overdueInstructorCount()))
                .replace("{{COURSE_COUNT}}", String.valueOf(email.missingCourseCount()))
                .replace("{{ACADEMIC_YEAR}}", esc(defaultText(email.academicYear(), "-")))
                .replace("{{SEMESTER}}", esc(defaultText(email.semester(), "-")))
                .replace("{{DEADLINE}}", esc(defaultText(email.deadlineText(), "-")))
                .replace("{{ROWS}}", rows.toString())
                .replace("{{ACTION_BLOCK}}", actionBlock);
    }

    public String renderEscalationText(
            DeadlineEscalationEmailMessage email,
            String recipientName,
            String absoluteActionUrl) {
        StringBuilder text = new StringBuilder();
        text.append("Kính gửi ")
                .append(defaultText(recipientName, "Thầy/Cô"))
                .append(",\n\n")
                .append(email.heading()).append("\n")
                .append("Quá hạn ").append(overdueTiming(email.daysOverdue())).append("\n\n")
                .append(email.message()).append("\n\n")
                .append("Phạm vi: ").append(defaultText(email.scopeLabel(), "Toàn khoa")).append("\n")
                .append("Năm học: ").append(defaultText(email.academicYear(), "-")).append("\n")
                .append("Học kỳ: ").append(defaultText(email.semester(), "-")).append("\n")
                .append("Deadline: ").append(defaultText(email.deadlineText(), "-")).append("\n")
                .append("Giảng viên quá hạn: ").append(email.overdueInstructorCount()).append("\n")
                .append("Môn chưa nộp: ").append(email.missingCourseCount()).append("\n\n")
                .append("Danh sách cần xử lý:\n");

        if (email.overdueInstructors() != null) {
            for (DeadlineEscalationEmailMessage.OverdueInstructorLine instructor
                    : email.overdueInstructors()) {
                text.append("- [")
                        .append(defaultText(instructor.departmentCode(), "Chưa gán"))
                        .append("] ")
                        .append(defaultText(instructor.instructorName(), "-"))
                        .append(": ");
                if (instructor.missingCourses() == null
                        || instructor.missingCourses().isEmpty()) {
                    text.append("-");
                } else {
                    text.append(instructor.missingCourses().stream()
                            .map(course -> defaultText(course.courseCode(), "-")
                                    + " · " + defaultText(course.courseName(), "-"))
                            .reduce((left, right) -> left + "; " + right)
                            .orElse("-"));
                }
                text.append("\n");
            }
        }

        if (!isBlank(absoluteActionUrl)) {
            text.append("\n")
                    .append(defaultText(email.actionLabel(), "Mở danh sách đề cương quá hạn"))
                    .append(": ")
                    .append(absoluteActionUrl)
                    .append("\n");
        }
        text.append("\n--\nSCSE Curriculum Management System");
        return text.toString();
    }

    private String overdueTiming(long daysOverdue) {
        if (daysOverdue <= 0) {
            return "trong hôm nay";
        }
        if (daysOverdue == 1) {
            return "1 ngày";
        }
        return daysOverdue + " ngày";
    }

    private String deadlineTiming(long daysRemaining) {
        if (daysRemaining <= 0) {
            return "Đến hạn hôm nay";
        }
        if (daysRemaining == 1) {
            return "Còn 1 ngày";
        }
        return "Còn " + daysRemaining + " ngày";
    }

    private String metadataRow(String label, String value) {
        return """
                <tr>
                  <td style="width:34%;padding:10px 16px;border-top:1px solid #e2e8f0;color:#64748b;font-size:13px">{{LABEL}}</td>
                  <td style="padding:10px 16px;border-top:1px solid #e2e8f0;color:#0f172a;font-size:13px;font-weight:650">{{VALUE}}</td>
                </tr>
                """
                .replace("{{LABEL}}", esc(label))
                .replace("{{VALUE}}", esc(defaultText(value, "-")));
    }

    private String[] palette(EmailTone tone) {
        EmailTone safeTone = tone == null ? EmailTone.INFO : tone;
        return switch (safeTone) {
            case SUCCESS -> new String[]{"#047857", "#d1fae5", "rgba(4,120,87,.28)"};
            case WARNING -> new String[]{"#b45309", "#fef3c7", "rgba(180,83,9,.25)"};
            case DANGER -> new String[]{"#be123c", "#ffe4e6", "rgba(190,18,60,.25)"};
            case INFO -> new String[]{"#1d4ed8", "#dbeafe", "rgba(29,78,216,.25)"};
        };
    }

    private String esc(String value) {
        return HtmlUtils.htmlEscape(defaultText(value, ""));
    }

    private String escAttr(String value) {
        return HtmlUtils.htmlEscape(defaultText(value, ""), "UTF-8");
    }

    private String defaultText(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
