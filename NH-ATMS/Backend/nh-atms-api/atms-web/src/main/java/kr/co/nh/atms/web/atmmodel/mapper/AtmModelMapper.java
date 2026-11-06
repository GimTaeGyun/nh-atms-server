package kr.co.nh.atms.web.atmmodel.mapper;

import kr.co.nh.atms.web.atmmodel.dto.AtmModelSearchDto;
import kr.co.nh.atms.web.atmmodel.entity.AtmModel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AtmModelMapper {
    List<AtmModel> selectList(AtmModelSearchDto search);
    long countList(AtmModelSearchDto search);
    AtmModel selectOne(@Param("modelCd") String modelCd);
    int insert(AtmModel model);
    int update(AtmModel model);
    int delete(@Param("modelCd") String modelCd);
}
