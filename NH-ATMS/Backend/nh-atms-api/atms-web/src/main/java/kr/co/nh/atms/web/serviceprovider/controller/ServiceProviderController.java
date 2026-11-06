package kr.co.nh.atms.web.serviceprovider.controller;

import jakarta.validation.Valid;
import kr.co.nh.atms.common.page.PageResult;
import kr.co.nh.atms.common.response.ApiResponse;
import kr.co.nh.atms.web.serviceprovider.dto.ServiceProviderSearchDto;
import kr.co.nh.atms.web.serviceprovider.entity.ServiceProvider;
import kr.co.nh.atms.web.serviceprovider.service.ServiceProviderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/service-provider")
@RequiredArgsConstructor
public class ServiceProviderController {

    private final ServiceProviderService service;

    @GetMapping
    public ApiResponse<PageResult<ServiceProvider>> list(@ModelAttribute ServiceProviderSearchDto search) {
        return ApiResponse.ok(service.search(search));
    }

    @GetMapping("/{providerCd}")
    public ApiResponse<ServiceProvider> get(@PathVariable String providerCd) {
        return ApiResponse.ok(service.get(providerCd));
    }

    @PostMapping
    public ApiResponse<Void> create(@Valid @RequestBody ServiceProvider sp) {
        service.create(sp);
        return ApiResponse.ok(null, "등록 성공");
    }

    @PutMapping("/{providerCd}")
    public ApiResponse<Void> update(@PathVariable String providerCd,
                                    @Valid @RequestBody ServiceProvider sp) {
        sp.setProviderCd(providerCd);
        service.update(sp);
        return ApiResponse.ok(null, "수정 성공");
    }

    @DeleteMapping("/{providerCd}")
    public ApiResponse<Void> delete(@PathVariable String providerCd) {
        service.delete(providerCd);
        return ApiResponse.ok(null, "삭제 성공");
    }
}
