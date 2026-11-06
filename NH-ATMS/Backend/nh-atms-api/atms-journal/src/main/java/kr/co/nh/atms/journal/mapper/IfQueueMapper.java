package kr.co.nh.atms.journal.mapper;

import kr.co.nh.atms.journal.entity.IfQueue;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IfQueueMapper {

    /** 큐 적재 (selectKey 로 queueSeq 채번). */
    int insert(IfQueue q);

    /** 대기(W) 건을 오래된 순으로 limit 만큼 조회. */
    List<IfQueue> selectPending(@Param("limit") int limit);

    /** 처리 결과 반영 (status/resultCd/resultMsg/retryCnt/sentDt). */
    int updateResult(IfQueue q);
}
