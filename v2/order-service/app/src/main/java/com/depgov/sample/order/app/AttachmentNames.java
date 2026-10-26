// SMP-C06: 첨부 파일 확장자 추출은 method reference로만 사용.
package com.depgov.sample.order.app;

import java.util.List;
import org.apache.commons.io.FilenameUtils;

public final class AttachmentNames {
    private AttachmentNames() {
    }

    public static List<String> extensions(List<String> names) {
        return names == null ? List.of() : names.stream().map(FilenameUtils::getExtension).toList();
    }
}
