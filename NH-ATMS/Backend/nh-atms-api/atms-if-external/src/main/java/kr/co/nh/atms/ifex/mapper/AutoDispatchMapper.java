package kr.co.nh.atms.ifex.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * 자동출동(대외IF 자동 출동요청) 선정 로직 전용 조회 매퍼.
 * 레거시 RecvJnlExtIFMapper(selectFaultAnalysis/selectErrorGroup/selectExistsHoliday/
 * selectAutoRequestCheck/selectAtmJoinCorner/selectExistsFaultmanagement/updateAtmStatus)
 * 를 신규 스키마(T_FAULT_ANALYSIS / T_ERRORGROUP / T_HOLIDAY / T_AUTOREQUESTCHECK /
 * T_ATM / T_CORNER / T_FAULT_MANAGEMENT / T_ATM_STATUS)에 맞춰 이식.
 */
@Mapper
public interface AutoDispatchMapper {

    /** 1) 저널키 기준 미조치 자동출동 대상 장애이력 조회 (T_FAULT_ANALYSIS). */
    List<Map<String, Object>> selectFaultAnalysis(Map<String, Object> params);

    /** 2) 장애그룹별 출동업체 설정 조회 (T_ERRORGROUP). SERVICE/SECURITY/ATM_CALL, ERR_GROUP_NAME. */
    Map<String, Object> selectErrorGroup(Map<String, Object> params);

    /** 3) 휴일 여부 (T_HOLIDAY, GROUP_CD IN (영업점코드,'00001')). */
    boolean existsHoliday(Map<String, Object> params);

    /** 4) 코너 관리/운영 구분에 따른 출동업체 판정 룰 조회 (T_AUTOREQUESTCHECK + T_ATM + T_CORNER). */
    Map<String, Object> selectAutoRequestCheck(Map<String, Object> params);

    /** 5) 선정된 출동업체구분(providerType)에 따른 업체코드 조회 (T_ATM + T_CORNER). */
    String selectProviderCd(Map<String, Object> params);

    /** 6) 최근 2일 내 동일 단말/분류/업체 출동요청 존재 여부 (T_FAULT_MANAGEMENT). */
    boolean existsRecentFaultManagement(Map<String, Object> params);

    /** 출동요청 코멘트용 장애코드명 (T_ERROR_MESSAGE). */
    String selectErrName(Map<String, Object> params);

    /** 출동요청 코멘트용 장치명 (T_ATM_UNIT). */
    String selectDeviceName(Map<String, Object> params);

    /** 7) 기기상태마스터 최종 출동요청 정보 갱신 (T_ATM_STATUS). */
    int updateAtmStatus(Map<String, Object> params);
}
