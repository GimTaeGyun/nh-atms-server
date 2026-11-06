package kr.co.nh.atms.web.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import kr.co.nh.atms.common.response.ApiResponse;
import kr.co.nh.atms.web.auth.dto.LoginRequest;
import kr.co.nh.atms.web.auth.dto.LoginResponse;
import kr.co.nh.atms.web.auth.dto.RefreshRequest;
import kr.co.nh.atms.web.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request,
                                                            HttpServletRequest httpRequest) {
        request.setClientIp(httpRequest.getRemoteAddr());
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok(response, "로그인 성공"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
        LoginResponse response = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.ok(response, "토큰 갱신 성공"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        authService.logout(userId);
        return ResponseEntity.ok(ApiResponse.ok(null, "로그아웃 성공"));
    }
}
