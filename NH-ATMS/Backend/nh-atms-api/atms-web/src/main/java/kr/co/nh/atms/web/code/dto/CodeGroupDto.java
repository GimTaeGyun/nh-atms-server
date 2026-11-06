package kr.co.nh.atms.web.code.dto;

import lombok.Data;

/** T_CODE_GROUP 조회 응답 DTO */
@Data
public class CodeGroupDto {
    private String langCd;
    private String groupCd;
    private String groupName;
}
