package kr.co.nh.atms.web.code.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** T_CODE_GROUP (공통코드 그룹) — PK: langCd + groupCd */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeGroup {
    private String langCd;
    private String groupCd;
    private String groupName;
}
