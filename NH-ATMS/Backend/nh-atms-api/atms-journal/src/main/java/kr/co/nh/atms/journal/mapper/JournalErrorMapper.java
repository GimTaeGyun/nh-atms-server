package kr.co.nh.atms.journal.mapper;

import kr.co.nh.atms.journal.entity.JournalError;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface JournalErrorMapper {
    int insert(JournalError e);

    /** 재처리 대상(STATUS='E' AND RETRY_CNT &lt; maxRetry) 조회. */
    List<JournalError> selectRetryable(@Param("limit") int limit, @Param("maxRetry") int maxRetry);

    /** 재처리 성공 → STATUS='R'. */
    int markResolved(@Param("seq") long seq);

    /** 재시도 초과 → STATUS='F', RETRY_CNT 갱신. */
    int markFailed(@Param("seq") long seq, @Param("retryCnt") int retryCnt);

    /** 재시도 여지 → RETRY_CNT+1 (STATUS='E' 유지). */
    int incrementRetry(@Param("seq") long seq);
}
