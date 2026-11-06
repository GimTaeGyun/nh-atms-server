package kr.co.nh.atms.journal.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * T_ATM_UNIT_STATUS — 기기 장치(디바이스) 단위 상태/버전 현황.
 * PK = (TERM_GROUP_ID, TERM_ID, DEVICE_ID, AGENT_TYPE)
 * 상태/장애 저널(21/22) 수신 시 디바이스별로 upsert (atmr 1-B).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AtmUnitStatus {
    private String termGroupId;
    private String termId;
    private String deviceId;
    private String agentType;
    private String logicalService;
    private String status;
    private String faultFlag;
    private String jnlDate;
    private String jnlTime;
    private String spVersion;
    private String epVersion;
}
