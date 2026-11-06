package kr.co.nh.atms.ifex.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 동시다발장애 조치결과 수신 (EM_01001190, 대외업체 -> ATMS).
 *
 * <p>AS-IS 는 T_REPEATEDLYFAULTATM(FREQ_FAULT_SNO)을 갱신했으나, TO-BE 는 동시다발장애
 * 처리요청(EM_01001180) 송신 시 출동요청번호로 실어보낸 FAULT_SEQ 로
 * T_FAULT_MANAGEMENT 를 갱신한다. AS-IS 정합: 기기사(vend_cd 선두 '1')인 경우만 반영.</p>
 */
@Data
public class ManyFailureResultRequest {

    /** 출동요청번호 (EM_01001180 송신 시의 FAULT_SEQ) */
    @NotNull(message = "faultSeq(출동요청번호)는 필수입니다.")
    private Long faultSeq;

    /** 송신업체코드 (전문 헤더 vend_cd) */
    private String vendorCd;

    /** 조치일자(yyyyMMdd) */
    private String finishedDate;
    /** 조치시각(HHmmss) */
    private String finishedTime;
    /** 조치자명 */
    private String repairPerson;
    /** 메모사항 (조치내용 서술) */
    private String memo;

    /** ATMS 전문일련번호 (전문 헤더 atms_sn) */
    private String atmsSn;
    /** 업체 전문일련번호 (전문 헤더 vend_sn) */
    private String vendSn;
}
