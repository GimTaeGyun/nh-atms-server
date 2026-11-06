package kr.co.nh.atms.journal.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * T_FAULT_MAP - 장치 상태코드별 장애 분류 맵핑(조회 전용).
 * PK: langCd + deviceId + moduleTypeCd + statusCd
 *
 * 21/22 장애저널 처리 시 device 의 상태 바이트를 이 맵과 대조하여
 * 장애여부(faultFlag)·장애그룹(errGroupCd)·장애분류(faultClassCd)를 판정한다.
 * (atmr select_T_ATMUNIT JOIN T_FAULTMAP 정합)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaultMap {
    private String langCd;        // CHAR(3) 언어코드
    private String deviceId;      // CHAR(1) 장치ID
    private String moduleTypeCd;  // CHAR(2) 모듈구분코드
    private String statusCd;      // CHAR(1) 상태코드
    private String faultClassCd;  // CHAR(2) 장애대분류코드
    private String errGroupCd;    // CHAR(2) 장애그룹코드
    private String faultFlag;     // CHAR(1) 장애여부(1:기기장애, 2:운영장애)
    private String autoRequest;   // CHAR(1) 자동출동대상여부
    private String displayFlag;   // CHAR(1) 상태표시여부
    private String monitorFlag;   // CHAR(1) 모니터링여부
    private String statusName;    // VARCHAR2(40) 상태표시명
}
