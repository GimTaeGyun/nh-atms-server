package kr.co.nh.atms.web.loginhist.controller;

import kr.co.nh.atms.common.page.PageResult;
import kr.co.nh.atms.common.response.ApiResponse;
import kr.co.nh.atms.web.loginhist.dto.LoginHistSearchDto;
import kr.co.nh.atms.web.loginhist.entity.LoginHist;
import kr.co.nh.atms.web.loginhist.service.LoginHistService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/login-hist")
@RequiredArgsConstructor
public class LoginHistController {

    private final LoginHistService service;

    @GetMapping
    public ApiResponse<PageResult<LoginHist>> list(@ModelAttribute LoginHistSearchDto search) {
        return ApiResponse.ok(service.getList(search));
    }
}
