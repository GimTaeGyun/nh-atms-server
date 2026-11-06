package kr.co.nh.atms.ifex.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.Map;

/**
 * 개국/장애/상태통보(EM_01001110) 전문 바디 enrichment 전용 조회 매퍼.
 * 레거시 atme ExternalInterfaceMapper(selectAtmMonitorAndAtmDeviceStatus /
 * selectCassetteStatus / selectFaultAnalysis)를 신규 스키마
 * (T_ATM / T_ATM_GROUP / T_CORNER / T_ATM_MONITOR / T_ATM_DEVICE_STATUS /
 *  T_CASSETTE_STATUS / T_FAULT_ANALYSIS)에 맞춰 이식.
 */
@Mapper
public interface StatusEnrichMapper {

    /**
     * 기기 상태/디바이스 기본정보 조회 (전문 01~16,18 및 22~31, 35~43 원천).
     * 키: termGroupId / termId / jnlDate / jnlTime.
     */
    Map<String, Object> selectAtmStatusInfo(Map<String, Object> params);

    /**
     * 카세트 잔여매수 조회 (전문 44~46). 만원/오만원/수표 5자리 우측정렬.
     * 키: termGroupId / termId.
     */
    Map<String, Object> selectCassetteRemain(Map<String, Object> params);

    /**
     * 대표 장애이력 1건 조회 (전문 17/19/20/21/33 원천). 장애분류코드 오름차순 첫 건.
     * 키: termGroupId / termId / jnlDate / jnlTime.
     */
    Map<String, Object> selectFaultAnalysis(Map<String, Object> params);
}
