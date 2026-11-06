package kr.co.nh.atms.web.usergroup.controller;

import kr.co.nh.atms.common.page.PageResult;
import kr.co.nh.atms.common.response.ApiResponse;
import kr.co.nh.atms.web.usergroup.dto.UserGroupSearchDto;
import kr.co.nh.atms.web.usergroup.entity.UserGroup;
import kr.co.nh.atms.web.usergroup.service.UserGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/usergroup")
@RequiredArgsConstructor
public class UserGroupController {

    private final UserGroupService service;

    @GetMapping
    public ApiResponse<PageResult<UserGroup>> list(@ModelAttribute UserGroupSearchDto s) {
        return ApiResponse.ok(service.search(s));
    }

    /** 셀렉트박스용 전체 목록 */
    @GetMapping("/all")
    public ApiResponse<List<UserGroup>> all() {
        return ApiResponse.ok(service.getAll());
    }

    @GetMapping("/{userGroupNo}")
    public ApiResponse<UserGroup> get(@PathVariable Long userGroupNo) {
        return ApiResponse.ok(service.get(userGroupNo));
    }

    @PostMapping
    public ApiResponse<Void> create(@RequestBody UserGroup g) {
        service.create(g);
        return ApiResponse.ok(null, "사용자그룹 등록 성공");
    }

    @PutMapping("/{userGroupNo}")
    public ApiResponse<Void> update(@PathVariable Long userGroupNo, @RequestBody UserGroup g) {
        g.setUserGroupNo(userGroupNo);
        service.update(g);
        return ApiResponse.ok(null, "사용자그룹 수정 성공");
    }

    @DeleteMapping("/{userGroupNo}")
    public ApiResponse<Void> delete(@PathVariable Long userGroupNo) {
        service.delete(userGroupNo);
        return ApiResponse.ok(null, "사용자그룹 삭제 성공");
    }

    /** 그룹 권한 코드 조회 */
    @GetMapping("/{userGroupNo}/auth")
    public ApiResponse<List<String>> getAuthCodes(@PathVariable Long userGroupNo) {
        return ApiResponse.ok(service.get(userGroupNo).getAuthCodes());
    }

    /** 그룹 권한 코드 교체 (body: {"authCodes":["ROLE_ADMIN","ROLE_OPER"]}) */
    @PutMapping("/{userGroupNo}/auth")
    public ApiResponse<Void> updateAuthCodes(@PathVariable Long userGroupNo,
                                             @RequestBody Map<String, List<String>> body) {
        service.updateAuthCodes(userGroupNo, body.get("authCodes"));
        return ApiResponse.ok(null, "권한 설정 완료");
    }
}
