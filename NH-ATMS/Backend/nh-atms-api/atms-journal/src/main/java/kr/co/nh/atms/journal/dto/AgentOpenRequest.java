package kr.co.nh.atms.journal.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 에이전트 기동(open) 요청.
 * atmId = termGroupId(5) + termId(3) = 8자리.
 */
@Data
public class AgentOpenRequest {

    @NotBlank
    private String atmId;       // 8자리 (00107404 형태)
    private String jnlMode;     // 저널 모드
    private String apVersion;   // AP 버전
    private String agVersion;   // 에이전트 버전
    private String osMac;       // MAC 주소
    private String osName;      // OS 정보
}
