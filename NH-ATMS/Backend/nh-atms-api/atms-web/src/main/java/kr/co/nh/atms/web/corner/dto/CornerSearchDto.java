package kr.co.nh.atms.web.corner.dto;

import kr.co.nh.atms.common.page.PageRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CornerSearchDto extends PageRequest {
    private String termGroupId;
    private String cornerCd;
    private String cornerName;
    private String cornerTypeCd;
    private String cornerStatus;
}
