package kr.co.nh.atms.journal.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * T_FAULT_ANALYSIS - 장애 이력.
 * PK: termGroupId + termId + jnlDate + jnlTime + deviceId + errGroupCd + moduleTypeCd
 *
 * 21/22 장애저널에서 device 상태가 장애로 판정되면 1건 적재한다.
 * (atmr insert_T_FAULTANALYSIS 정합)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaultAnalysis {
    private String termGroupId;   // CHAR(5)
    private String termId;        // VARCHAR2(20)
    private String jnlDate;       // CHAR(8)
    private String jnlTime;       // CHAR(10)
    private String deviceId;      // CHAR(1)
    private String errGroupCd;    // CHAR(2)
    private String moduleTypeCd;  // CHAR(2)
    private String faultClassCd;  // CHAR(2)
    private String errCd;         // VARCHAR2(10)
    private String atmCoCd;       // CHAR(2)
    private String modelCd;       // CHAR(3)
    private String status;        // CHAR(1) 상태
    private String autoRequest;   // CHAR(1) 자동요청여부
    private String requestFlag;   // CHAR(1) 요청여부
    private String bankCd;        // CHAR(3)
    private String accountNo;     // VARCHAR2(64)
    private Long trAmount;        // NUMBER(19)
    private Long checkAmount;     // NUMBER(19)
}
