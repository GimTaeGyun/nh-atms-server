package kr.co.nh.atms.web.atmstatus.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** T_ATM_STATUS (기기상태마스터) — PK: termGroupId + termId */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AtmStatus {
    private String termGroupId;
    private String termId;
    private String serviceStatus;
    private String initDate;
    private String initTime;
    private String atmFaultStatus;
    private String opFaultStatus;
    private String errGroupCd;
    private String errCd;
    private String atmSessionStatus;
    private String faultArisedDate;
    private String faultArisedTime;
    private String svrReceiveDate;
    private String svrReceiveTime;
    private Long remainAmount;
    private Long checkAmount;
    private String cduStatus;
    private String manStatus;
    private String omanStatus;
    private String chkStatus;
    private String mcrStatus;
    private String sprStatus;
    private String jprStatus;
    private String rjtStatus;
    private String lastTrDate;
    private String lastTrTime;
    private String lastAddCashDate;
    private String lastAddCashTime;
    private String lastDayTotalDate;
    private String lastDayTotalTime;
    private String status;
    private String lastOutCallDate;
    private String lastOutCallTime;
    private Integer lastOutSno;
    private String faultClassCd;
    private String lastAtmCallDate;
    private String lastAtmCallTime;
    private String lastAtmFaultClassCd;
    private Integer lastAtmSno;
    private String lastKeyCallDate;
    private String lastKeyCallTime;
    private String lastKeyFaultClassCd;
    private Integer lastKeySno;
}
