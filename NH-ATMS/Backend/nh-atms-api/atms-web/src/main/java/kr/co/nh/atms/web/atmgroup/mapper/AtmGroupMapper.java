package kr.co.nh.atms.web.atmgroup.mapper;

import kr.co.nh.atms.web.atmgroup.dto.AtmGroupSearchDto;
import kr.co.nh.atms.web.atmgroup.entity.AtmGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AtmGroupMapper {
    List<AtmGroup> selectList(AtmGroupSearchDto search);
    long countList(AtmGroupSearchDto search);
    AtmGroup selectOne(@Param("termGroupId") String termGroupId);
    int insert(AtmGroup g);
    int update(AtmGroup g);
    int delete(@Param("termGroupId") String termGroupId);
}
