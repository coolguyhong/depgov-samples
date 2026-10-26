// SMP-C02: ObjectMapper 출력; SMP-C03: JsonFactory streaming; SMP-C08: JsonCodec.write/read 정적 호출.
package com.depgov.sample.order.app;

import com.depgov.common.convert.JsonCodec;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class SummaryWriter {
    private final ObjectMapper mapper = new ObjectMapper();

    public void write(Path output, Summary summary) throws IOException {
        // 공통 codec의 DTO 왕복을 거친 결과를 ObjectMapper로 읽기 좋게 저장한다.
        String encoded = JsonCodec.write(summary);
        Summary decoded = JsonCodec.read(encoded, Summary.class);
        Path absolute = output.toAbsolutePath();
        Files.createDirectories(absolute.getParent());
        mapper.writerWithDefaultPrettyPrinter().writeValue(absolute.toFile(), decoded);
    }

    public String streamingLine(Summary summary) throws IOException {
        StringWriter output = new StringWriter();
        JsonFactory factory = new JsonFactory();
        try (JsonGenerator generator = factory.createGenerator(output)) {
            generator.writeStartObject();
            generator.writeNumberField("orderCount", summary.orderCount());
            generator.writeNumberField("subtotal", summary.subtotal());
            generator.writeNumberField("tax", summary.tax());
            generator.writeNumberField("total", summary.total());
            generator.writeEndObject();
        }
        return output.toString();
    }

    public record Summary(int orderCount, long subtotal, long tax, long total, List<OrderSummary> orders) {
    }

    public record OrderSummary(String orderId, String customerId, long subtotal, long tax, long total,
                               List<String> attachmentExtensions, String partnerResponse) {
    }
}
