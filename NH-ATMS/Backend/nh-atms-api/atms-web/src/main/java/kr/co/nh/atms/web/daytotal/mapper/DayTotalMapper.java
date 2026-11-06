package kr.co.nh.atms.web.daytotal.mapper;

import kr.co.nh.atms.web.daytotal.dto.DayTotalSearchDto;
import kr.co.nh.atms.web.daytotal.entity.DayTotalRow;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DayTotalMapper {
    List<DayTotalRow> selectList(DayTotalSearchDto search);
    long countList(DayTotalSearchDto search);
}
