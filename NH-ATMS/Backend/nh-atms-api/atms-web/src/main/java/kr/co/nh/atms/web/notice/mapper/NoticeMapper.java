package kr.co.nh.atms.web.notice.mapper;

import kr.co.nh.atms.web.notice.dto.NoticeSearchDto;
import kr.co.nh.atms.web.notice.entity.Notice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NoticeMapper {
    List<Notice> selectList(NoticeSearchDto search);
    long countList(NoticeSearchDto search);
    Notice selectOne(@Param("catCd") String catCd, @Param("seqNo") Long seqNo);
    Long nextSeqNo(@Param("catCd") String catCd);
    int insert(Notice n);
    int update(Notice n);
    int delete(@Param("catCd") String catCd, @Param("seqNo") Long seqNo);
    int increaseHit(@Param("catCd") String catCd, @Param("seqNo") Long seqNo);
}
