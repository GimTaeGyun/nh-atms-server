package kr.co.nh.atms.batch.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * batday08 (장애/거래 보고서 집계) 매퍼 — AS-IS atmb sqlmap/batday08.xml 이식.
 * 결과 테이블: T_DAILY_REPORT01~05, 07 (resources/sql/daily-report-tables-oracle.sql)
 */
@Mapper
public interface Batday08Mapper {

    long countReportTarget();

    int deleteReport01(@Param("trDate") String trDate);
    int deleteReport02(@Param("trDate") String trDate);
    int deleteReport03(@Param("trDate") String trDate);
    int deleteReport04(@Param("trDate") String trDate);
    int deleteReport05(@Param("trDate") String trDate);
    int deleteReport07(@Param("trDate") String trDate);

    /** 기기별 장애/거래 집계 (AS-IS dailyReport01) */
    int insertReport01(@Param("trDate") String trDate, @Param("bankCd") String bankCd);

    /** 지역별 상향집계 (AS-IS dailyReport02) */
    int insertReport02(@Param("trDate") String trDate);

    /** 설치장소별 상향집계 (AS-IS dailyReport03) */
    int insertReport03(@Param("trDate") String trDate);

    /** 영업본부별 상향집계 (AS-IS dailyReport04) */
    int insertReport04(@Param("trDate") String trDate);

    /** 기종별 상향집계 (AS-IS dailyReport05) */
    int insertReport05(@Param("trDate") String trDate);

    /** 금고(그룹)별/기종별 상향집계 (AS-IS dailyReport07) */
    int insertReport07(@Param("trDate") String trDate);
}
