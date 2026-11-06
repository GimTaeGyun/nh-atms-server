package kr.co.nh.atms.file.image.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * T_JOURNAL_IMAGE — 저널 첨부 이미지 메타.
 * PK = (TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE)
 * 실파일은 NAS/디스크에 저장, 본 행에는 경로 메타만 보관 (atmr 1-C).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalImage {
    private String termGroupId;
    private String termId;
    private String jnlDate;
    private String jnlTime;
    private String imageType;   // 1=카드 2=통장 3=명세표 (CHAR 1)
    private String imagePath;   // 저장 절대경로
}
