package kr.co.nh.atms.web.remotecontrol.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * 운영자 원격제어 명령 발행 요청.
 * 발행된 명령은 T_AGENT_COMMAND 에 STATUS=W 로 적재되어 에이전트 폴링을 대기한다.
 */
@Getter
@Setter
public class RemoteCommandIssueRequest {

    /** 영업점(금고)코드. */
    @NotBlank(message = "영업점(금고)을 선택하세요.")
    private String termGroupId;

    /** 기번(단말번호). */
    @NotBlank(message = "기번을 선택하세요.")
    private String termId;

    /** 명령유형 (계약 enum). */
    @NotBlank
    @Pattern(regexp = "STAT|IMGC|DLD|FCOL|RST|RBT|POFF|STOP|OPEN_OP|JMOD|MSG|TMOD|CLS|RCMD|GDL",
            message = "지원하지 않는 명령유형입니다.")
    private String cmdType;

    /** 명령 파라미터(선택). */
    private Map<String, Object> payload;

    /** 실행 제한(초). 미지정 시 180. */
    private Integer timeoutSec;
}
