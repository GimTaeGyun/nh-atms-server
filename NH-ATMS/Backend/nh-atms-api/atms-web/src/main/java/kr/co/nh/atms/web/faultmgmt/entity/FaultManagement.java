package kr.co.nh.atms.web.faultmgmt.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * T_FAULT_MANAGEMENT — 장애관리(출동요청/상태) 마스터.
 * 운영자가 실시간 모니터링/유지보수 화면에서 기기장애접수(출동요청)를 하면
 * FAULT_STATUS='O'(접수)로 적재된다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaultManagement {
    private Long faultSeq;          // PK (SEQ_T_FAULT_MANAGEMENT)
    private String globalId;
    private String termGroupId;     // 영업점(금고)코드
    private String termId;          // 기번(단말번호)
    private String faultType;       // 출동요청사유 (SERVICE_CALL/MANY_FAILURE/STATUS 등)
    private String errCd;           // 장애코드
    private String faultStatus;     // O=접수,P=진행,C=완료,X=취소
    private String callDate;        // 출동요청일 (YYYYMMDD)
    private String callTime;        // 출동요청시간 (HHMMSS)
    private String doneDate;        // 처리완료일
    private String doneTime;        // 처리완료시간
    private String vendorCd;        // 출동업체(처리업체)코드
    private String faultDesc;       // 출동요청내용(장애내용)
    private LocalDateTime regDt;
    private LocalDateTime updDt;
}
