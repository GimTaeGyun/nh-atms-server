package kr.co.nh.atms.web.faultfrequent.dto;

import lombok.Data;

/**
 * 장애다발기기 조회 결과 (T_ATM_MONITOR 기기별 집계).
 */
@Data
public class FaultFrequent {
    private String termGroupId;    // 영업점코드
    private String termId;         // 단말번호
    private Long totalCnt;         // 전체 모니터링 건수
    private Long faultCnt;         // 장애 발생 건수
    private Double faultRate;      // 장애율(%)
    private String lastFaultDate;  // 최근 장애 일자 (YYYYMMDD)
    private String lastFaultTime;  // 최근 장애 시각
    private String lastErrCd;      // 최근 장애코드
}
