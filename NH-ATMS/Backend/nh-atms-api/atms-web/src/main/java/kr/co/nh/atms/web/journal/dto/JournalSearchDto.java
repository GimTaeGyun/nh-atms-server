package kr.co.nh.atms.web.journal.dto;

import kr.co.nh.atms.common.page.PageRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JournalSearchDto extends PageRequest {
    private String termGroupId;
    private String termId;
    private String jnlTypeCd;   // 11/12/21/22/31/41/52/71
    private String fromDate;    // YYYYMMDD
    private String toDate;      // YYYYMMDD
}
