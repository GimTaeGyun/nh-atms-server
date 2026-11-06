package kr.co.nh.atms.web.faultmgmt.mapper;

import kr.co.nh.atms.web.faultmgmt.dto.FaultManagementSearchDto;
import kr.co.nh.atms.web.faultmgmt.entity.FaultManagement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FaultManagementMapper {
    List<FaultManagement> selectList(FaultManagementSearchDto search);
    long countList(FaultManagementSearchDto search);
    FaultManagement selectOne(@Param("faultSeq") Long faultSeq);
    int insert(FaultManagement fault);
    int cancel(@Param("faultSeq") Long faultSeq);
}
