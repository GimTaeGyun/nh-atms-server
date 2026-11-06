package kr.co.nh.atms.web.atm.dto;

import kr.co.nh.atms.common.page.PageRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AtmSearchDto extends PageRequest {
    /** 영업점코드 (LIKE) */
    private String termGroupId;
    /** 단말번호 (LIKE) */
    private String termId;
    /** ATM 상태 (정확히) */
    private String atmStatus;
    /** 기기업체코드 */
    private String atmCoCd;
    /** 모델코드 */
    private String modelCd;
    /** 본지점코드 */
    private String branchCd;
    /** 등록일 시작 (yyyymmdd) */
    private String enterDateFrom;
    /** 등록일 종료 (yyyymmdd) */
    private String enterDateTo;
}
