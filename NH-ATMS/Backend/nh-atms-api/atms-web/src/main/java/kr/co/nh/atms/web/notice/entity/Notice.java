package kr.co.nh.atms.web.notice.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** T_NOTICE — PK: catCd + seqNo */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Notice {
    private String  catCd;
    private Long    seqNo;
    private String  subject;
    private String  content;
    private Long    hit;
    private Integer step;
    private Integer depth;
    private String  enterDate;
    private String  enterUserId;
    private String  enterUserName;
    private String  enterTermGroupId;
    private String  attachedFileFlag;
    private Long    parentSeqNo;
    private String  bankCd;
    private String  targetUser;
    private String  useYn;
    private String  startDate;
    private String  endDate;
}
