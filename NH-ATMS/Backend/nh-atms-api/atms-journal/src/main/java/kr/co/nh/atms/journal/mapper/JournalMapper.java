package kr.co.nh.atms.journal.mapper;

import kr.co.nh.atms.journal.entity.Journal;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface JournalMapper {
    /** PK 충돌 시 무시(IGNORE_ROW_ON_DUPKEY_INDEX 힌트). */
    int insert(Journal j);
}
