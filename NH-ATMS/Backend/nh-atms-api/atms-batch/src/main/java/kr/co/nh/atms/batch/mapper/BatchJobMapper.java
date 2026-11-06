package kr.co.nh.atms.batch.mapper;

import kr.co.nh.atms.batch.dto.BatchJobSearchRequest;
import kr.co.nh.atms.batch.entity.BatchJob;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BatchJobMapper {

    /** 목록(페이징) */
    List<BatchJob> selectList(BatchJobSearchRequest req);

    /** 목록 카운트 */
    long countList(BatchJobSearchRequest req);

    /** 단건 */
    BatchJob selectByProcName(@Param("procName") String procName);

    /** 사용중(USE_FLAG='Y') 전체 — 스케줄러 기동용 */
    List<BatchJob> selectEnabled();

    /** 신규 등록 */
    int insert(BatchJob job);

    /** 수정 */
    int update(BatchJob job);

    /** 삭제 */
    int delete(@Param("procName") String procName);

    /** 실행 시작 시 상태/시작시각 갱신 */
    int updateRunStart(@Param("procName") String procName,
                       @Param("startDate") String startDate,
                       @Param("startTime") String startTime);

    /** 실행 종료 시 상태/종료시각 갱신 */
    int updateRunEnd(@Param("procName") String procName,
                     @Param("endDate") String endDate,
                     @Param("endTime") String endTime);
}
