package kr.co.nh.atms.journal.mapper;

import kr.co.nh.atms.journal.entity.CashTransaction;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CashTransactionWriteMapper {
    /** 71 거래 권종별 현금 상세 적재 (PK 중복 무시). */
    int insert(CashTransaction t);
}
