package kr.co.nh.atms.journal.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * T_CASSETTE_STATUS - 카세트 현황 스냅샷.
 * PK: termGroupId + termId + cassetteType + cassetteNo
 *
 * 카세트구분(cassetteType): 1=논리(logical), 2=물리(physical), 4=수표(BCA).
 * 41 시재 / 71 거래 / 21·22 상태 저널이 잔여매수·상태를 동기화한다.
 * (atmr update_T_CASSETTESTATUS_* 정합)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CassetteStatus {
    private String termGroupId;
    private String termId;
    private String cassetteType;  // CHAR(1) 1=논리 2=물리 4=수표
    private String cassetteNo;    // CHAR(5) 카세트 번호(배열 순번)
    private String denomCd;       // VARCHAR2(10) 권종 코드
    private String currencyCd;    // CHAR(3) 통화 코드
    private Long remainCount;     // NUMBER(19) 잔여 매수
    private String status;        // CHAR(2) 카세트 상태
    private String checkNo;       // VARCHAR2(32) 수표 번호(수표 카세트)
}
