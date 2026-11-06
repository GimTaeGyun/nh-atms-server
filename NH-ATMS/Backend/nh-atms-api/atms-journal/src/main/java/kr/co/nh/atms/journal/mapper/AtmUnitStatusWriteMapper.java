package kr.co.nh.atms.journal.mapper;

import kr.co.nh.atms.journal.entity.AtmUnitStatus;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AtmUnitStatusWriteMapper {
    /** 디바이스 단위 상태 upsert (Oracle MERGE). */
    int upsert(AtmUnitStatus s);
}
