package com.scse.curriculum.email;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EmailTemplateServiceTest {

    private final EmailTemplateService service = new EmailTemplateService();

    @Test
    void htmlTemplateEscapesDynamicContentAndContainsAction() {
        WorkflowEmailMessage message = new WorkflowEmailMessage(
                "SYLLABUS_REVISION_REQUESTED",
                "Subject",
                "Cần chỉnh sửa",
                "Yêu cầu chỉnh sửa",
                EmailTone.WARNING,
                "Vui lòng cập nhật <script>alert('x')</script>",
                "Dean & Reviewer",
                "Sửa CLO <b>1</b>",
                "Xem đề cương",
                "/instructor/syllabus/10",
                10,
                "IT001IU",
                "Web & Application",
                "v2.0",
                "2026-2027",
                "1");

        String html = service.renderHtml(
                message,
                "Nguyễn <Admin>",
                "http://localhost:5173/instructor/syllabus/10");

        assertThat(html)
                .contains("Curriculum Management System")
                .contains("http://localhost:5173/instructor/syllabus/10")
                .contains("Nguyễn &lt;Admin&gt;")
                .contains("&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt;")
                .contains("Sửa CLO &lt;b&gt;1&lt;/b&gt;")
                .doesNotContain("<script>alert('x')</script>");
    }

    @Test
    void textTemplateContainsFallbackInformation() {
        WorkflowEmailMessage message = new WorkflowEmailMessage(
                "SYLLABUS_APPROVED",
                "Subject",
                "Đã duyệt",
                "Hoàn tất",
                EmailTone.SUCCESS,
                "Đề cương đã được duyệt.",
                null,
                null,
                "Xem chi tiết",
                null,
                11,
                "IT002IU",
                "Database",
                "v1.0",
                "2026-2027",
                "2");

        String text = service.renderText(message, null, null);

        assertThat(text)
                .contains("Kính gửi Thầy/Cô")
                .contains("IT002IU")
                .contains("Hoàn tất")
                .contains("SCSE Curriculum Management System");
    }
}
