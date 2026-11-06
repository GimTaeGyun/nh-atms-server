package kr.co.nh.atms.common.exception;

import lombok.Getter;

/**
 * 비즈니스 예외.
 * 코드 + 메시지 형태로 GlobalExceptionHandler 에서 일괄 처리.
 */
@Getter
public class BusinessException extends RuntimeException {
    private final String code;

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }
}
