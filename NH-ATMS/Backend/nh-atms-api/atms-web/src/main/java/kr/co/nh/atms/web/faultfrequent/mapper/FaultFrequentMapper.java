package kr.co.nh.atms.web.faultfrequent.mapper;

import kr.co.nh.atms.web.faultfrequent.dto.FaultFrequent;
import kr.co.nh.atms.web.faultfrequent.dto.FaultFrequentSearchDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface FaultFrequentMapper {
    List<FaultFrequent> selectList(FaultFrequentSearchDto search);
    long countList(FaultFrequentSearchDto search);
}
