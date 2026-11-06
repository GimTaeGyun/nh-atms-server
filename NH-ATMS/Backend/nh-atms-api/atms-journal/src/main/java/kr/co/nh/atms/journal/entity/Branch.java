package kr.co.nh.atms.journal.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * T_BRANCH - 본지점(영업점) 마스터.
 * PK: termGroupId + branchCd + branchName
 *
 * 12(개국) 저널 수신 시 미등록 조합이면 임시 영업점('미등록영업점')을 자동 생성한다.
 * (atmr insert_ignore_T_BRANCH 정합)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Branch {
    private String termGroupId;   // CHAR(5) 영업점코드
    private String branchCd;      // VARCHAR2(5) 본지점코드('01')
    private String branchName;    // VARCHAR2(200) 본지점명('미등록영업점')
    private String branchTypeCd;  // CHAR(1) 본지점구분('1')
}
