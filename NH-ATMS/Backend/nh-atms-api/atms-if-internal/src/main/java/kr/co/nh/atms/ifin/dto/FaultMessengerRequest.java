package kr.co.nh.atms.ifin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 장애 메신저 통보 요청 (레거시 sendFaultMessenger.do / ATMBYDD10100, DB2DB).
 */
@Data
public class FaultMessengerRequest {

    @NotBlank(message = "termGroupId(영업점코드)는 필수입니다.")
    private String termGroupId;

    @NotBlank(message = "termId(단말번호)는 필수입니다.")
    private String termId;

    /** 장애코드 */
    private String errCd;

    @NotBlank(message = "msgContent(메시지)는 필수입니다.")
    private String msgContent;

    /** 수신 대상자 ID(메신저). 다건은 콤마 구분. */
    private String recvUserId;
}
