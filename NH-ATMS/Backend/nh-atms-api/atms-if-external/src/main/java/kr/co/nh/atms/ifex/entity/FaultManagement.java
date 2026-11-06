package kr.co.nh.atms.ifex.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 장애관리(출동요청/상태) 마스터 (T_FAULT_MANAGEMENT).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaultManagement {
    private Long faultSeq;
    private String globalId;
    private String termGroupId;
    private String termId;
    private String faultType;
    private String errCd;
    private String faultStatus;
    private String callDate;
    private String callTime;
    private String doneDate;
    private String doneTime;
    private String vendorCd;
    private String faultDesc;
    /** 출동업체구분(1:기기, 2:경비, 3:용역) */
    private String providerTypeCd;
    /** 장애대분류코드 */
    private String faultClassCd;
    /** 장애그룹코드 */
    private String errGroupCd;
    /** 저널발생일 */
    private String jnlDate;
    /** 저널발생시간 */
    private String jnlTime;
    /** 요청구분(1:수동 2:민원 3:자동 4:이상개국) */
    private String requestTypeCd;
    /** 출동요청차수 (동일 장애 N차 출동, 1부터 증가 — 레거시 MANAGE_NO) */
    private Integer manageNo;
    /** 출동요청사유코드 (레거시 REQUEST_REASON_CD) */
    private String requestReasonCd;
    /** 고객대기여부 (0:미대기 1:대기 — 레거시 CUSTOMER_WAIT_FLAG) */
    private String customerWaitFlag;
    /** 경비해제일자 (IQ_04001130 코너입퇴실 — 레거시 SECURITY_OFF_DATE) */
    private String securityOffDate;
    /** 경비해제시각 (IQ_04001130 코너입퇴실 — 레거시 SECURITY_OFF_TIME) */
    private String securityOffTime;
    /** 출동상태코드(3:도착예정 4:도착 5:조치완료 8:미복구) */
    private String dispatchStatusCd;
    /** 도착예정일자 */
    private String departureDate;
    /** 도착예정시각 */
    private String departureTime;
    /** 업체접수자명 */
    private String responsePerson;
    /** 출동요원명 */
    private String servicePerson;
    /** 출동요원전화번호 */
    private String servicePhoneNo;
    /** 코너도착일자 */
    private String arrivalDate;
    /** 코너도착시각 */
    private String arrivalTime;
    /** 출동~도착 경과시간(분) */
    private Long arrivalElapsedTime;
    /** 조치완료일자 */
    private String finishedDate;
    /** 조치완료시각 */
    private String finishedTime;
    /** 조치결과코드(1:조치 0:미조치) */
    private String repairFlag;
    /** 조치불가사유코드 */
    private String failureReasonCd;
    /** 조치의견 */
    private String repairComment;
    /** 도착~조치 경과시간(분) */
    private Long finishElapsedTime;
    private LocalDateTime regDt;
    private LocalDateTime updDt;
}
