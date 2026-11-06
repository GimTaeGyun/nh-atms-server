package kr.co.nh.atms.web.corner.mapper;

import kr.co.nh.atms.web.corner.dto.CornerSearchDto;
import kr.co.nh.atms.web.corner.entity.Corner;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CornerMapper {
    List<Corner> selectList(CornerSearchDto search);
    long countList(CornerSearchDto search);
    Corner selectOne(@Param("termGroupId") String termGroupId,
                     @Param("cornerCd") String cornerCd);
}
