package kr.co.nh.atms.web.download.dto;

import kr.co.nh.atms.common.page.PageRequest;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/** 다운로드 패키지(일괄다운로드관리) 검색 조건. */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class DownloadPackageSearchDto extends PageRequest {
    private String atmCoCd;        // 기기업체
    private String modelCd;        // 모델
    private String pkgType;        // 유형(A/G/M)
    private String confirmStatus;  // 결재여부(N/Y/R)
    private String useYn;          // 사용여부
}
