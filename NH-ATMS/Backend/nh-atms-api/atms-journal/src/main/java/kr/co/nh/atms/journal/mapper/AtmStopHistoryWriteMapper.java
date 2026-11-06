package kr.co.nh.atms.journal.mapper;

import kr.co.nh.atms.journal.entity.AtmStopHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * T_ATM_STOP_HISTORY(기기 운영중지 이력) 적재/복구 매퍼.
 * (atmr insert/update_T_ATMSTOPHISTORY 정합)
 */
@Mapper
public interface AtmStopHistoryWriteMapper {

    /** 운영중지 장애 1건 적재(PK 중복 시 무시). */
    int insertStop(AtmStopHistory stop);

    /** 정상복구 - 미완료 중지건의 조치완료일시 채움. */
    int recover(@Param("termGroupId") String termGroupId,
                @Param("termId") String termId,
                @Param("finishedDate") String finishedDate,
                @Param("finishedTime") String finishedTime);
}
