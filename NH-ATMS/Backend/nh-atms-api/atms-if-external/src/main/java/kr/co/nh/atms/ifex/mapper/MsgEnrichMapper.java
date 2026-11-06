package kr.co.nh.atms.ifex.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

/**
 * 대외 전문 바디 조립용 enrichment / 부가 이력 매퍼.
 *
 * <p>레거시 atme ExternalInterfaceMapper 의 아래 쿼리를 신규 스키마로 이식:</p>
 * <ul>
 *   <li>selectFaultManagement (+_at_01001160/_at_01001170 통합)  → {@link #selectServiceCallInfo}</li>
 *   <li>selectCorner                                             → {@link #selectCorner}</li>
 *   <li>selectAtmDeviceStatusAndServiceProvider                  → {@link #selectMachineInfo}</li>
 *   <li>selectRepeatedlyFaultAtm (T_REPEATEDLYFAULTATM 폐지)     → {@link #selectManyFailureAtmInfo}</li>
 *   <li>insertAtmMonitor                                         → {@link #insertAtmMonitor}</li>
 *   <li>updateAtmStatus_clear                                    → {@link #updateAtmStatusClear}</li>
 *   <li>insertCornerTimeInfo / updateCorner / updateFaultManagement_at_04001130
 *       → {@link #insertCornerTimeInfo} / {@link #updateCornerGuardStatus} / {@link #updateFaultSecurityOff}</li>
 * </ul>
 */
@Mapper
public interface MsgEnrichMapper {

    /**
     * 출동요청/취소/변경 전문 조립용 장애관리+기기+코너 정보 (레거시 selectFaultManagement 계열).
     * 키: faultSeq. 출동차수구분(PROVIDER_TYPE_CD 1/4 → '2' 그외 '1') 포함.
     */
    Map<String, Object> selectServiceCallInfo(@Param("faultSeq") Long faultSeq);

    /** 코너변경(SM_02001160) 전문 조립용 코너 정보. 키: termGroupId / cornerCd. */
    Map<String, Object> selectCorner(Map<String, Object> params);

    /** 기기정보통보(IQ_04001140) 전문 조립용 (ADD_INFO / PROVIDER_NAME / ATM_CO_CD). 키: termGroupId / termId. */
    Map<String, Object> selectMachineInfo(Map<String, Object> params);

    /** 동시다발장애(EM_01001180) 전문 조립용 대표 단말 기기/코너 정보. 키: termGroupId / termId. */
    Map<String, Object> selectManyFailureAtmInfo(Map<String, Object> params);

    /** 기기상태이력 생성 (레거시 insertAtmMonitor — AGENT_TYPE '9'). 키: termGroupId / termId / atmStatus. */
    int insertAtmMonitor(Map<String, Object> params);

    /**
     * 자동화기기 최종 출동정보 클리어 (레거시 updateAtmStatus_clear).
     * 키: termGroupId / termId / vendorType(1:기기 3:용역 그외:경비) / faultClassCd(50:Key지원).
     */
    int updateAtmStatusClear(Map<String, Object> params);

    /** 코너입퇴실시간정보 생성 (IQ_04001130 — T_CORNER_TIME_INFO). */
    int insertCornerTimeInfo(Map<String, Object> params);

    /** 코너 경비상태 갱신 (IQ_04001130 — T_CORNER.GUARD_STATUS). */
    int updateCornerGuardStatus(Map<String, Object> params);

    /**
     * 장애관리 경비해제일시 갱신 (레거시 updateFaultManagement_at_04001130 단순화 —
     * 동일 위치구분 단말의 미도착/미해제 진행중 출동 건).
     */
    int updateFaultSecurityOff(Map<String, Object> params);
}
