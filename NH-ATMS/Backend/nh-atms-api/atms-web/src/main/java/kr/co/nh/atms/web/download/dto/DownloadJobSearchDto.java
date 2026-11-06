package kr.co.nh.atms.web.download.dto;

import kr.co.nh.atms.common.page.PageRequest;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/** 다운로드 작업/배포 이력 검색 조건. */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class DownloadJobSearchDto extends PageRequest {
    private Long pkgId;
    private String termGroupId;
    private String termId;
    private String execType;   // I/B
    private String status;     // W/S/C/F/X
    private String fromDate;   // YYYYMMDD
    private String toDate;     // YYYYMMDD
}
