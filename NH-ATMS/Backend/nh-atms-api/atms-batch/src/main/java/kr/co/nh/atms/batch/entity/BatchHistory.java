package kr.co.nh.atms.batch.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * T_BATCH_HISTORY — 배치 실행 이력.
 * PK: histId (IDENTITY)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchHistory {
    private Long histId;
    private String procName;
    private String hostName;
    private String runType;     // SCHEDULE / MANUAL
    private String statusCd;    // RUNNING / SUCCESS / FAIL
    private String startDate;   // yyyyMMdd
    private String startTime;   // HHmmss
    private String endDate;
    private String endTime;
    private Long delayTime;     // ms
    private Long targetCnt;
    private Long procCnt;
    private Long errCnt;
    private String errMsg;
    private LocalDateTime regDt;
}
