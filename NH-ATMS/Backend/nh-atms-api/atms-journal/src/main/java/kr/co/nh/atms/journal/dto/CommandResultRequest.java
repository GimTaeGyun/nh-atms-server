package kr.co.nh.atms.journal.dto;

import lombok.Data;

import java.util.Map;

/**
 * 명령 실행 결과 통보 요청 (계약: CommandResult 스키마).
 * cmdId 는 경로변수로 받으므로 본문에서는 선택.
 */
@Data
public class CommandResultRequest {
    private boolean success;
    private String resultCode;             // 0x30=성공, 그 외=실패
    private Map<String, Object> resultData;
    private String errorMessage;
    private String completedAt;            // ISO-8601 (없으면 서버시각)
}
