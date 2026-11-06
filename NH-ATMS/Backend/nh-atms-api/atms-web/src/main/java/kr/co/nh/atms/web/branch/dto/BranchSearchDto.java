package kr.co.nh.atms.web.branch.dto;

import kr.co.nh.atms.common.page.PageRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BranchSearchDto extends PageRequest {
    private String termGroupId;
    private String branchCd;
    private String branchName;
    private String branchTypeCd;
}
