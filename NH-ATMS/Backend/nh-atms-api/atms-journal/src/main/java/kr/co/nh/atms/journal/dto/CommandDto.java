package kr.co.nh.atms.journal.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 에이전트 폴링 응답의 단일 명령 (계약: Command 스키마).
 * payload 는 JSON 객체로 역직렬화하여 전달한다.
 */
@Data
@Builder
public class CommandDto {
    private String cmdId;
    private String type;                  // 명령유형
    private String atmId;                 // termGroupId + termId
    private Map<String, Object> payload;  // 명령 파라미터
    private String issuedAt;              // ISO-8601
    private Integer timeoutSec;
}
