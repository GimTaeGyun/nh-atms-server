package kr.co.nh.atms.common.exception;

import kr.co.nh.atms.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        log.warn("BusinessException: [{}] {}", e.getCode(), e.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(e.getCode(), e.getMessage()));
    }

    /**
     * 요청 본문 역직렬화 중 BusinessException(예: @EncryptedField 복호화 실패 CRYPTO_*)이 나면
     * Jackson 이 감싸서 여기로 온다 → 원래 코드로 400. 그 밖의 본문 오류는 기존과 같이 500 처리.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotReadable(HttpMessageNotReadableException e) {
        for (Throwable t = e; t != null && t.getCause() != t; t = t.getCause()) {
            if (t instanceof BusinessException be) {
                return handleBusinessException(be);
            }
        }
        return handleException(e);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException e) {
        log.warn("인증 실패: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.fail("401", "아이디 또는 비밀번호가 올바르지 않습니다."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("시스템 오류: ", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail("500", "시스템 오류가 발생했습니다."));
    }
}
