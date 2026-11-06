package kr.co.nh.atms.web.cash.mapper;

import kr.co.nh.atms.web.cash.dto.CashSearchDto;
import kr.co.nh.atms.web.cash.entity.CashPosition;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CashMapper {
    List<CashPosition> selectList(CashSearchDto search);
    long countList(CashSearchDto search);
}
