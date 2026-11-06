package kr.co.nh.atms.web.notice.mapper;

import kr.co.nh.atms.web.notice.entity.AttachedFile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AttachedFileMapper {
    List<AttachedFile> selectByNotice(@Param("catCd") String catCd, @Param("seqNo") Long seqNo);
    Long nextFileNo(@Param("catCd") String catCd, @Param("seqNo") Long seqNo);
    int insert(AttachedFile f);
    int delete(@Param("catCd") String catCd, @Param("seqNo") Long seqNo, @Param("fileNo") Long fileNo);
    int deleteByNotice(@Param("catCd") String catCd, @Param("seqNo") Long seqNo);
}
