package kr.co.nh.atms.web.code.dto;

import lombok.Data;

/** T_CODE 조회 응답 DTO */
@Data
public class CodeItemDto {
    private String langCd;
    private String groupCd;
    private String code;
    private String name;
    private Integer orderNo;
}
