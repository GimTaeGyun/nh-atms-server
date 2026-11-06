package kr.co.nh.atms.web.serviceprovider.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * T_SERVICE_PROVIDER (외부 사업자/업체 관리)
 * PK: providerCd
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceProvider {
    private String providerCd;
    private String knbankFlag;
    private String providerName;
    private String providerTypeCd;
    private String person;
    private String phoneNo;
    private String faxNo;
    private String email;
    private String zipCd;
    private String address;
    private String sendType;
    private String faultSendType;
    private String useYn;
    private String wooriFlag;
}
