package kr.co.nh.atms.journal.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * T_ATM_DEVICE_STATUS - 기기 디바이스정보(개국정보).
 * PK: termGroupId + termId
 *
 * 12(개국) 저널 수신 시 디바이스 장착/지원 정보를 upsert 한다.
 * (atmr OpenProcessor 5단계 upsert_T_ATMDEVICESTATUS 정합)
 *   - des_st/icc_st/emv_st/ir_st/rf_st/fp_st/50k_st/enc_st -> 디바이스 적용여부
 *   - vd_dt(기기업체 data) -> addInfo
 *   - 2d_bc/1000k_check/pb_auto_flip/pwd -> 바코드/수표/통장넘김/장애우
 *   - etc_info(200byte) 파싱 -> ip/os/cpu/mem/hdd/installDate
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AtmDeviceStatus {
    private String termGroupId;   // CHAR(5) 영업점코드
    private String termId;        // VARCHAR2(20) 단말번호
    private String desApply;      // CHAR(1) DES적용여부(des_st)
    private String icApply;       // CHAR(1) IC적용여부(icc_st)
    private String emvApply;      // CHAR(1) EMV적용여부(emv_st)
    private String irApply;       // CHAR(1) IR적용여부(ir_st)
    private String rfApply;       // CHAR(1) RF적용여부(rf_st)
    private String fpApply;       // CHAR(1) FP(지문)적용여부(fp_st)
    private String omanApply;     // CHAR(1) 오만원권가능여부(50k_st)
    private String encodeApply;   // CHAR(1) 암호화적용여부(enc_st)
    private String addInfo;       // VARCHAR2(200) 부가정보(vd_dt)
    private String barcodeApply;  // CHAR(1) 바코드적용여부(2d_bc)
    private String chkStatus;     // CHAR(1) 수표부상태/백만원수표출금가능여부(1000k_check)
    private String pbAutoTurn;    // CHAR(1) 통장자동넘김여부(pb_auto_flip)
    private String handicapAtm;   // CHAR(1) 장애인용ATM여부(pwd)
    private String ipAddress;     // CHAR(20) IP주소(etc_info[0,20])
    private String osInfo;        // CHAR(50) OS정보(etc_info[20,70])
    private String cpuInfo;       // CHAR(50) CPU정보(etc_info[70,120])
    private String memInfo;       // CHAR(20) 메모리정보(etc_info[120,140])
    private String hdd1Tot;       // CHAR(10) HDD1용량(etc_info[140,150])
    private String hdd1Used;      // CHAR(10) HDD1사용량(etc_info[150,160])
    private String hdd2Tot;       // CHAR(10) HDD2용량(etc_info[160,170])
    private String hdd2Used;      // CHAR(10) HDD2사용량(etc_info[170,180])
    private String installDate;   // VARCHAR2(8) 설치일자(etc_info[180,188])
}
