package kr.co.nh.atms.web.atmgroup.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** T_ATM_GROUP (기기그룹마스터) — PK: termGroupId */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AtmGroup {
    private String termGroupId;
    private String termGroupName;
    private Integer groupLevel;
    private String parentId;
    private String superId;
}
