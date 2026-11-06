package kr.co.nh.atms.ifin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 장애 SMS 발송 요청 (레거시 sendFaultSms.do / SMSONSS10200).
 */
@Data
public class FaultSmsRequest {

    @NotBlank(message = "termGroupId(영업점코드)는 필수입니다.")
    private String termGroupId;

    @NotBlank(message = "termId(단말번호)는 필수입니다.")
    private String termId;

    @NotBlank(message = "recvPhone(수신번호)는 필수입니다.")
    private String recvPhone;

    /** 발신번호(미지정 시 시스템 기본 발신번호) */
    private String sendPhone;

    /** 장애코드 */
    private String errCd;

    @NotBlank(message = "msgContent(메시지)는 필수입니다.")
    private String msgContent;
}
