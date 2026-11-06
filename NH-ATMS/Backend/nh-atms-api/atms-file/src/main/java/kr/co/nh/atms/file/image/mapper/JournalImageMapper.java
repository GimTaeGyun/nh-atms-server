package kr.co.nh.atms.file.image.mapper;

import kr.co.nh.atms.file.image.entity.JournalImage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface JournalImageMapper {

    /** 이미지 메타 upsert (Oracle MERGE, PK 충돌 시 경로 갱신). */
    int upsert(JournalImage img);

    /** 저널 4PK 단위 이미지 메타 목록. */
    List<JournalImage> selectByJournal(@Param("termGroupId") String termGroupId,
                                       @Param("termId") String termId,
                                       @Param("jnlDate") String jnlDate,
                                       @Param("jnlTime") String jnlTime);

    /** 단일 이미지 메타 (4PK + imageType). */
    JournalImage selectOne(@Param("termGroupId") String termGroupId,
                           @Param("termId") String termId,
                           @Param("jnlDate") String jnlDate,
                           @Param("jnlTime") String jnlTime,
                           @Param("imageType") String imageType);
}
