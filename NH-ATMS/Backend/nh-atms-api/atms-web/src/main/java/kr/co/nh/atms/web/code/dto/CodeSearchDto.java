package kr.co.nh.atms.web.code.dto;

import kr.co.nh.atms.common.page.PageRequest;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CodeSearchDto extends PageRequest {
    private String langCd;
    private String groupCd;
    private String code;
    private String name;
}
