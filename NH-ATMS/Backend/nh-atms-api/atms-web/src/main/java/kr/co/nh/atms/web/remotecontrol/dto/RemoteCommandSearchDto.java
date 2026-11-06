package kr.co.nh.atms.web.remotecontrol.dto;

import kr.co.nh.atms.common.page.PageRequest;
import lombok.Getter;
import lombok.Setter;

/**
 * 원격제어 명령 발행이력 검색 조건.
 */
@Getter
@Setter
public class RemoteCommandSearchDto extends PageRequest {
    private String termGroupId;   // 영업점(금고)코드
    private String termId;        // 기번(단말번호)
    private String cmdType;       // 명령유형
    private String status;        // 처리상태 (W/A/S/F/T/C)
    private String fromDate;      // 발행일자 시작 (YYYYMMDD)
    private String toDate;        // 발행일자 종료 (YYYYMMDD)
}
