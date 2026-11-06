package kr.co.nh.atms.journal.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * T_ATM_STOP_HISTORY - 기기 운영중지 이력.
 * PK: termGroupId + termId + jnlDate + jnlTime + errGroupCd
 *
 * 21/22 장애저널에서 기기가 운영중지(OUT_OF_SERVICE)되면 중지장애를 적재하고,
 * 정상복구(IN_SERVICE)되면 미완료 중지건의 조치완료일시를 채운다.
 * (atmr insert/update_T_ATMSTOPHISTORY 정합)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AtmStopHistory {
    private String termGroupId;   // CHAR(5)
    private String termId;        // VARCHAR2(20)
    private String jnlDate;       // CHAR(8)
    private String jnlTime;       // CHAR(10)
    private String errGroupCd;    // CHAR(2)
    private String deviceId;      // CHAR(1)
    private String faultStatus;   // CHAR(2)
    private String finishedDate;  // CHAR(8)
    private String finishedTime;  // CHAR(6)
    private String exceptFlag;    // CHAR(1)
}
