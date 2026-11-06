package kr.co.nh.atms.journal.mapper;

import kr.co.nh.atms.journal.entity.FaultMap;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * T_FAULT_MAP(장애 분류 맵) 조회 매퍼.
 * 21/22 장애저널 처리 시 device 상태 바이트를 장애 분류로 변환.
 */
@Mapper
public interface FaultMapMapper {

    /** 장치/모듈/상태코드로 장애 분류 1건 조회(없으면 null). */
    FaultMap select(@Param("langCd") String langCd,
                    @Param("deviceId") String deviceId,
                    @Param("moduleTypeCd") String moduleTypeCd,
                    @Param("statusCd") String statusCd);
}
