package kr.co.nh.atms.web.notice.dto;

import kr.co.nh.atms.common.page.PageRequest;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class NoticeSearchDto extends PageRequest {
    private String catCd;
    private String subject;
    private String enterUserId;
    private String bankCd;
    private String useYn;
    private String startDate;
    private String endDate;
}
