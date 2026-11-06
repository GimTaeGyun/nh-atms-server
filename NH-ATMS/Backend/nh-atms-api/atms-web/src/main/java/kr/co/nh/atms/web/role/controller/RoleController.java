package kr.co.nh.atms.web.role.controller;

import kr.co.nh.atms.common.page.PageResult;
import kr.co.nh.atms.common.response.ApiResponse;
import kr.co.nh.atms.web.role.dto.MenuRolePermDetail;
import kr.co.nh.atms.web.role.dto.MenuRolePermSaveRequest;
import kr.co.nh.atms.web.role.dto.RoleSearchDto;
import kr.co.nh.atms.web.role.entity.Role;
import kr.co.nh.atms.web.role.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/role")
@RequiredArgsConstructor
public class RoleController {
    private final RoleService service;

    @GetMapping
    public ApiResponse<PageResult<Role>> list(@ModelAttribute RoleSearchDto s) {
        return ApiResponse.ok(service.search(s));
    }
    /** 셀렉트박스용 전체 목록 */
    @GetMapping("/all")
    public ApiResponse<java.util.List<Role>> all() {
        return ApiResponse.ok(service.getAll());
    }
    @GetMapping("/{authCode}")
    public ApiResponse<Role> get(@PathVariable String authCode) {
        return ApiResponse.ok(service.get(authCode));
    }
    @PostMapping
    public ApiResponse<Void> create(@RequestBody Role r) {
        service.create(r);
        return ApiResponse.ok(null, "권한 등록 성공");
    }
    @PutMapping("/{authCode}")
    public ApiResponse<Void> update(@PathVariable String authCode, @RequestBody Role r) {
        r.setAuthCode(authCode);
        service.update(r);
        return ApiResponse.ok(null, "권한 수정 성공");
    }
    @DeleteMapping("/{authCode}")
    public ApiResponse<Void> delete(@PathVariable String authCode) {
        service.delete(authCode);
        return ApiResponse.ok(null, "권한 삭제 성공");
    }

    /** 특정 메뉴 + 역할의 조회/수정 권한 조회 (접근권한 팝업 우측) */
    @GetMapping("/menu/{menuNo}/perm/{authCode}")
    public ApiResponse<MenuRolePermDetail> getMenuRolePerm(@PathVariable Long menuNo,
                                                           @PathVariable String authCode) {
        return ApiResponse.ok(service.getMenuRolePerm(menuNo, authCode));
    }

    /** 특정 메뉴 + 역할의 조회/수정 권한 저장 (접근권한 팝업 저장) */
    @PutMapping("/menu/{menuNo}/perm/{authCode}")
    public ApiResponse<Void> saveMenuRolePerm(@PathVariable Long menuNo,
                                              @PathVariable String authCode,
                                              @RequestBody MenuRolePermSaveRequest req) {
        service.saveMenuRolePerm(menuNo, authCode, req);
        return ApiResponse.ok(null, "권한 저장 성공");
    }
}
