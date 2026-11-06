package kr.co.nh.atms.web.transactionjournal.dto;

import kr.co.nh.atms.common.page.PageRequest;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TransactionJournalSearchDto extends PageRequest {
    private String termGroupId;
    private String termId;
    private String trType;          // 단일 거래종류 (WD0001/DP0001/TR0001/BL0001/WD0002 등)
    private List<String> trTypes;   // 다중 거래종류 (입금/출금 화면 등 여러 종류 동시 조회). 지정 시 trType보다 우선.
    private String accountNo;
    private String cardData;
    private String trResult;        // 0000 정상, 그 외 실패
    private String fromDate;        // YYYYMMDD
    private String toDate;          // YYYYMMDD
    private Boolean errorOnly;      // true 면 TR_RESULT <> '0000'
}
