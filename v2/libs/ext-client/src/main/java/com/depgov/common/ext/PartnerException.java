// SMP-C09, SMP-C19: PartnerClient 생성·호출 시 HTTP/입출력 실패 전달용 공개 예외.
package com.depgov.common.ext;

public class PartnerException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public PartnerException(String message) {
        super(message);
    }

    public PartnerException(String message, Throwable cause) {
        super(message, cause);
    }
}
