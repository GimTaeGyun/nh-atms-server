package kr.co.nh.atms.common.security.crypto;

import java.util.LinkedHashMap;
import java.util.Map;

/** 화면에 내려줄 공개키 정보 (GET /api/auth/public-key 응답 data). */
public class FieldPublicKeyProvider {

    private final FieldCryptoProperties props;
    private final FieldDecryptor decryptor;

    FieldPublicKeyProvider(FieldCryptoProperties props, FieldDecryptor decryptor) {
        this.props = props;
        this.decryptor = decryptor;
    }

    /** enabled=false 면 {enabled:false} 만 — 화면은 평문으로 보낸다. serverTime 은 화면 시계 보정용(초). */
    public Map<String, Object> describe() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("enabled", decryptor.isEnabled());
        m.put("serverTime", System.currentTimeMillis() / 1000);
        if (decryptor.isEnabled()) {
            m.put("kid", props.getKid());
            m.put("alg", "RSA-OAEP-256/A256GCM");
            m.put("publicKey", decryptor.publicKeyPem());
        }
        return m;
    }
}
