package kr.co.nh.atms.web.menu.controller;

import kr.co.nh.atms.common.response.ApiResponse;
import kr.co.nh.atms.web.menu.dto.MenuTreeNode;
import kr.co.nh.atms.web.menu.entity.Menu;
import kr.co.nh.atms.web.menu.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/menu")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    /** 로그인 사용자의 권한 기반 메뉴 트리 */
    @GetMapping("/my")
    public ApiResponse<List<MenuTreeNode>> myMenus() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ApiResponse.ok(menuService.getMyMenuTree(userId));
    }

    /** 전체 메뉴 트리 (관리자 화면용) */
    @GetMapping("/all")
    public ApiResponse<List<MenuTreeNode>> allMenus() {
        return ApiResponse.ok(menuService.getAllMenuTree());
    }

    @GetMapping("/{menuNo}")
    public ApiResponse<Menu> getOne(@PathVariable Long menuNo) {
        return ApiResponse.ok(menuService.getOne(menuNo));
    }

    @PostMapping
    public ApiResponse<Menu> create(@RequestBody Menu menu) {
        return ApiResponse.ok(menuService.create(menu));
    }

    @PutMapping("/{menuNo}")
    public ApiResponse<Void> update(@PathVariable Long menuNo, @RequestBody Menu menu) {
        menu.setMenuNo(menuNo);
        menuService.update(menu);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{menuNo}")
    public ApiResponse<Void> delete(@PathVariable Long menuNo) {
        menuService.delete(menuNo);
        return ApiResponse.ok(null);
    }
}

