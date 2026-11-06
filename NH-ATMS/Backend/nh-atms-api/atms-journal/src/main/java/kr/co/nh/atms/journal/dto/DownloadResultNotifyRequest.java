package kr.co.nh.atms.journal.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 다운로드 결과 통보 (계약: DownloadResultNotify).
 * 에이전트가 펌웨어/파일 다운로드 완료 후 성공/실패를 서버로 통보.
 */
@Data
public class DownloadResultNotifyRequest {

    @NotBlank
    private String atmId;        // 8자리

    @NotBlank
    private String fileName;

    private String version;

    private boolean success;

    private String errorMessage;

    /** 발생시각 ISO-8601 (없으면 서버시각). */
    private String occurredAt;
}
