package kr.co.nh.atms.batch.dto;

import lombok.Data;

/**
 * batday01 장애경과시간 산출용 T_ATM_STOP_HISTORY 1행.
 * AS-IS atmb batday01$AtmsTopHistory 이식.
 */
@Data
public class StopHistoryRow {
    private String jnlDate;        // 장애 발생일(yyyyMMdd)
    private String jnlTime;        // 장애 발생시간(HHmmss)
    private String finishedDate;   // 복구일 (미복구 시 NULL)
    private String finishedTime;   // 복구시간
    private String deviceId;       // 장애 디바이스
    private String errGroupCd;     // 장애그룹(중분류)코드
}
