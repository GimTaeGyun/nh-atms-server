package kr.co.nh.atms.journal.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * T_ATM - 기기 마스터(개국 시 등록/갱신 대상 컬럼만 보유).
 * PK: termGroupId + termId
 *
 * 12(개국) 저널 수신 시 미등록 기기는 신규 등록(asset_flag='2'),
 * 기등록 기기는 제조번호/모델/버전 등을 갱신한다.
 * (atmr select/insert/update_T_ATM 정합)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Atm {
    private String termGroupId;   // CHAR(5) 영업점코드
    private String termId;        // VARCHAR2(20) 단말번호
    private String atmStatus;     // CHAR(2) ATM상태
    private String assetFlag;     // CHAR(1) 자산구분(신규등록 '2')
    private String enterDate;     // CHAR(8) 등록일자
    private String enterTime;     // CHAR(6) 입력시간
    private String atmCoCd;       // CHAR(2) 기기업체코드(vd_cd)
    private String modelCd;       // CHAR(3) 모델코드(md_no)
    private String machineSno;    // VARCHAR2(20) 제조일련번호(sn_no)
    private String appVersion;    // VARCHAR2(32) 기기APP버전(ap_ver)
    private String atmIp;         // CHAR(15) 기기IP주소(etc_info)
    private String ownerCd;       // CHAR(10) 소유자코드(bc_no)
    private String branchCd;      // VARCHAR2(5) 본지점코드(bc_no)
    private String boothTypeCd;   // CHAR(2) 부스타입(bc_no)

    // --- 개국(12) 시 함께 적재하는 부가 컬럼 (atmr insert/update_T_ATM 정합) ---
    private String assetSno;       // VARCHAR2(20) 기기제조일련번호(sn_no)
    private String recvMachineSno; // VARCHAR2(20) 수신제조일련번호(sn_no, 교체판정 기준)
    private String recvAtmIp;      // CHAR(15) 수신기기IP주소(atm_ip)
    private String statusFlag;     // CHAR(1) 상태플래그(agent_type)
    private String tranFlag;       // CHAR(1) 거래플래그(agent_type)
    private String processorType;  // CHAR(1) 프로세서타입(pc_cd)
    private String settleDate;     // CHAR(8) 자동기설치일자(now)
    private String remoteFlag;     // CHAR(1) 원격플래그(agent_type)
    private String protocolType;   // CHAR(1) 프로토콜타입('T')
}
