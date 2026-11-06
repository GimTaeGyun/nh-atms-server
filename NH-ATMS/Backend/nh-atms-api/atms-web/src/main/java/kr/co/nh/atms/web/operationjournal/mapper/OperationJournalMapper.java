package kr.co.nh.atms.web.operationjournal.mapper;

import kr.co.nh.atms.web.operationjournal.dto.OperationJournalSearchDto;
import kr.co.nh.atms.web.operationjournal.entity.OperationJournal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OperationJournalMapper {
    List<OperationJournal> selectList(OperationJournalSearchDto search);
    long countList(OperationJournalSearchDto search);
    OperationJournal selectOne(@Param("termGroupId") String termGroupId,
                               @Param("termId") String termId,
                               @Param("jnlDate") String jnlDate,
                               @Param("jnlTime") String jnlTime);
}
