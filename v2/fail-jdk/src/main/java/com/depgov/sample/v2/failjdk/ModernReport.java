package com.depgov.sample.v2.failjdk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.apache.commons.lang3.StringUtils;

public final class ModernReport {
    private ModernReport() {
    }

    public sealed interface ReportResult permits Report {
    }

    public record Report(String title, int rows) implements ReportResult {
    }

    public static String render(Object result) {
        if (result instanceof Report report) {
            return report.title() + ": " + report.rows();
        }
        throw new IllegalArgumentException("Unsupported report result");
    }

    public static void main(String[] args) throws IOException {
        String input = """
                {"title": " Monthly report ", "rows": 3}
                """;
        JsonNode data = new ObjectMapper().readTree(input);
        ReportResult result = new Report(
                StringUtils.trimToEmpty(data.get("title").asText()),
                data.get("rows").asInt());
        System.out.println(render(result));
    }
}
