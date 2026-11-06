package kr.co.nh.atms.journal.mapper;

import kr.co.nh.atms.journal.entity.Branch;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * T_BRANCH(본지점 마스터) 등록 매퍼.
 * 12(개국) 저널 수신 시 미등록 조합의 임시 영업점 자동생성에 사용.
 * (atmr select_count_T_BRANCH / insert_ignore_T_BRANCH 정합)
 */
@Mapper
public interface BranchWriteMapper {

    /** 해당 조합(term_group_id)의 영업점 존재 건수(0=신규조합). */
    int countByGroup(@Param("termGroupId") String termGroupId);

    /** 임시 영업점 등록(PK 중복 시 무시). */
    int insertIgnore(Branch branch);
}
