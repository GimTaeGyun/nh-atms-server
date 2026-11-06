package kr.co.nh.atms.web.atmmodel.dto;

import kr.co.nh.atms.common.page.PageRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AtmModelSearchDto extends PageRequest {
    private String modelCd;
    private String modelName;
    private String atmCoCd;
    private String atmTypeCd;
    private String useYn;
}
