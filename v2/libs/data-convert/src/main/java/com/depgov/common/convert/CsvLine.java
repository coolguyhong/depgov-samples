// SMP-C08, SMP-C12: 공통 변환 API 정적 호출; StringUtils 정적 호출로 필드 trim.
package com.depgov.common.convert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;
import org.apache.commons.lang3.StringUtils;

public final class CsvLine {
    private CsvLine() {
    }

    public static List<String> split(String line) {
        if (line == null) {
            return Collections.emptyList();
        }
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        boolean closed = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (quoted) {
                if (ch == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        quoted = false;
                        closed = true;
                    }
                } else {
                    field.append(ch);
                }
            } else if (ch == ',') {
                fields.add(StringUtils.trim(field.toString()));
                field.setLength(0);
                closed = false;
            } else if (closed) {
                if (!Character.isWhitespace(ch)) {
                    throw new IllegalArgumentException("Unexpected text after CSV quote");
                }
            } else if (ch == '"') {
                if (!StringUtils.isBlank(field)) {
                    throw new IllegalArgumentException("Unexpected quote in CSV field");
                }
                field.setLength(0);
                quoted = true;
            } else {
                field.append(ch);
            }
        }
        if (quoted) {
            throw new IllegalArgumentException("Unclosed CSV quote");
        }
        fields.add(StringUtils.trim(field.toString()));
        return fields;
    }

    public static String join(List<String> fields) {
        if (fields == null) {
            return "";
        }
        StringJoiner line = new StringJoiner(",");
        for (String field : fields) {
            String value = StringUtils.defaultString(field);
            if (StringUtils.containsAny(value, ',', '"', '\r', '\n')) {
                value = '"' + value.replace("\"", "\"\"") + '"';
            }
            line.add(value);
        }
        return line.toString();
    }
}
