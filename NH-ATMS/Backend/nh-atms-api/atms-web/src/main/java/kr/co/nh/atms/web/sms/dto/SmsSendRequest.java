package kr.co.nh.atms.web.sms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * SMS 전송 등록 요청.
 * 등록 시 SEND_STATUS='W'(대기)로 적재되어 발송 모듈이 처리한다.
 */
@Getter
@Setter
public class SmsSendRequest {

    /** 영업점(금고)코드(선택). */
    private String termGroupId;

    /** 단말번호(선택). */
    private String termId;

    /** 수신번호(휴대폰번호). */
    @NotBlank(message = "휴대폰번호를 입력하세요.")
    private String recvPhone;

    /** 회신번호. */
    @NotBlank(message = "회신번호를 입력하세요.")
    private String sendPhone;

    /** 전송내용. */
    @NotBlank(message = "전송내용을 입력하세요.")
    private String msgContent;
}
