package kr.co.nh.atms.ifex.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 영구 채번 마스터(T_SEQ_NO_MASTER) 접근 매퍼.
 * 구 MG SP_GET_SEQNO / SP_UPDATE_SEQNO 의 행 잠금(SELECT ... FOR UPDATE) 의미를 그대로 옮긴다.
 */
@Mapper
public interface SequenceNoMapper {

    /** 현재 일련번호 조회 + 행 잠금. 없으면 null. */
    Long selectBaseNoForUpdate(@Param("type") String type,
                               @Param("c1") String content1,
                               @Param("c2") String content2);

    /** 현재 일련번호 단순 조회 (잠금 없음 — 결번요청 최종일련번호 응답용). 없으면 null. */
    Long selectBaseNo(@Param("type") String type,
                      @Param("c1") String content1,
                      @Param("c2") String content2);

    /** 신규 채번 행 삽입 (content3..5 = '0'). */
    int insert(@Param("type") String type,
               @Param("c1") String content1,
               @Param("c2") String content2,
               @Param("baseNo") long baseNo);

    /** 기존 채번 행 갱신. */
    int updateBaseNo(@Param("type") String type,
                     @Param("c1") String content1,
                     @Param("c2") String content2,
                     @Param("baseNo") long baseNo);

    /** SP_UPDATE_SEQNO 등가: 벤더(005) 일련번호 덮어쓰기. 갱신 행 수 반환. */
    int updateVendorBaseNo(@Param("vendorCd") String vendorCd,
                           @Param("baseNo") long baseNo);

    /** SP_UPDATE_SEQNO 등가: 벤더(005) 일련번호 신규 삽입. */
    int insertVendor(@Param("vendorCd") String vendorCd,
                     @Param("baseNo") long baseNo);
}
