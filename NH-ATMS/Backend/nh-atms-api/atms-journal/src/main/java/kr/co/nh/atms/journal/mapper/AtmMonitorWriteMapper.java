package kr.co.nh.atms.journal.mapper;

import kr.co.nh.atms.journal.entity.AtmMonitor;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AtmMonitorWriteMapper {
    int insert(AtmMonitor m);
}
