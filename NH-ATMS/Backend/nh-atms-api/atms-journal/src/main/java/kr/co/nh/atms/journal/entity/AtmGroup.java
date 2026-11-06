package kr.co.nh.atms.journal.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * T_ATM_GROUP - 기기그룹(조합) 마스터.
 * PK: termGroupId
 *
 * 12(개국) 저널 수신 시 미등록 조합이면 임시 기기그룹('미등록영업점')을 자동 생성한다.
 * (atmr insert_ignore_T_ATMGROUP 정합)
 *   PARENT_ID/SUPER_ID 는 CHAR(3) 이고 termGroupId 는 5자리이므로 임시노드는 null 로 둔다.
 *   (atmr 는 MySQL 자동절삭으로 termGroupId 를 넣으나 Oracle 은 길이초과 에러)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AtmGroup {
    private String termGroupId;     // CHAR(5) 영업점코드
    private String termGroupName;   // CHAR(40) 영업점명('미등록영업점')
    private Integer groupLevel;     // NUMBER(10) 그룹레벨(=parseInt(termGroupId))
    private String parentId;        // CHAR(3) 상위그룹(임시노드 null)
    private String superId;         // CHAR(3) 최상위그룹(임시노드 null)
}
