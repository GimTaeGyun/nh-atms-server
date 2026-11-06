package kr.co.nh.atms.web.download.dto;

import kr.co.nh.atms.common.page.PageRequest;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/** 개별다운로드관리 대상 기기 검색 조건. */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class DownloadDeviceSearchDto extends PageRequest {
    private String termGroupId;  // 금고(영업점)
    private String branchCd;     // 본지점
    private String cornerCd;     // 코너
    private String termId;       // 기번
    private String atmCoCd;      // 기기업체
    private String modelCd;      // 모델
}
