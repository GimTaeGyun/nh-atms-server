package kr.co.nh.atms.journal.mapper;

import kr.co.nh.atms.journal.entity.AtmGroup;
import org.apache.ibatis.annotations.Mapper;

/**
 * T_ATM_GROUP(기기그룹 마스터) 등록 매퍼.
 * 12(개국) 저널 수신 시 미등록 조합의 임시 기기그룹 자동생성에 사용.
 * (atmr insert_ignore_T_ATMGROUP 정합)
 */
@Mapper
public interface AtmGroupWriteMapper {

    /** 임시 기기그룹 등록(PK 중복 시 무시). */
    int insertIgnore(AtmGroup atmGroup);
}
