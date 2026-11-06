package kr.co.nh.atms.web.notice.dto;

import kr.co.nh.atms.web.notice.entity.AttachedFile;
import kr.co.nh.atms.web.notice.entity.Notice;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** 공지사항 + 첨부파일 목록 복합 응답 */
@Data @NoArgsConstructor @AllArgsConstructor
public class NoticeWithFilesDto {
    private Notice notice;
    private List<AttachedFile> files;
}
