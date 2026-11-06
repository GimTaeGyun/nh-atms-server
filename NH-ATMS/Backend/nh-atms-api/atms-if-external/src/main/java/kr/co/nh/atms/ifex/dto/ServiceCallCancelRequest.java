package kr.co.nh.atms.ifex.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 출동 취소 (serviceCallCancel.do / EM_01001160).
 */
@Data
public class ServiceCallCancelRequest {

    @NotNull(message = "faultSeq(장애관리 일련번호)는 필수입니다.")
    private Long faultSeq;

    private String reason;

    /** 출동취소사유코드 (1:민원해결 2:출동등록오류 3:복구) */
    private String cancelReasonCd;
}
