package com.scse.curriculum.common.security;

import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.temporal.Temporal;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class XssInputValidator {

    /*
     * Các field bảo mật không phải dữ liệu hiển thị ra UI.
     * Không kiểm tra nội dung password/token để tránh làm thay đổi
     * hoặc từ chối mật khẩu hợp lệ có ký tự đặc biệt.
     */
    private static final Set<String> SECRET_FIELDS = Set.of(
            "password",
            "passwordHash",
            "currentPassword",
            "newPassword",
            "confirmPassword",
            "token",
            "idToken",
            "accessToken",
            "refreshToken",
            "secret",
            "authorization"
    );

    private static final Pattern SCRIPT_TAG =
            Pattern.compile(
                    "(?i)<\\s*/?\\s*script\\b"
            );

    private static final Pattern DANGEROUS_TAG =
            Pattern.compile(
                    "(?i)<\\s*/?\\s*(iframe|object|embed|svg|math|meta|link)\\b"
            );

    private static final Pattern EVENT_HANDLER =
            Pattern.compile(
                    "(?i)\\bon[a-z]{3,}\\s*=\\s*['\"]?"
            );

    private static final Pattern SCRIPT_PROTOCOL =
            Pattern.compile(
                    "(?i)\\b(?:javascript|vbscript)\\s*:\\s*"
                            + "(?:alert|confirm|prompt|document|window|"
                            + "location|eval|fetch|void|\\()"
            );

    private static final Pattern DATA_HTML =
            Pattern.compile(
                    "(?i)data\\s*:\\s*text/html"
            );

    private static final Pattern CSS_EXPRESSION =
            Pattern.compile(
                    "(?i)expression\\s*\\("
            );

    private static final Pattern ENCODED_SCRIPT =
            Pattern.compile(
                    "(?i)%3c\\s*/?\\s*script"
            );

    public void validate(Object body) {

        IdentityHashMap<Object, Boolean> visited =
                new IdentityHashMap<>();

        inspect(
                body,
                null,
                visited
        );
    }

    private void inspect(
            Object value,
            String fieldName,
            IdentityHashMap<Object, Boolean> visited) {

        if (value == null) {
            return;
        }

        if (fieldName != null
                && isSecretField(fieldName)) {

            return;
        }

        if (value instanceof String text) {
            validateString(
                    text,
                    fieldName
            );
            return;
        }

        if (isSimpleValue(value)) {
            return;
        }

        if (visited.containsKey(value)) {
            return;
        }

        visited.put(value, Boolean.TRUE);

        if (value instanceof Map<?, ?> map) {

            for (Map.Entry<?, ?> entry :
                    map.entrySet()) {

                String childName =
                        entry.getKey() == null
                                ? null
                                : entry.getKey().toString();

                inspect(
                        entry.getValue(),
                        childName,
                        visited
                );
            }

            return;
        }

        if (value instanceof Iterable<?> iterable) {

            for (Object item : iterable) {
                inspect(
                        item,
                        fieldName,
                        visited
                );
            }

            return;
        }

        Class<?> type = value.getClass();

        if (type.isArray()) {

            int length =
                    Array.getLength(value);

            for (int i = 0; i < length; i++) {
                inspect(
                        Array.get(value, i),
                        fieldName,
                        visited
                );
            }

            return;
        }

        /*
         * Chỉ reflection vào object của project.
         * Không cố truy cập nội bộ java.*, Spring, Hibernate...
         */
        Package objectPackage =
                type.getPackage();

        if (objectPackage == null
                || !objectPackage.getName()
                .startsWith("com.scse.curriculum")) {

            return;
        }

        inspectFields(
                value,
                type,
                visited
        );
    }

    private void inspectFields(
            Object object,
            Class<?> type,
            IdentityHashMap<Object, Boolean> visited) {

        Class<?> current = type;

        while (current != null
                && current != Object.class) {

            Field[] fields =
                    current.getDeclaredFields();

            for (Field field : fields) {

                if (Modifier.isStatic(
                        field.getModifiers())
                        || field.isSynthetic()) {

                    continue;
                }

                String fieldName =
                        field.getName();

                if (isSecretField(fieldName)) {
                    continue;
                }

                try {
                    field.setAccessible(true);

                    Object child =
                            field.get(object);

                    inspect(
                            child,
                            fieldName,
                            visited
                    );

                } catch (IllegalAccessException ignored) {
                    /*
                     * Không làm request lỗi chỉ vì một field
                     * không thể reflection.
                     */
                }
            }

            current =
                    current.getSuperclass();
        }
    }

    private void validateString(
            String value,
            String fieldName) {

        if (value == null
                || value.isBlank()) {

            return;
        }

        /*
         * Decode HTML entity trước khi kiểm tra để bắt:
         * &lt;script&gt;...
         */
        String normalized =
                HtmlUtils.htmlUnescape(value);

        boolean unsafe =
                SCRIPT_TAG
                        .matcher(normalized)
                        .find()
                || DANGEROUS_TAG
                        .matcher(normalized)
                        .find()
                || EVENT_HANDLER
                        .matcher(normalized)
                        .find()
                || SCRIPT_PROTOCOL
                        .matcher(normalized)
                        .find()
                || DATA_HTML
                        .matcher(normalized)
                        .find()
                || CSS_EXPRESSION
                        .matcher(normalized)
                        .find()
                || ENCODED_SCRIPT
                        .matcher(normalized)
                        .find();

        if (!unsafe) {
            return;
        }

        String location =
                fieldName == null
                        ? ""
                        : " ở trường '" + fieldName + "'";

        throw new UnsafeInputException(
                "Dữ liệu chứa nội dung HTML/script "
                        + "không an toàn"
                        + location
                        + "."
        );
    }

    private boolean isSecretField(
            String fieldName) {

        for (String secret :
                SECRET_FIELDS) {

            if (secret.equalsIgnoreCase(
                    fieldName)) {

                return true;
            }
        }

        return false;
    }

    private boolean isSimpleValue(
            Object value) {

        return value instanceof Number
                || value instanceof Boolean
                || value instanceof Character
                || value instanceof Enum<?>
                || value instanceof UUID
                || value instanceof Temporal;
    }
}