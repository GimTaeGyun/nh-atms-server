package kr.co.nh.atms.web.journal.mapper;

import kr.co.nh.atms.web.journal.dto.JournalSearchDto;
import kr.co.nh.atms.web.journal.entity.Journal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface JournalMapper {
    List<Journal> selectList(JournalSearchDto search);
    long countList(JournalSearchDto search);
    Journal selectOne(@Param("termGroupId") String termGroupId,
                      @Param("termId") String termId,
                      @Param("jnlDate") String jnlDate,
                      @Param("jnlTime") String jnlTime);
}
