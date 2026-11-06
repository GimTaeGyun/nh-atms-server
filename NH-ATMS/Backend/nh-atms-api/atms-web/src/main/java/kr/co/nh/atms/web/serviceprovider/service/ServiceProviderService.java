package kr.co.nh.atms.web.serviceprovider.service;

import kr.co.nh.atms.common.exception.BusinessException;
import kr.co.nh.atms.common.page.PageResult;
import kr.co.nh.atms.web.serviceprovider.dto.ServiceProviderSearchDto;
import kr.co.nh.atms.web.serviceprovider.entity.ServiceProvider;
import kr.co.nh.atms.web.serviceprovider.mapper.ServiceProviderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServiceProviderService {

    private final ServiceProviderMapper mapper;

    public PageResult<ServiceProvider> search(ServiceProviderSearchDto search) {
        return PageResult.of(mapper.selectList(search), mapper.countList(search), search);
    }

    public ServiceProvider get(String providerCd) {
        ServiceProvider sp = mapper.selectOne(providerCd);
        if (sp == null) throw new BusinessException("SP_001", "업체 정보를 찾을 수 없습니다.");
        return sp;
    }

    @Transactional
    public void create(ServiceProvider sp) {
        if (mapper.selectOne(sp.getProviderCd()) != null)
            throw new BusinessException("SP_002", "이미 등록된 업체코드입니다.");
        mapper.insert(sp);
    }

    @Transactional
    public void update(ServiceProvider sp) {
        if (mapper.update(sp) == 0)
            throw new BusinessException("SP_001", "업체 정보를 찾을 수 없습니다.");
    }

    @Transactional
    public void delete(String providerCd) {
        if (mapper.delete(providerCd) == 0)
            throw new BusinessException("SP_001", "업체 정보를 찾을 수 없습니다.");
    }
}
