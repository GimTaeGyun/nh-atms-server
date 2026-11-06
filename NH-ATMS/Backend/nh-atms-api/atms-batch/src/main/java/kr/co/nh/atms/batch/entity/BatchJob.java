package kr.co.nh.atms.batch.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * T_BATCH_JOB — 배치작업 마스터.
 * PK: procName
 * cron 기반 스케줄 + 사용여부/정렬/최근 실행시각 관리.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchJob {
    private String procName;        // 프로세스명 (PK)
    private String jobDesc;         // 작업설명
    private String cronExpr;        // Cron Expression
    private String hostName;        // 기동 호스트 (NULL=전체)
    private String jobStatus;       // 작업상태 (IDLE/RUNNING)
    private String lastStartDate;   // 최근 시작일 yyyyMMdd
    private String lastStartTime;   // 최근 시작시간 HHmmss
    private String lastEndDate;     // 최근 종료일
    private String lastEndTime;     // 최근 종료시간
    private String useFlag;         // 사용여부 (Y/N)
    private Integer sortOrder;      // 정렬순서
    private LocalDateTime regDt;    // 등록일
    private LocalDateTime updDt;    // 수정일
}
