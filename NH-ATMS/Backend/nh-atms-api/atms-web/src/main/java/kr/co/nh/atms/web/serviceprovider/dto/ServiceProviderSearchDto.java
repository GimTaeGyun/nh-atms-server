package kr.co.nh.atms.web.serviceprovider.dto;

import kr.co.nh.atms.common.page.PageRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ServiceProviderSearchDto extends PageRequest {
    private String providerCd;
    private String providerName;
    private String providerTypeCd;
    private String useYn;
}
