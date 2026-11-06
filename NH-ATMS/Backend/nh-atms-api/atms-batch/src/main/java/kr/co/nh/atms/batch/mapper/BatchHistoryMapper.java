package kr.co.nh.atms.batch.mapper;

import kr.co.nh.atms.batch.entity.BatchHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BatchHistoryMapper {

    /** 이력 등록 (실행 시작 시) → histId 채번 */
    int insert(BatchHistory h);

    /** 이력 갱신 (실행 종료 시) */
    int updateResult(BatchHistory h);

    /** 프로세스별 이력 목록 (최근순) */
    List<BatchHistory> selectByProcName(@Param("procName") String procName,
                                        @Param("offset") int offset,
                                        @Param("limit") int limit);

    /** 프로세스별 이력 카운트 */
    long countByProcName(@Param("procName") String procName);
}
