package kr.co.nh.atms.web.journalattachment.entity;

import lombok.Data;

@Data
public class JournalImage {
    private String termGroupId;
    private String termId;
    private String jnlDate;
    private String jnlTime;
    private String imageType;
    private String imagePath;
}
