package kr.co.nh.atms.web.atm.controller;

import jakarta.validation.Valid;
import kr.co.nh.atms.common.page.PageResult;
import kr.co.nh.atms.common.response.ApiResponse;
import kr.co.nh.atms.web.atm.dto.AtmSearchDto;
import kr.co.nh.atms.web.atm.entity.Atm;
import kr.co.nh.atms.web.atm.service.AtmService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/atm")
@RequiredArgsConstructor
public class AtmController {

    private final AtmService atmService;

    @GetMapping
    public ApiResponse<PageResult<Atm>> list(@ModelAttribute AtmSearchDto search) {
        return ApiResponse.ok(atmService.search(search));
    }

    @GetMapping("/{termGroupId}/{termId}")
    public ApiResponse<Atm> get(@PathVariable String termGroupId,
                                 @PathVariable String termId) {
        return ApiResponse.ok(atmService.get(termGroupId, termId));
    }

    @PostMapping
    public ApiResponse<Void> create(@Valid @RequestBody Atm atm) {
        atmService.create(atm);
        return ApiResponse.ok(null, "등록 성공");
    }

    @PutMapping("/{termGroupId}/{termId}")
    public ApiResponse<Void> update(@PathVariable String termGroupId,
                                     @PathVariable String termId,
                                     @Valid @RequestBody Atm atm) {
        atm.setTermGroupId(termGroupId);
        atm.setTermId(termId);
        atmService.update(atm);
        return ApiResponse.ok(null, "수정 성공");
    }

    @DeleteMapping("/{termGroupId}/{termId}")
    public ApiResponse<Void> delete(@PathVariable String termGroupId,
                                     @PathVariable String termId) {
        atmService.delete(termGroupId, termId);
        return ApiResponse.ok(null, "삭제 성공");
    }
}
