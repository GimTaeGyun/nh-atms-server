package kr.co.nh.atms.ifex.mapper;

import kr.co.nh.atms.ifex.entity.FaultManagement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FaultManagementMapper {

    /** 장애관리 등록 (FAULT_SEQ 채번) */
    int insert(FaultManagement fault);

    /** 단건 조회 */
    FaultManagement selectBySeq(@Param("faultSeq") Long faultSeq);

    /** 상태/내용 변경 */
    int update(FaultManagement fault);

    /** 송신 후 출동상태코드 갱신 (레거시 updateFaultManagement_at_01001120 — 1:출동요청 0:미처리) */
    int updateDispatchStatus(@Param("faultSeq") Long faultSeq,
                             @Param("dispatchStatusCd") String dispatchStatusCd);

    /** 도착예정 수신 갱신 (EM 01001130) */
    int updateDeparture(FaultManagement fault);

    /** 도착통보 수신 갱신 (EM 01001140) */
    int updateArrival(FaultManagement fault);

    /** 조치결과 수신 갱신 (EM 01001150 / 01001190) */
    int updateRepairResult(FaultManagement fault);
}
