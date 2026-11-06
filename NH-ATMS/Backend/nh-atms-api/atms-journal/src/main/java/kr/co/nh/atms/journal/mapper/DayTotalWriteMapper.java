package kr.co.nh.atms.journal.mapper;

import kr.co.nh.atms.journal.entity.DayTotal;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DayTotalWriteMapper {
    int insert(DayTotal t);
}
