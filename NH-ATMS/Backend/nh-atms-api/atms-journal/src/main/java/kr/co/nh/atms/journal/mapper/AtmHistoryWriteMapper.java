package kr.co.nh.atms.journal.mapper;

import kr.co.nh.atms.journal.entity.AtmHistory;
import org.apache.ibatis.annotations.Mapper;

/**
 * T_ATM_HISTORY(기기 설치/변경 이력) 적재 매퍼.
 * 12(개국) 신규등록(update_cd='1') / 제조번호변경(update_cd='3') 시 1건 적재.
 * (atmr insert_T_ATMHISTORY 정합)
 */
@Mapper
public interface AtmHistoryWriteMapper {

    /** 이력 1건 적재(PK 중복 시 무시). */
    int insert(AtmHistory history);
}
