package kr.co.nh.atms.ifex.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 출동 변경 (serviceCallChange.do / EM_01001170).
 */
@Data
public class ServiceCallChangeRequest {

    @NotNull(message = "faultSeq(장애관리 일련번호)는 필수입니다.")
    private Long faultSeq;

    private String errCd;
    private String vendorCd;
    private String faultDesc;

    /** 고객대기여부 변경값 (0:미대기 1:대기) — 전문 09 변경구분코드 원천 */
    private String customerWaitFlag;
}
