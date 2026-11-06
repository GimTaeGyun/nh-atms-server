package kr.co.nh.atms.web.transactionjournal.mapper;

import kr.co.nh.atms.web.transactionjournal.dto.TransactionJournalSearchDto;
import kr.co.nh.atms.web.transactionjournal.entity.TransactionJournal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TransactionJournalMapper {
    List<TransactionJournal> selectList(TransactionJournalSearchDto search);
    long countList(TransactionJournalSearchDto search);
    TransactionJournal selectOne(@Param("termGroupId") String termGroupId,
                                 @Param("termId") String termId,
                                 @Param("jnlDate") String jnlDate,
                                 @Param("jnlTime") String jnlTime);
}
