package kr.co.nh.atms.web.operationjournal.dto;

import kr.co.nh.atms.common.page.PageRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OperationJournalSearchDto extends PageRequest {
    private String termGroupId;
    private String termId;
    private String operationCd;   // 11/12/21/22/31/99 ...
    private String keyword;       // OPERATION_ACTION LIKE %keyword%
    private String fromDate;      // YYYYMMDD
    private String toDate;        // YYYYMMDD
}
