package kr.co.nh.atms.web.auth.controller;

import kr.co.nh.atms.common.response.ApiResponse;
import kr.co.nh.atms.common.security.crypto.FieldPublicKeyProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 민감필드 암호화용 공개키 (로그인 전 호출 — SecurityConfig permitAll).
 * 응답: {enabled, serverTime, kid, alg, publicKey(PEM)}. 설계: docs/FIELD_ENCRYPTION_DESIGN.md
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class FieldCryptoController {

    private final FieldPublicKeyProvider publicKeyProvider;

    @GetMapping("/public-key")
    public ApiResponse<Map<String, Object>> publicKey() {
        return ApiResponse.ok(publicKeyProvider.describe());
    }
}
