package kr.co.nh.atms.common.security.crypto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.nh.atms.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.spec.MGF1ParameterSpec;
import java.util.Base64;

/**
 * 화면이 보낸 민감필드 복호화.
 *
 * 형식: ENC1.&lt;kid&gt;.&lt;b64 RSA-OAEP(AES키)&gt;.&lt;b64 IV(12)&gt;.&lt;b64 AES-256-GCM 암호문+태그&gt;
 * 평문: {"v":"&lt;값&gt;","ts":&lt;epoch초&gt;,"n":"&lt;nonce&gt;"}
 *
 * 실패는 BusinessException(CRYPTO_*) → GlobalExceptionHandler 가 400 으로 응답. 값·암호문은 로그에 남기지 않는다.
 */
@Slf4j
public class FieldDecryptor {

    public static final String PREFIX = "ENC1.";

    /** OAEP 파라미터를 명시한다 — 생략하면 Java 기본 MGF1 이 SHA-1 이라 화면(node-forge, MGF1 SHA-256)과 맞지 않는다. */
    static final OAEPParameterSpec OAEP_SHA256 =
            new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);

    private static volatile FieldDecryptor current;

    private final FieldCryptoProperties props;
    private final FieldKeyHolder keys;          // enabled=false 이면 null
    private final ReplayGuard replayGuard;
    private final ObjectMapper json = new ObjectMapper();

    public FieldDecryptor(FieldCryptoProperties props, FieldKeyHolder keys) {
        this.props = props;
        this.keys = keys;
        this.replayGuard = new ReplayGuard(props.getMaxSkewSeconds());
        current = this;
    }

    /** Jackson 역직렬화기(@EncryptedField)가 쓰는 현재 앱의 인스턴스 (WAR 마다 클래스로더가 달라 앱별로 따로 존재). */
    public static FieldDecryptor current() {
        return current;
    }

    String publicKeyPem() {
        return keys == null ? null : keys.publicKeyPem();
    }

    public boolean isEnabled() {
        return props.isEnabled() && keys != null;
    }

    /** 암호문이면 복호화, 평문이면 required 설정에 따라 통과 또는 거부. null·빈 값은 그대로. */
    public String decrypt(String value) {
        if (value == null || value.isEmpty() || !isEnabled()) {
            return value;
        }
        if (!value.startsWith(PREFIX)) {
            if (props.isRequired()) {
                throw new BusinessException("CRYPTO_REQUIRED", "암호화되지 않은 값은 받을 수 없습니다.");
            }
            return value;
        }
        String[] p = value.split("\\.");
        if (p.length != 5) {
            throw new BusinessException("CRYPTO_FORMAT", "암호화 값 형식이 올바르지 않습니다.");
        }
        if (!props.getKid().equals(p[1])) {
            throw new BusinessException("CRYPTO_KID", "암호화 키가 바뀌었습니다. 다시 시도하세요.");
        }
        byte[] plain;
        try {
            Base64.Decoder b64 = Base64.getDecoder();
            Cipher rsa = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            rsa.init(Cipher.DECRYPT_MODE, keys.privateKey(), OAEP_SHA256);
            byte[] aesKey = rsa.doFinal(b64.decode(p[2]));
            Cipher aes = Cipher.getInstance("AES/GCM/NoPadding");
            aes.init(Cipher.DECRYPT_MODE, new SecretKeySpec(aesKey, "AES"), new GCMParameterSpec(128, b64.decode(p[3])));
            plain = aes.doFinal(b64.decode(p[4]));
        } catch (Exception e) {
            log.warn("[field-crypto] 복호화 실패: {}", e.getClass().getSimpleName());
            throw new BusinessException("CRYPTO_DECRYPT", "암호화 값을 해독할 수 없습니다.");
        }
        JsonNode node;
        try {
            node = json.readTree(new String(plain, StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new BusinessException("CRYPTO_FORMAT", "암호화 값 형식이 올바르지 않습니다.");
        }
        if (!node.hasNonNull("v") || !node.hasNonNull("ts") || !node.hasNonNull("n")) {
            throw new BusinessException("CRYPTO_FORMAT", "암호화 값 형식이 올바르지 않습니다.");
        }
        long nowMillis = System.currentTimeMillis();
        long skew = Math.abs(nowMillis / 1000 - node.get("ts").asLong());
        if (skew > props.getMaxSkewSeconds()) {
            throw new BusinessException("CRYPTO_EXPIRED", "요청 시각이 맞지 않습니다. 다시 시도하세요.");
        }
        if (!replayGuard.firstUse(node.get("n").asText(), nowMillis)) {
            throw new BusinessException("CRYPTO_REPLAY", "이미 사용된 요청입니다.");
        }
        return node.get("v").asText();
    }
}
