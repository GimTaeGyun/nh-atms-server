package kr.co.nh.atms.journal.mapper;

import kr.co.nh.atms.journal.entity.FaultAnalysis;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * T_FAULT_ANALYSIS(장애 이력) 적재/조치 매퍼.
 * (atmr insert/update_T_FAULTANALYSIS 정합)
 */
@Mapper
public interface FaultAnalysisWriteMapper {

    /** 장애 이력 1건 적재(PK 중복 시 무시). */
    int insert(FaultAnalysis fa);

    /** 장애 해소 - 미완료 장애건의 조치완료일시/상태 갱신. */
    int relieve(@Param("termGroupId") String termGroupId,
                @Param("termId") String termId,
                @Param("finishedDate") String finishedDate,
                @Param("finishedTime") String finishedTime);
}
