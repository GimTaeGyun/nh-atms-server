package kr.co.nh.atms.web.serviceprovider.mapper;

import kr.co.nh.atms.web.serviceprovider.dto.ServiceProviderSearchDto;
import kr.co.nh.atms.web.serviceprovider.entity.ServiceProvider;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ServiceProviderMapper {
    List<ServiceProvider> selectList(ServiceProviderSearchDto search);
    long countList(ServiceProviderSearchDto search);
    ServiceProvider selectOne(@Param("providerCd") String providerCd);
    int insert(ServiceProvider sp);
    int update(ServiceProvider sp);
    int delete(@Param("providerCd") String providerCd);
}
