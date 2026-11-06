package kr.co.nh.atms.journal.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * T_ATM_HISTORY - 기기 설치/변경 이력.
 * PK: machineSno + updateDate + updateTime
 *
 * 12(개국) 시 기기를 신규 등록(updateCd='1')하거나
 * 제조번호 변경(updateCd='3')이 감지되면 1건 적재한다.
 * (atmr insert_T_ATMHISTORY 정합)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AtmHistory {
    private String machineSno;    // VARCHAR2(20) 제조일련번호
    private String updateDate;    // CHAR(8) 변경일
    private String updateTime;    // CHAR(6) 변경시간
    private String termGroupId;   // CHAR(5) 영업점코드
    private String termId;        // VARCHAR2(20) 단말번호
    private String atmCoCd;       // CHAR(2) 기기업체코드
    private String modelCd;       // CHAR(3) 모델코드
    private String updateCd;      // CHAR(1) 변경구분(1:신규, 3:제조번호변경)
    private String updateDesc;    // VARCHAR2(255) 변경내용
}
