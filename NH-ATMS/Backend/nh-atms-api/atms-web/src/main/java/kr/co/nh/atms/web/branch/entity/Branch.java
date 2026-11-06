package kr.co.nh.atms.web.branch.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * T_BRANCH (조합정보)
 * PK: termGroupId + branchCd + branchName
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Branch {
    private String termGroupId;
    private String branchCd;
    private String branchName;
    private String branchTypeCd;
    private String zipcode;
    private String address;
    private String detailAddress;
    private String phoneNo;
    private String faxNo;
    private String partnerCd;
    private String blngSotyCd;
    private String sotyNm;
    private String termGroupName;
    private String addrSeq;
}
