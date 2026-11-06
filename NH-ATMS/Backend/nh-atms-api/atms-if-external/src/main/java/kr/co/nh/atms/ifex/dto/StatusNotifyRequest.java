package kr.co.nh.atms.ifex.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 개국/장애/상태 통보 요청 (openFailureStatus.do / EM_01001110).
 */
@Data
public class StatusNotifyRequest {

    @NotBlank(message = "termGroupId(영업점코드)는 필수입니다.")
    private String termGroupId;

    @NotBlank(message = "termId(단말번호)는 필수입니다.")
    private String termId;

    /** 저널발생일(YYYYMMDD) — 전문 바디 enrichment 조회키. */
    @NotBlank(message = "jnlDate(저널발생일)는 필수입니다.")
    private String jnlDate;

    /** 저널발생시간(HHMMSSxxxx) — 전문 바디 enrichment 조회키. */
    @NotBlank(message = "jnlTime(저널발생시간)은 필수입니다.")
    private String jnlTime;

    /** 상태유형(OPEN=개국, FAULT=장애, NORMAL=정상복구 등) */
    @NotBlank(message = "statusType(상태유형)은 필수입니다.")
    private String statusType;

    private String errCd;
    private String statusDesc;
}
