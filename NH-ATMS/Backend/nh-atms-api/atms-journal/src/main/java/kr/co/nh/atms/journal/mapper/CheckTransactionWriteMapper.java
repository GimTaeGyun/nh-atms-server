package kr.co.nh.atms.journal.mapper;

import kr.co.nh.atms.journal.entity.CheckTransaction;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CheckTransactionWriteMapper {
    /** 71 거래 수표 상세 적재 (PK 중복 무시). */
    int insert(CheckTransaction t);
}
