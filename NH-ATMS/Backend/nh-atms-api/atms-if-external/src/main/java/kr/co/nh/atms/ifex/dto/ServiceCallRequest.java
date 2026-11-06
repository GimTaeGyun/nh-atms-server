package kr.co.nh.atms.ifex.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 출동 요청 (serviceCallRequest.do / EM_01001120).
 */
@Data
public class ServiceCallRequest {

    @NotBlank(message = "termGroupId(영업점코드)는 필수입니다.")
    private String termGroupId;

    @NotBlank(message = "termId(단말번호)는 필수입니다.")
    private String termId;

    private String errCd;
    private String vendorCd;

    @NotBlank(message = "faultDesc(장애내용)는 필수입니다.")
    private String faultDesc;

    /** 출동업체구분 (1:기기 2:경비 3:용역 4:협력) */
    private String providerTypeCd;
    /** 출동요청구분코드 (1:수동 2:민원 3:자동 4:이상개국) — 미지정 시 1(수동) */
    private String requestTypeCd;
    /** 출동요청사유코드 */
    private String requestReasonCd;
    /** 고객대기여부 (0:미대기 1:대기) */
    private String customerWaitFlag;
    /** 출동요원명 */
    private String servicePerson;
    /** 출동요원전화번호 */
    private String servicePhoneNo;
    /** 표준장애코드(장애그룹) */
    private String errGroupCd;
    /** 집계분류코드(장애대분류) */
    private String faultClassCd;
    /** 장애발생일자(yyyyMMdd) — 미지정 시 요청일 */
    private String jnlDate;
    /** 장애발생시각 — 미지정 시 요청시각 */
    private String jnlTime;
}
