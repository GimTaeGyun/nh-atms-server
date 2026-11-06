package kr.co.nh.atms.web.loginhist.service;

import kr.co.nh.atms.common.page.PageResult;
import kr.co.nh.atms.web.loginhist.dto.LoginHistSearchDto;
import kr.co.nh.atms.web.loginhist.entity.LoginHist;
import kr.co.nh.atms.web.loginhist.mapper.LoginHistQueryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoginHistService {

    private final LoginHistQueryMapper mapper;

    public PageResult<LoginHist> getList(LoginHistSearchDto search) {
        return PageResult.of(mapper.selectList(search), mapper.countList(search), search);
    }
}
