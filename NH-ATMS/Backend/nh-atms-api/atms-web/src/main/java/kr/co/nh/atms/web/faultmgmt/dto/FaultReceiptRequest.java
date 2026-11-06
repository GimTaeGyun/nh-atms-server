package kr.co.nh.atms.web.faultmgmt.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 기기장애접수(출동요청) 등록 요청.
 * FAULT_STATUS='O'(접수), CALL_DATE/CALL_TIME=현재시각으로 적재된다.
 */
@Getter
@Setter
public class FaultReceiptRequest {

    /** 영업점(금고)코드. */
    @NotBlank(message = "영업점(금고)을 선택하세요.")
    private String termGroupId;

    /** 기번(단말번호). */
    @NotBlank(message = "기번을 선택하세요.")
    private String termId;

    /** 출동요청사유 (장애유형). */
    private String faultType;

    /** 장애코드(선택). */
    private String errCd;

    /** 출동업체(처리업체)코드. */
    private String vendorCd;

    /** 출동요청내용(장애내용). */
    private String faultDesc;
}
