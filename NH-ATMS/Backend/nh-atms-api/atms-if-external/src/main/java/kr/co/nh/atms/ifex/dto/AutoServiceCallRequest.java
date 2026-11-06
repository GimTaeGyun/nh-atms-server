package kr.co.nh.atms.ifex.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 자동출동 요청 (대외IF 자동 출동요청 / 레거시 statusFailureJnlExtIFAutoServiceCallHandler).
 * 장애저널(22) 수신 시점의 저널키로 T_FAULT_ANALYSIS 의 자동출동 대상 건을 선정한다.
 */
@Data
public class AutoServiceCallRequest {

    @NotBlank(message = "termGroupId(영업점코드)는 필수입니다.")
    private String termGroupId;

    @NotBlank(message = "termId(단말번호)는 필수입니다.")
    private String termId;

    @NotBlank(message = "jnlDate(저널발생일)는 필수입니다.")
    private String jnlDate;

    @NotBlank(message = "jnlTime(저널발생시간)는 필수입니다.")
    private String jnlTime;
}
