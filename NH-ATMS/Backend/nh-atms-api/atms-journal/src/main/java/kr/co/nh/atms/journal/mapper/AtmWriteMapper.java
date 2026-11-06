package kr.co.nh.atms.journal.mapper;

import kr.co.nh.atms.journal.entity.Atm;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * T_ATM(기기 마스터) 등록/갱신 매퍼.
 * 12(개국) 저널 수신 시 기기 자동 등록/갱신에 사용.
 * (atmr select/insert/update_T_ATM 정합)
 */
@Mapper
public interface AtmWriteMapper {

    /** 등록 여부 확인(0=미등록). */
    int existsByTerm(@Param("termGroupId") String termGroupId,
                     @Param("termId") String termId);

    /** 현재 등록된 수신제조일련번호 조회(미등록 시 null). 개국 시 기기교체 판정 기준. */
    String selectRecvMachineSno(@Param("termGroupId") String termGroupId,
                                @Param("termId") String termId);

    /** 신규 기기 등록. */
    int insert(Atm atm);

    /** 개국 시 변경 가능 컬럼 갱신(제조번호/모델/업체/버전/IP/부가정보). */
    int updateOnOpen(Atm atm);

    /** 개국 공통 후처리: 본지점코드 갱신(branch_cd 유효 시에만 호출). */
    int updateBranchCd(@Param("termGroupId") String termGroupId,
                       @Param("termId") String termId,
                       @Param("branchCd") String branchCd);

    /** 개국 공통 후처리: 부스타입코드 갱신(booth_type_cd 유효 시에만 호출). */
    int updateBoothTypeCd(@Param("termGroupId") String termGroupId,
                          @Param("termId") String termId,
                          @Param("boothTypeCd") String boothTypeCd);
}
