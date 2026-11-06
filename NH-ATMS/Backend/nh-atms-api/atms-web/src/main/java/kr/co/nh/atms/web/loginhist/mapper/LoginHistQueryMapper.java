package kr.co.nh.atms.web.loginhist.mapper;

import kr.co.nh.atms.web.loginhist.dto.LoginHistSearchDto;
import kr.co.nh.atms.web.loginhist.entity.LoginHist;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface LoginHistQueryMapper {
    List<LoginHist> selectList(LoginHistSearchDto search);
    long countList(LoginHistSearchDto search);
}
