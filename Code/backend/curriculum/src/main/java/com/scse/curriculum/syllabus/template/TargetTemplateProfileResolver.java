package com.scse.curriculum.syllabus.template;

import com.scse.curriculum.cohort.entity.Cohort;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TargetTemplateProfileResolver {

    public static final String SOURCE_TEMPLATE = "SOURCE_TEMPLATE";
    public static final String NEW_2027 = "NEW_2027";

    private static final Pattern YEAR_AT_END = Pattern.compile("(20\\d{2})$");

    public String resolve(Cohort cohort) {
        Integer targetYear = resolveTargetYear(cohort);
        if (targetYear != null && targetYear >= 2027) {
            return NEW_2027;
        }

        return SOURCE_TEMPLATE;
    }

    public boolean isNew2027(Cohort cohort) {
        return NEW_2027.equals(resolve(cohort));
    }

    private Integer resolveTargetYear(Cohort cohort) {
        if (cohort == null) {
            return null;
        }

        Integer yearFromGetter = readIntegerGetter(cohort, "getEntryYear", "getYear");
        if (yearFromGetter != null) {
            return yearFromGetter;
        }

        String cohortName = normalize(cohort.getName());
        Matcher matcher = YEAR_AT_END.matcher(cohortName);
        if (matcher.find()) {
            return Integer.valueOf(matcher.group(1));
        }

        return null;
    }

    private Integer readIntegerGetter(Cohort cohort, String... getterNames) {
        for (String getterName : Arrays.asList(getterNames)) {
            try {
                Method method = cohort.getClass().getMethod(getterName);
                Object value = method.invoke(cohort);

                if (value instanceof Number number) {
                    return number.intValue();
                }

                if (value instanceof String text && !text.isBlank()) {
                    return Integer.valueOf(text.trim());
                }
            } catch (ReflectiveOperationException | IllegalArgumentException ignored) {
                // Cohort entity may not expose entryYear/year. Fallback to cohort name.
            }
        }

        return null;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
