package kr.co.nh.atms.batch.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * batday09 (일별 자동화기기 이력정보) 매퍼 — AS-IS atmb sqlmap/batday09.xml 이식.
 * 결과 테이블: T_DAILY_REPORT06 (resources/sql/daily-report-tables-oracle.sql)
 */
@Mapper
public interface Batday09Mapper {

    long countReport06Target();

    int deleteReport06(@Param("trDate") String trDate);

    /** 기기 이력(디바이스 적용정보 포함) 스냅샷 적재 (AS-IS ins_t_dailyreport06) */
    int insertReport06(@Param("trDate") String trDate, @Param("bankCd") String bankCd);
}
