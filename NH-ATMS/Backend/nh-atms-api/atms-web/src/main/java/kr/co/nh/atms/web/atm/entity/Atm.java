package kr.co.nh.atms.web.atm.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * T_ATM (자동화기기 마스터)
 * PK: termGroupId + termId
 *
 * 출처: 새마을금고 t_atm — Oracle 19C 변환 (T_ATM)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Atm {
    /** 영업점코드 (PK) */
    private String termGroupId;
    /** 단말번호 (PK) */
    private String termId;

    /** ATM 상태 */
    private String atmStatus;
    /** 자산구분 */
    private String assetFlag;
    /** 관리구분코드 */
    private String manageTypeCd;
    /** 설치장소구분코드 */
    private String locationTypeCd;
    /** 설치장소명 */
    private String locationName;
    /** 운영일구분코드 */
    private String serviceDayTypeCd;
    private String serviceStartTime;
    private String serviceEndTime;
    private String serviceStartDate;
    private String serviceEndDate;
    private String enterDate;
    private String enterTime;
    private String phoneNo;
    /** 기기업체코드 */
    private String atmCoCd;
    /** 모델코드 */
    private String modelCd;
    /** 기기 IP */
    private String atmIp;
    private String bpServerIp;
    private String securityProviderCd;
    private String serviceProviderCd;
    private String officeCd;
    private String teamCd;
    private String assetSno;
    private String machineSno;
    private String recvMachineSno;
    private String ownerCd;
    private String appVersion;
    private String statusFlag;
    private String tranFlag;
    private String recvAtmIp;
    private String operateWeek;
    private String introduceDate;
    private String processorType;
    private String settleDate;
    private String disuseDate;
    private String remoteFlag;
    private Long atmCost;
    private String durableYears;
    private Long firstSetupCosts;
    /** 본지점코드 */
    private String branchCd;
    private String protocolType;
    private String boothTypeCd;
    /** 코너코드 */
    private String cornerCd;
}
