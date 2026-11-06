package kr.co.nh.atms.journal.mapper;

import kr.co.nh.atms.journal.entity.AtmDeviceStatus;
import org.apache.ibatis.annotations.Mapper;

/**
 * T_ATM_DEVICE_STATUS(기기 디바이스정보) 등록/갱신 매퍼.
 * 12(개국) 저널 수신 시 디바이스 장착/지원 정보를 upsert 한다.
 * (atmr RecvJournalMapper.upsert_T_ATMDEVICESTATUS 정합)
 */
@Mapper
public interface AtmDeviceStatusWriteMapper {

    /** 디바이스정보 upsert (PK termGroupId+termId 기준 MERGE). */
    int upsert(AtmDeviceStatus device);

    /**
     * 21/22 상태·장애 → 거래모드 상태정보(입출금 가능여부) 갱신.
     * (atmr update_T_ATMDEVICESTATUS) row 없으면 0건(개국 전 기기는 무시).
     */
    int updateTxMode(@org.apache.ibatis.annotations.Param("termGroupId") String termGroupId,
                     @org.apache.ibatis.annotations.Param("termId") String termId,
                     @org.apache.ibatis.annotations.Param("manOStatus") String manOStatus,
                     @org.apache.ibatis.annotations.Param("manIStatus") String manIStatus,
                     @org.apache.ibatis.annotations.Param("omanOStatus") String omanOStatus,
                     @org.apache.ibatis.annotations.Param("omanIStatus") String omanIStatus,
                     @org.apache.ibatis.annotations.Param("chkOStatus") String chkOStatus,
                     @org.apache.ibatis.annotations.Param("chkIStatus") String chkIStatus,
                     @org.apache.ibatis.annotations.Param("pbStatus") String pbStatus,
                     @org.apache.ibatis.annotations.Param("ochunStatus") String ochunStatus);
}
