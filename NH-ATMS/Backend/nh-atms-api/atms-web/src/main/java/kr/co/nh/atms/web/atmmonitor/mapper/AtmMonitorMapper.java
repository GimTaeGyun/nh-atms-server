package kr.co.nh.atms.web.atmmonitor.mapper;

import kr.co.nh.atms.web.atmmonitor.dto.AtmMonitorSearchDto;
import kr.co.nh.atms.web.atmmonitor.entity.AtmMonitor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AtmMonitorMapper {
    List<AtmMonitor> selectList(AtmMonitorSearchDto search);
    long countList(AtmMonitorSearchDto search);
    AtmMonitor selectOne(@Param("termGroupId") String termGroupId,
                         @Param("termId") String termId,
                         @Param("jnlDate") String jnlDate,
                         @Param("jnlTime") String jnlTime);
    int insert(AtmMonitor m);
}
