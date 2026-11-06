package kr.co.nh.atms.journal.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * T_ATM_STATUS(기기상태마스터) 갱신 매퍼.
 *
 * 레거시 atmr 의 각 processor 가 저널 처리 후 마지막에 호출하던
 * update_T_ATMSTATUS_at_{Transaction,Cash,Total,Operation} 에 대응.
 * 신규 시스템은 개국(Open) 시 row 선생성이 아직 없을 수 있으므로
 * MERGE(upsert) 로 안전 처리한다. row 신규 생성 시 SERVICE_STATUS 는
 * NOT NULL 이라 '00'(IN_SERVICE) 기본값을 넣고, 호출별 해당 컬럼만 채운다.
 */
@Mapper
public interface AtmStatusWriteMapper {

    /** 71 거래 → 최종 거래일시 갱신. */
    int touchTransaction(@Param("termGroupId") String termGroupId,
                         @Param("termId") String termId,
                         @Param("jnlDate") String jnlDate,
                         @Param("jnlTime") String jnlTime);

    /** 41 시재 → 최종 현금보충일시 갱신. */
    int touchAddCash(@Param("termGroupId") String termGroupId,
                     @Param("termId") String termId,
                     @Param("jnlDate") String jnlDate,
                     @Param("jnlTime") String jnlTime);

    /** 31 마감 → 최종 일마감일시 갱신. */
    int touchDayTotal(@Param("termGroupId") String termGroupId,
                      @Param("termId") String termId,
                      @Param("jnlDate") String jnlDate,
                      @Param("jnlTime") String jnlTime);

    /** 52 운영 / 21·22 상태 → 기기 운영상태(SERVICE_STATUS) 갱신. */
    int updateServiceStatus(@Param("termGroupId") String termGroupId,
                            @Param("termId") String termId,
                            @Param("serviceStatus") String serviceStatus);

    /**
     * 12 개국 → 오늘(jnlDate) 개국 이력 존재 여부 카운트.
     * (atmr select_count_T_ATMSTATUS : INIT_DATE = jnl_date)
     * INIT_DATE 갱신(openInit) 직전에 호출하여 "오늘 최초 개국" 을 판별한다.
     */
    int countTodayOpen(@Param("termGroupId") String termGroupId,
                       @Param("termId") String termId,
                       @Param("initDate") String initDate);

    /**
     * 12 개국 → 개국정보(SERVICE_STATUS='05', INIT_DATE/INIT_TIME, 세션상태) 갱신.
     * (atmr upsert_T_ATMSTATUS)
     */
    int openInit(@Param("termGroupId") String termGroupId,
                 @Param("termId") String termId,
                 @Param("initDate") String initDate,
                 @Param("initTime") String initTime);

    /**
     * 21/22 상태·장애 → 서비스상태 + 장애여부 + 에러그룹/에러코드 + 세션상태
     * + 디바이스별 상태(CDU/만/오만/CHK/MCR/SPR/JPR) + 장애발생일시 일괄 갱신.
     * (atmr update_T_ATMSTATUS / update_T_ATMSTATUS_at_ManOman 정합 —
     *  ManOman 은 동일 컬럼 재갱신이므로 단일 문으로 통합)
     * serviceStatus 만 null 이면 미갱신, 나머지는 AS-IS 처럼 스냅샷 덮어쓰기(null 클리어).
     * SVR_RECEIVE_DATE/TIME 은 XML 에서 SYSDATE 로 채운다.
     * 장애현황 화면(3100/3101)이 이 컬럼들을 조회한다.
     */
    int updateMonitorStatus(@Param("termGroupId") String termGroupId,
                            @Param("termId") String termId,
                            @Param("serviceStatus") String serviceStatus,
                            @Param("atmFaultStatus") String atmFaultStatus,
                            @Param("opFaultStatus") String opFaultStatus,
                            @Param("errGroupCd") String errGroupCd,
                            @Param("errCd") String errCd,
                            @Param("sessionStatus") String sessionStatus,
                            @Param("cduStatus") String cduStatus,
                            @Param("manStatus") String manStatus,
                            @Param("omanStatus") String omanStatus,
                            @Param("chkStatus") String chkStatus,
                            @Param("mcrStatus") String mcrStatus,
                            @Param("sprStatus") String sprStatus,
                            @Param("jprStatus") String jprStatus,
                            @Param("faultArisedDate") String faultArisedDate,
                            @Param("faultArisedTime") String faultArisedTime);
}
