package kr.co.nh.atms.ifex.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 동시다발 장애 통보 (manyFailureRequest.do / EM_01001180).
 */
@Data
public class ManyFailureRequest {

    @NotBlank(message = "termGroupId(영업점코드)는 필수입니다.")
    private String termGroupId;

    /** 장애 대상 단말번호 목록 */
    @NotEmpty(message = "termIds(단말번호 목록)는 필수입니다.")
    private List<String> termIds;

    private String errCd;
    private String faultDesc;

    /** 관리기준임계치 (3자리) — 전문 04 */
    private String faultThreshold;
    /** 장애발생매체(장치ID) — 전문 13 */
    private String errDevice;
    /** 수신업체코드 — 미지정 시 대표 단말의 기기업체코드(ATM_CO_CD) */
    private String vendorCd;
}
