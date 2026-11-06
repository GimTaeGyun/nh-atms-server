package kr.co.nh.atms.web.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import kr.co.nh.atms.common.security.crypto.EncryptedField;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "아이디를 입력하세요")
    private String userId;

    @NotBlank(message = "비밀번호를 입력하세요")
    @EncryptedField   // 화면이 ENC1 암호문으로 보냄 → 여기서 평문으로 (docs/FIELD_ENCRYPTION_DESIGN.md)
    private String password;

    @JsonIgnore
    private String clientIp;
}
