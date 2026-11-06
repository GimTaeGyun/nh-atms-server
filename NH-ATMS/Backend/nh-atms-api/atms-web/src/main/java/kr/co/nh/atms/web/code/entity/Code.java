package kr.co.nh.atms.web.code.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** T_CODE (공통코드) — PK: langCd + groupCd + code */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Code {
    private String langCd;
    private String groupCd;
    private String code;
    private String name;
    private Integer orderNo;
}
