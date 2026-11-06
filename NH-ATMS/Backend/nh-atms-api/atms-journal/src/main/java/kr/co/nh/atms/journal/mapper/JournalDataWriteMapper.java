package kr.co.nh.atms.journal.mapper;

import kr.co.nh.atms.journal.entity.JournalData;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface JournalDataWriteMapper {
    int insert(JournalData d);
}
