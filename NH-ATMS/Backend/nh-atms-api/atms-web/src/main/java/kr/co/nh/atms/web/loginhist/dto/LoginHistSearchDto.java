package kr.co.nh.atms.web.loginhist.dto;

import kr.co.nh.atms.common.page.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class LoginHistSearchDto extends PageRequest {
    private String userId;
    private String loginResult;   // S/F/null=전체
    private String fromDt;        // yyyy-MM-dd
    private String toDt;          // yyyy-MM-dd
}
