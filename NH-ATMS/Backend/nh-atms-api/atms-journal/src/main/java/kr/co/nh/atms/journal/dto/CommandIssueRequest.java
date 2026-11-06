package kr.co.nh.atms.journal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.Map;

/**
 * 운영자/시스템의 원격제어 명령 발행 요청.
 * 발행된 명령은 T_AGENT_COMMAND 에 STATUS=W 로 적재되어 에이전트 폴링을 대기한다.
 */
@Data
public class CommandIssueRequest {

    /** 명령유형 (계약 enum). */
    @NotBlank
    @Pattern(regexp = "STAT|IMGC|DLD|FCOL|RST|RBT|POFF|STOP|OPEN_OP|JMOD|MSG|TMOD|CLS|RCMD|GDL",
            message = "지원하지 않는 명령유형입니다.")
    private String type;

    /** 명령 파라미터(선택). */
    private Map<String, Object> payload;

    /** 실행 제한(초). 미지정 시 180. */
    private Integer timeoutSec;

    /** 발행자 ID(선택). 미지정 시 SYSTEM. */
    private String regUserId;
}
