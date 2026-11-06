package kr.co.nh.atms.batch.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 배치 집계용 매퍼 (batday02 / batday30).
 * 모든 집계는 멱등(재실행 안전)하도록 대상일자 DELETE 후 INSERT 로 처리한다.
 * ※ batday01 은 AS-IS 시간 기반 가동률 로직 재구현으로 Batday01Mapper 로 분리됨.
 */
@Mapper
public interface StatMapper {

    /* ---- batday02 : 거래실적 집계 ---- */

    /** 거래저널 소스 건수 (대상건수 — 성공/실패 무관 당일 전체) */
    long countTxSource(@Param("statDate") String statDate);

    /** 기존 집계 삭제 */
    int deleteTxDailyTotal(@Param("statDate") String statDate);

    /* Reader(selectTxDailyTotalRows)/Writer(insertTxDailyTotalRow)는
       MyBatisCursorItemReader / MyBatisBatchItemWriter 가 statement id 로 직접 사용 */

    /* ---- batday30 : 사용자 부서(금고)명 동기화 ---- */

    /** 부서명 갱신 대상 건수 (DEPT_CD 가 T_ATM_GROUP 에 존재하고 이름이 불일치한 사용자) */
    long countUserDeptSyncTarget();

    /** T_ATM_GROUP.TERM_GROUP_NAME 으로 T_USER.DEPT_NAME 갱신 */
    int updateUserDeptName();
}
