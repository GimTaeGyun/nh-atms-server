package kr.co.nh.atms.journal.mapper;

import kr.co.nh.atms.journal.entity.CassetteStatus;
import org.apache.ibatis.annotations.Mapper;

import org.apache.ibatis.annotations.Param;

@Mapper
public interface CassetteStatusWriteMapper {
    /** 카세트 현황 upsert (PK = 단말+카세트구분+카세트번호 기준 MERGE). */
    int upsert(CassetteStatus t);

    /**
     * 21/22 상태저널 → 단말의 카세트 현황 전건 삭제(재생성 직전).
     * (atmr delete_T_CASSETTESTATUS)
     */
    int deleteByTerm(@Param("termGroupId") String termGroupId,
                     @Param("termId") String termId);
}
