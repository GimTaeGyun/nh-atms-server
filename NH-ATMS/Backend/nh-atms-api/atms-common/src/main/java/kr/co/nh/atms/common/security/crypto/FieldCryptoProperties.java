package kr.co.nh.atms.common.security.crypto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 화면↔서버 민감필드 암호화 설정 (crypto.field.*). 설계: docs/FIELD_ENCRYPTION_DESIGN.md
 *
 * 기본값 enabled=false → 설정하지 않은 모듈·환경은 영향 없음 (평문 그대로 통과).
 * 1차 적용은 atms-web 의 application-jeus.yml(서버 키 파일) / application-local.yml(임시 키 자동 생성).
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "crypto.field")
public class FieldCryptoProperties {

    /** 암호화 사용 여부. false 면 ENC1 값도 해석하지 않고 그대로 둔다. */
    private boolean enabled = false;

    /** true 면 평문(ENC1 로 시작하지 않는 값)을 거부한다. 화면 배포 확인 전까지 false. */
    private boolean required = false;

    /** 키 버전. 화면이 보낸 kid 와 다르면 거부 (키 교체 시 변경). */
    private String kid = "dev";

    /** RSA 개인키 PEM (PKCS#8, openssl genpkey 결과). */
    private String privateKeyPath;

    /** RSA 공개키 PEM (X.509 SubjectPublicKeyInfo, openssl pkey -pubout 결과). */
    private String publicKeyPath;

    /** 키 파일이 없으면 메모리에 임시 키를 만든다 — 로컬 PC 전용. 서버(jeus)에서는 켜지 않는다. */
    private boolean generateIfMissing = false;

    /** 화면 시각(ts)과 서버 시각의 허용 차이(초). 재전송 방지 nonce 보관 시간도 이 값. */
    private long maxSkewSeconds = 120;
}
