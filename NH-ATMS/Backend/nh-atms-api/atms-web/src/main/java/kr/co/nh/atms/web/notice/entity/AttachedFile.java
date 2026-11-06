package kr.co.nh.atms.web.notice.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** T_ATTACHED_FILE — 공지사항 첨부파일 PK: catCd + seqNo + fileNo */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AttachedFile {
    private String catCd;
    private Long   seqNo;
    private Long   fileNo;
    private String attachedFileName;
    private Long   attachedFileSize;
}
