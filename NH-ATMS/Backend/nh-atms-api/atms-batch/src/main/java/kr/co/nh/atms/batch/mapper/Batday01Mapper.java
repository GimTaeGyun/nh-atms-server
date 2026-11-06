package kr.co.nh.atms.batch.mapper;

import kr.co.nh.atms.batch.dto.RunHistoryRow;
import kr.co.nh.atms.batch.dto.StopHistoryRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * batday01 (장애율/가동률 집계) 매퍼 — AS-IS atmb sqlmap/batday01.xml 이식.
 * 결과 테이블: T_ATM_RUN_HISTORY / T_ATM_ERR_GROUP_HISTORY (resources/sql/stat-tables-oracle.sql)
 */
@Mapper
public interface Batday01Mapper {

    /** 대상 기기 건수(대상건수 기록용) */
    long countRunHistoryTarget();

    /** 기존 가동률 이력 삭제 (멱등) */
    int deleteRunHistory(@Param("baseDate") String baseDate);

    /** 기존 장애그룹 이력 삭제 (멱등) */
    int deleteErrGroupHistory(@Param("baseDate") String baseDate);

    /** 가동률 산출 대상 기기 + 디바이스/분류별 장애 건수·시간 (AS-IS sel_faultdata) */
    List<RunHistoryRow> selectFaultData(@Param("baseDate") String baseDate);

    /** 당일 발생 장애(개국 FO 제외) — 장애경과시간 산출용 (AS-IS sel_t_atmstophistory) */
    List<StopHistoryRow> selectStopHistoryOfDay(Map<String, Object> param);

    /** 전일 이전 발생 + 당일 복구 장애 (AS-IS sel_t_atmstophistory_before) */
    List<StopHistoryRow> selectStopHistoryBefore(Map<String, Object> param);

    /** 당일 거래건수 (AS-IS sel_t_transactionjournal) */
    long countTransactions(Map<String, Object> param);

    /** 가동률 이력 1건 적재 */
    int insertRunHistory(RunHistoryRow row);

    /** 일별 기기·장애그룹별 장애건수 집계 적재 (AS-IS ins_t_atmerrgrouphistory_from_select) */
    int insertErrGroupHistory(@Param("baseDate") String baseDate);
}
