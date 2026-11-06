package kr.co.nh.atms.web.corner.controller;

import kr.co.nh.atms.common.page.PageResult;
import kr.co.nh.atms.common.response.ApiResponse;
import kr.co.nh.atms.web.corner.dto.CornerSearchDto;
import kr.co.nh.atms.web.corner.entity.Corner;
import kr.co.nh.atms.web.corner.service.CornerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/corner")
@RequiredArgsConstructor
public class CornerController {
    private final CornerService service;

    @GetMapping
    public ApiResponse<PageResult<Corner>> list(@ModelAttribute CornerSearchDto search) {
        return ApiResponse.ok(service.search(search));
    }

    @GetMapping("/{termGroupId}/{cornerCd}")
    public ApiResponse<Corner> get(@PathVariable String termGroupId,
                                   @PathVariable String cornerCd) {
        return ApiResponse.ok(service.get(termGroupId, cornerCd));
    }
}
