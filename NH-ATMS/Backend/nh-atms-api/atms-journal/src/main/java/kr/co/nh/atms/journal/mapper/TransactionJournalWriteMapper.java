package kr.co.nh.atms.journal.mapper;

import kr.co.nh.atms.journal.entity.TransactionJournal;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TransactionJournalWriteMapper {
    int insert(TransactionJournal t);
}
