package kr.co.nh.atms.web.user.controller;

import kr.co.nh.atms.common.page.PageResult;
import kr.co.nh.atms.common.response.ApiResponse;
import kr.co.nh.atms.common.security.crypto.FieldDecryptor;
import kr.co.nh.atms.web.user.dto.UserSearchDto;
import kr.co.nh.atms.web.user.entity.User;
import kr.co.nh.atms.web.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService service;
    private final FieldDecryptor fieldDecryptor;

    @GetMapping
    public ApiResponse<PageResult<User>> list(@ModelAttribute UserSearchDto s) {
        return ApiResponse.ok(service.search(s));
    }
    @GetMapping("/{userId}")
    public ApiResponse<User> get(@PathVariable String userId) {
        return ApiResponse.ok(service.get(userId));
    }
    @PostMapping
    public ApiResponse<Void> create(@RequestBody User u) {
        service.create(u);
        return ApiResponse.ok(null, "사용자 등록 성공");
    }
    @PutMapping("/{userId}")
    public ApiResponse<Void> update(@PathVariable String userId, @RequestBody User u) {
        u.setUserId(userId);
        service.update(u);
        return ApiResponse.ok(null, "사용자 수정 성공");
    }
    @DeleteMapping("/{userId}")
    public ApiResponse<Void> delete(@PathVariable String userId) {
        service.delete(userId);
        return ApiResponse.ok(null, "사용자 삭제 성공");
    }
    @PutMapping("/{userId}/password")
    public ApiResponse<Void> changePassword(@PathVariable String userId, @RequestBody Map<String, String> body) {
        // 화면이 ENC1 암호문으로 보냄 → 평문으로 (Map 바디라 직접 복호화)
        service.changePassword(userId, fieldDecryptor.decrypt(body.get("password")));
        return ApiResponse.ok(null, "비밀번호 변경 성공");
    }

    /** 계정 잠금 해제 (LOCK_YN='N', LOGIN_FAIL_CNT=0) */
    @PutMapping("/{userId}/unlock")
    public ApiResponse<Void> unlock(@PathVariable String userId) {
        service.unlock(userId);
        return ApiResponse.ok(null, "계정 잠금 해제 성공");
    }

    /** 사용여부 토글 (USE_YN 변경) */
    @PutMapping("/{userId}/use-yn")
    public ApiResponse<Void> toggleUseYn(@PathVariable String userId, @RequestBody Map<String, String> body) {
        service.updateUseYn(userId, body.get("useYn"));
        return ApiResponse.ok(null, "사용여부 변경 성공");
    }
}
