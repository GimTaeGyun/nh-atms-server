package kr.co.nh.atms.web.atmgroup.controller;

import jakarta.validation.Valid;
import kr.co.nh.atms.common.page.PageResult;
import kr.co.nh.atms.common.response.ApiResponse;
import kr.co.nh.atms.web.atmgroup.dto.AtmGroupSearchDto;
import kr.co.nh.atms.web.atmgroup.entity.AtmGroup;
import kr.co.nh.atms.web.atmgroup.service.AtmGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/atm-group")
@RequiredArgsConstructor
public class AtmGroupController {
    private final AtmGroupService service;

    @GetMapping
    public ApiResponse<PageResult<AtmGroup>> list(@ModelAttribute AtmGroupSearchDto s) {
        return ApiResponse.ok(service.search(s));
    }
    @GetMapping("/{termGroupId}")
    public ApiResponse<AtmGroup> get(@PathVariable String termGroupId) {
        return ApiResponse.ok(service.get(termGroupId));
    }
    @PostMapping
    public ApiResponse<Void> create(@Valid @RequestBody AtmGroup g) {
        service.create(g); return ApiResponse.ok(null, "등록 성공");
    }
    @PutMapping("/{termGroupId}")
    public ApiResponse<Void> update(@PathVariable String termGroupId, @Valid @RequestBody AtmGroup g) {
        g.setTermGroupId(termGroupId); service.update(g);
        return ApiResponse.ok(null, "수정 성공");
    }
    @DeleteMapping("/{termGroupId}")
    public ApiResponse<Void> delete(@PathVariable String termGroupId) {
        service.delete(termGroupId); return ApiResponse.ok(null, "삭제 성공");
    }
}
