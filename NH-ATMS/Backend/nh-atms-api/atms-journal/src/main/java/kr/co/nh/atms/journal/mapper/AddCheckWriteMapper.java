package kr.co.nh.atms.journal.mapper;

import kr.co.nh.atms.journal.entity.AddCheck;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AddCheckWriteMapper {
    int insert(AddCheck t);
}
