package kr.co.nh.atms.web.sms.dto;

import kr.co.nh.atms.common.page.PageRequest;
import lombok.Getter;
import lombok.Setter;

/**
 * SMS 발송이력 검색 조건.
 */
@Getter
@Setter
public class SmsSearchDto extends PageRequest {
    private String termGroupId;
    private String recvPhone;
    private String sendStatus;   // W/S/F
    private String fromDate;     // 발송일 시작 (YYYYMMDD)
    private String toDate;       // 발송일 종료 (YYYYMMDD)
}
