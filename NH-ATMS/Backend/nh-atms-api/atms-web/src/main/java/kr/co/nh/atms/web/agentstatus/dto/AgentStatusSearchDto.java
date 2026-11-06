package kr.co.nh.atms.web.agentstatus.dto;

import kr.co.nh.atms.common.page.PageRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AgentStatusSearchDto extends PageRequest {
    private String termGroupId;   // 영업점코드
    private String termId;        // 단말번호
    private String connectFlag;   // 접속여부 (1=접속, 0=해제)
    private String fromDate;      // 기동일자 시작 (YYYYMMDD)
    private String toDate;        // 기동일자 종료 (YYYYMMDD)
}
