package kr.co.nh.atms.web.branch.controller;

import jakarta.validation.Valid;
import kr.co.nh.atms.common.page.PageResult;
import kr.co.nh.atms.common.response.ApiResponse;
import kr.co.nh.atms.web.branch.dto.BranchSearchDto;
import kr.co.nh.atms.web.branch.entity.Branch;
import kr.co.nh.atms.web.branch.service.BranchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/branch")
@RequiredArgsConstructor
public class BranchController {
    private final BranchService service;

    @GetMapping
    public ApiResponse<PageResult<Branch>> list(@ModelAttribute BranchSearchDto search) {
        return ApiResponse.ok(service.search(search));
    }

    @GetMapping("/{termGroupId}/{branchCd}/{branchName}")
    public ApiResponse<Branch> get(@PathVariable String termGroupId,
                                   @PathVariable String branchCd,
                                   @PathVariable String branchName) {
        return ApiResponse.ok(service.get(termGroupId, branchCd, branchName));
    }

    @PostMapping
    public ApiResponse<Void> create(@Valid @RequestBody Branch b) {
        service.create(b);
        return ApiResponse.ok(null, "등록 성공");
    }

    @PutMapping("/{termGroupId}/{branchCd}/{branchName}")
    public ApiResponse<Void> update(@PathVariable String termGroupId,
                                    @PathVariable String branchCd,
                                    @PathVariable String branchName,
                                    @Valid @RequestBody Branch b) {
        b.setTermGroupId(termGroupId); b.setBranchCd(branchCd); b.setBranchName(branchName);
        service.update(b);
        return ApiResponse.ok(null, "수정 성공");
    }

    @DeleteMapping("/{termGroupId}/{branchCd}/{branchName}")
    public ApiResponse<Void> delete(@PathVariable String termGroupId,
                                    @PathVariable String branchCd,
                                    @PathVariable String branchName) {
        service.delete(termGroupId, branchCd, branchName);
        return ApiResponse.ok(null, "삭제 성공");
    }
}
