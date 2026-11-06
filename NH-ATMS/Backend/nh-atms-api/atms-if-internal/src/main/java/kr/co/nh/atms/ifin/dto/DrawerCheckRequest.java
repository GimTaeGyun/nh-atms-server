package kr.co.nh.atms.ifin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 시재 조회 요청 (레거시 getDrawerCheck.do / ATMONST10100).
 */
@Data
public class DrawerCheckRequest {

    @NotBlank(message = "termGroupId(영업점코드)는 필수입니다.")
    private String termGroupId;

    @NotBlank(message = "termId(단말번호)는 필수입니다.")
    private String termId;

    /** 조회 기준일자(yyyyMMdd). 미지정 시 당일. */
    private String baseDate;

    /**
     * 조회 대상 텔러번호 목록(각 3자, 최대 30건 - AS-IS teller_no[] occur 반복).
     * 미지정 시 termId 1건으로 조회.
     */
    private List<String> tellerNos;
}
