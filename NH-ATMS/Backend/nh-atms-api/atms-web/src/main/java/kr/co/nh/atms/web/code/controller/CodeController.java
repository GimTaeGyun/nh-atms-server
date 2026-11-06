package kr.co.nh.atms.web.code.controller;

import kr.co.nh.atms.common.page.PageResult;
import kr.co.nh.atms.common.response.ApiResponse;
import kr.co.nh.atms.web.code.dto.CodeGroupDto;
import kr.co.nh.atms.web.code.dto.CodeItemDto;
import kr.co.nh.atms.web.code.dto.CodeSearchDto;
import kr.co.nh.atms.web.code.entity.Code;
import kr.co.nh.atms.web.code.entity.CodeGroup;
import kr.co.nh.atms.web.code.service.CodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/code")
@RequiredArgsConstructor
public class CodeController {
    private final CodeService service;

    // ── CodeGroup (T_CODE_GROUP) ──────────────────────────────────────────────

    @GetMapping("/groups")
    public ApiResponse<List<CodeGroupDto>> groupList() {
        return ApiResponse.ok(service.getAllGroups());
    }

    @GetMapping("/groups/{langCd}/{groupCd}")
    public ApiResponse<CodeGroupDto> groupGet(@PathVariable String langCd, @PathVariable String groupCd) {
        return ApiResponse.ok(service.getGroup(langCd, groupCd));
    }

    @PostMapping("/groups")
    public ApiResponse<Void> groupCreate(@RequestBody CodeGroup g) {
        service.createGroup(g);
        return ApiResponse.ok(null, "코드 그룹 등록 성공");
    }

    @PutMapping("/groups/{langCd}/{groupCd}")
    public ApiResponse<Void> groupUpdate(@PathVariable String langCd, @PathVariable String groupCd,
                                         @RequestBody CodeGroup g) {
        g.setLangCd(langCd); g.setGroupCd(groupCd);
        service.updateGroup(g);
        return ApiResponse.ok(null, "코드 그룹 수정 성공");
    }

    @DeleteMapping("/groups/{langCd}/{groupCd}")
    public ApiResponse<Void> groupDelete(@PathVariable String langCd, @PathVariable String groupCd) {
        service.deleteGroup(langCd, groupCd);
        return ApiResponse.ok(null, "코드 그룹 삭제 성공");
    }

    // ── Code (T_CODE) ────────────────────────────────────────────────────────

    @GetMapping
    public ApiResponse<PageResult<CodeItemDto>> list(@ModelAttribute CodeSearchDto s) {
        return ApiResponse.ok(service.search(s));
    }

    @GetMapping("/{langCd}/{groupCd}")
    public ApiResponse<List<CodeItemDto>> listByGroup(@PathVariable String langCd, @PathVariable String groupCd) {
        return ApiResponse.ok(service.getByGroup(langCd, groupCd));
    }

    @GetMapping("/{langCd}/{groupCd}/{code}")
    public ApiResponse<CodeItemDto> get(@PathVariable String langCd, @PathVariable String groupCd, @PathVariable String code) {
        return ApiResponse.ok(service.get(langCd, groupCd, code));
    }

    @PostMapping
    public ApiResponse<Void> create(@RequestBody Code c) {
        service.create(c);
        return ApiResponse.ok(null, "공통코드 등록 성공");
    }

    @PutMapping("/{langCd}/{groupCd}/{code}")
    public ApiResponse<Void> update(@PathVariable String langCd, @PathVariable String groupCd,
                                    @PathVariable String code, @RequestBody Code c) {
        c.setLangCd(langCd); c.setGroupCd(groupCd); c.setCode(code);
        service.update(c);
        return ApiResponse.ok(null, "공통코드 수정 성공");
    }

    @DeleteMapping("/{langCd}/{groupCd}/{code}")
    public ApiResponse<Void> delete(@PathVariable String langCd, @PathVariable String groupCd, @PathVariable String code) {
        service.delete(langCd, groupCd, code);
        return ApiResponse.ok(null, "공통코드 삭제 성공");
    }
}
