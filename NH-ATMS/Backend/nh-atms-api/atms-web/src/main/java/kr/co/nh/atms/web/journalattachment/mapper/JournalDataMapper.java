package kr.co.nh.atms.web.journalattachment.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface JournalDataMapper {
    String selectData(@Param("termGroupId") String termGroupId,
                      @Param("termId") String termId,
                      @Param("jnlDate") String jnlDate,
                      @Param("jnlTime") String jnlTime);
}
