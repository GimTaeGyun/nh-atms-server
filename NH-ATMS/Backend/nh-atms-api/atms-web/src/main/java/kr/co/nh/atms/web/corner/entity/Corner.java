package kr.co.nh.atms.web.corner.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * T_CORNER (코너/설치장소 마스터)
 * PK: termGroupId + cornerCd
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Corner {
    private String termGroupId;
    private String cornerCd;
    private String cornerName;
    private String cornerTypeCd;
    private String operationTypeCd;
    private String locationTypeCd;
    private String serviceStartTime;
    private String serviceEndTime;
    private String serviceWeekday;
    private String boothTypeCd;
    private Long   boothCount;
    private String manageTypeCd;
    private String outsrcCoCd;
    private String guardCoCd;
    private String guardBrCd;
    private String guardCrtno;
    private String guardStatus;
    private String cornerSafeUseFlag;
    private String cornerStatus;
    private String openDate;
    private String closeDate;
    private String tempClsStartDate;
    private String tempClsEndDate;
    private String tempClsReason;
    private String phoneNo;
    private String zipcode;
    private String area1Cd;
    private String area2Cd;
    private String area3Cd;
    private String detailAddress;
    private String address;
    private String location;
    private Long   leaseDeposit;
    private Long   leaseValue;
    private String checkDepFlag;
    private String checkWitFlag;
    private String lat;
    private String lng;
    private String enterDate;
    private String enterUserId;
    private String cctvSetFlag;
}
