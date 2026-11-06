package kr.co.nh.atms.web.atmmodel.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * T_ATM_MODEL (ATM 기기모델 관리)
 * PK: modelCd
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AtmModel {
    private String modelCd;
    private String modelName;
    private String atmCoCd;
    private String atmTypeCd;
    private String lastAppVer;
    private String lastMwiVer;
    private String jnlWriteMode;
    private String lastAgentVer;
    private Long   maxAmount;
    private String dataSupported;
    private String sprImgSupported;
    private String faceImgSupported;
    private String handImgSupported;
    private String mcrImgSupported;
    private String chkFrontImgSupported;
    private String chkBackImgSupported;
    private String apLogDir;
    private String agentLogDir;
    private String jnlIndexDir;
    private String useYn;
    private String downloadPath;
    private String remoteYn;
    private String exeFilePath;
    private String osCd;
    private String bankModelCd;
    private String dotSupported;
    private String earphoneSupported;
    private String zoomSupported;
    private String wheelchairSupported;
}
