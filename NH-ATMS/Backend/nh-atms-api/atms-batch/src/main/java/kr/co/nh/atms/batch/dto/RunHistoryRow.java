package kr.co.nh.atms.batch.dto;

import lombok.Data;

/**
 * batday01 가동률 집계 1행 — T_ATM_RUN_HISTORY 적재 단위.
 * AS-IS atmb batday01$RunHistory 구조체 이식(카멜케이스 변환).
 *
 * 시간 항목(…FaultTime / totalTargetTime / totalRunTime / delayOpenTime)의 단위는 초.
 * 디바이스별 장애건수/시간(sprFault…~apFault…)과 장애분류별 건수/시간(machine…~etc…)은
 * T_FAULT_ANALYSIS 집계 SQL 에서 채워지고, 가동시간 3종은 Java 로직이 계산한다.
 */
@Data
public class RunHistoryRow {
    private String baseDate;
    private String termGroupId;
    private String termId;
    private String cornerCd;
    private String cornerTypeCd;
    private String operationTypeCd;
    private String locationTypeCd;
    private String areaCd;
    private String serviceStartTime;   // HHmm
    private String serviceEndTime;     // HHmm
    private String atmCoCd;
    private String modelCd;
    private String securityProviderCd;
    private String serviceProviderCd;
    private String openTime;           // HHmmss (당일 개국(FO) 저널시간, 미개국 시 공백)

    private long totalTargetTime;
    private long totalRunTime;
    private long delayOpenTime;

    private long sprFaultCnt;      private long sprFaultTime;
    private long pbFaultCnt;       private long pbFaultTime;
    private long jprFaultCnt;      private long jprFaultTime;
    private long docFaultCnt;      private long docFaultTime;
    private long scnFaultCnt;      private long scnFaultTime;
    private long idcFaultCnt;      private long idcFaultTime;
    private long cdmBillFaultCnt;  private long cdmBillFaultTime;
    private long cdmCoinFaultCnt;  private long cdmCoinFaultTime;
    private long pinFaultCnt;      private long pinFaultTime;
    private long chkFaultCnt;      private long chkFaultTime;
    private long depFaultCnt;      private long depFaultTime;
    private long ttuFaultCnt;      private long ttuFaultTime;
    private long siuFaultCnt;      private long siuFaultTime;
    private long vdmFaultCnt;      private long vdmFaultTime;
    private long camFaultCnt;      private long camFaultTime;
    private long almFaultCnt;      private long almFaultTime;
    private long ceuFaultCnt;      private long ceuFaultTime;
    private long cimBillFaultCnt;  private long cimBillFaultTime;
    private long cimCoinFaultCnt;  private long cimCoinFaultTime;
    private long crdFaultCnt;      private long crdFaultTime;
    private long bcrFaultCnt;      private long bcrFaultTime;
    private long ipmFaultCnt;      private long ipmFaultTime;
    private long apFaultCnt;       private long apFaultTime;
    private long machineFaultCnt;  private long machineFaultTime;
    private long operationFaultCnt;private long operationFaultTime;
    private long empFaultCnt;      private long empFaultTime;
    private long cusFaultCnt;      private long cusFaultTime;
    private long netFaultCnt;      private long netFaultTime;
    private long etcFaultCnt;      private long etcFaultTime;

    private long transactionCnt;
}
