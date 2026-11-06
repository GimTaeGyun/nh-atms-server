package kr.co.nh.atms.web.atm.mapper;

import kr.co.nh.atms.web.atm.dto.AtmSearchDto;
import kr.co.nh.atms.web.atm.entity.Atm;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AtmMapper {

    List<Atm> selectList(AtmSearchDto search);

    long countList(AtmSearchDto search);

    Atm selectOne(@Param("termGroupId") String termGroupId,
                  @Param("termId") String termId);

    int insert(Atm atm);

    int update(Atm atm);

    int delete(@Param("termGroupId") String termGroupId,
               @Param("termId") String termId);
}
