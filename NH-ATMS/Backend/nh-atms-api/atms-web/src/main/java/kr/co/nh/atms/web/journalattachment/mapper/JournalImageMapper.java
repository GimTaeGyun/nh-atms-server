package kr.co.nh.atms.web.journalattachment.mapper;

import kr.co.nh.atms.web.journalattachment.entity.JournalImage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface JournalImageMapper {
    List<JournalImage> selectImages(@Param("termGroupId") String termGroupId,
                                    @Param("termId") String termId,
                                    @Param("jnlDate") String jnlDate,
                                    @Param("jnlTime") String jnlTime);
}
