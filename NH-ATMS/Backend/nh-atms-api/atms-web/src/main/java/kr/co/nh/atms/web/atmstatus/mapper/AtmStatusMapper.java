package kr.co.nh.atms.web.atmstatus.mapper;

import kr.co.nh.atms.web.atmstatus.dto.AtmStatusSearchDto;
import kr.co.nh.atms.web.atmstatus.entity.AtmStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AtmStatusMapper {
    List<AtmStatus> selectList(AtmStatusSearchDto search);
    long countList(AtmStatusSearchDto search);
    AtmStatus selectOne(@Param("termGroupId") String termGroupId,
                        @Param("termId") String termId);
    int upsert(AtmStatus s);
}
