package kr.co.nh.atms.common.security.crypto;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.nh.atms.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 민감필드 복호화 — 화면(node-forge) 암호문 교차 검증 + 변조·키·시각·재전송·평문 정책.
 * 테스트 키(test-only.*.pem)는 openssl 로 만든 시험 전용 키다 (서버 키 아님).
 */
class FieldDecryptorTest {

    private static final long HUNDRED_YEARS = 100L * 365 * 24 * 3600;

    private FieldCryptoProperties props;
    private FieldKeyHolder keys;

    private static Path res(String name) throws URISyntaxException {
        return Path.of(FieldDecryptorTest.class.getResource("/field-crypto/" + name).toURI());
    }

    @BeforeEach
    void setUp() throws Exception {
        props = new FieldCryptoProperties();
        props.setEnabled(true);
        props.setKid("test");
        props.setPrivateKeyPath(res("test-only.key.pem").toString());
        props.setPublicKeyPath(res("test-only.pub.pem").toString());
        keys = FieldKeyHolder.load(props);   // openssl PEM(PKCS#8 / X.509) 읽기 확인
    }

    /** Java 로 화면과 같은 형식의 암호문을 만든다 (시각·nonce·kid 를 바꿔가며 시험하려고). */
    private String encrypt(String value, long ts, String nonce, String kid) throws Exception {
        byte[] aesKey = new byte[32];
        byte[] iv = new byte[12];
        SecureRandom rnd = new SecureRandom();
        rnd.nextBytes(aesKey);
        rnd.nextBytes(iv);
        String plain = new ObjectMapper().writeValueAsString(java.util.Map.of("v", value, "ts", ts, "n", nonce));
        Cipher aes = Cipher.getInstance("AES/GCM/NoPadding");
        aes.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(aesKey, "AES"), new GCMParameterSpec(128, iv));
        byte[] ct = aes.doFinal(plain.getBytes(StandardCharsets.UTF_8));
        Cipher rsa = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        rsa.init(Cipher.ENCRYPT_MODE, keys.publicKey(), FieldDecryptor.OAEP_SHA256);
        Base64.Encoder b = Base64.getEncoder();
        return "ENC1." + kid + "." + b.encodeToString(rsa.doFinal(aesKey)) + "." + b.encodeToString(iv) + "." + b.encodeToString(ct);
    }

    private static long now() {
        return System.currentTimeMillis() / 1000;
    }

    private static String code(Runnable r) {
        return assertThrows(BusinessException.class, r::run).getCode();
    }

    @Test
    void decryptsCiphertextMadeByFrontendNodeForge() throws Exception {
        props.setMaxSkewSeconds(HUNDRED_YEARS);   // 저장된 시험 벡터라 시각 검사는 끈다
        FieldDecryptor d = new FieldDecryptor(props, keys);
        List<String> lines = Files.readAllLines(res("node-forge-vectors.txt")).stream()
                .filter(l -> l.startsWith("ENC1.")).toList();
        assertEquals(List.of("admin1234!", "한글비번Ab1!", "p@ss.word.with.dots"),
                lines.stream().map(d::decrypt).toList());
    }

    @Test
    void roundTripAndPolicies() throws Exception {
        FieldDecryptor d = new FieldDecryptor(props, keys);
        String enc = encrypt("secret", now(), "n-1", "test");
        assertEquals("secret", d.decrypt(enc));
        assertEquals("CRYPTO_REPLAY", code(() -> d.decrypt(enc)));                       // 같은 요청 재전송
        assertEquals("CRYPTO_KID", code(() -> d.decrypt(encryptUnchecked("x", now(), "n-2", "old"))));
        assertEquals("CRYPTO_EXPIRED", code(() -> d.decrypt(encryptUnchecked("x", now() - 600, "n-3", "test"))));
        String fresh = encrypt("x", now(), "n-4", "test");
        String tampered = fresh.substring(0, fresh.length() - 4) + (fresh.endsWith("AAAA") ? "BBBB" : "AAAA");
        assertEquals("CRYPTO_DECRYPT", code(() -> d.decrypt(tampered)));                  // GCM 태그 불일치
        assertEquals("CRYPTO_FORMAT", code(() -> d.decrypt("ENC1.test.abc")));
    }

    @Test
    void plainValueFollowsRequiredFlag() {
        FieldDecryptor lenient = new FieldDecryptor(props, keys);
        assertEquals("plain", lenient.decrypt("plain"));
        assertNull(lenient.decrypt(null));
        assertEquals("", lenient.decrypt(""));

        props.setRequired(true);
        FieldDecryptor strict = new FieldDecryptor(props, keys);
        assertEquals("CRYPTO_REQUIRED", code(() -> strict.decrypt("plain")));
    }

    @Test
    void disabledPassesThroughWithoutKeys() {
        FieldCryptoProperties off = new FieldCryptoProperties();
        FieldDecryptor d = new FieldDecryptor(off, null);
        assertFalse(d.isEnabled());
        assertEquals("ENC1.whatever", d.decrypt("ENC1.whatever"));
    }

    @Test
    void jacksonFieldIsDecryptedOnDeserialization() throws Exception {
        new FieldDecryptor(props, keys);   // current() 등록
        String body = "{\"userId\":\"admin\",\"password\":\"" + encrypt("admin1234!", now(), "n-json", "test") + "\"}";
        Req r = new ObjectMapper().readValue(body, Req.class);
        assertEquals("admin", r.userId);
        assertEquals("admin1234!", r.password);
    }

    @Test
    void keyFilesMissing() {
        FieldCryptoProperties p = new FieldCryptoProperties();
        p.setEnabled(true);
        p.setPrivateKeyPath("/no/such.key.pem");
        p.setPublicKeyPath("/no/such.pub.pem");
        assertThrows(IllegalStateException.class, () -> FieldKeyHolder.load(p));   // 서버: 기동 실패
        p.setGenerateIfMissing(true);
        assertNotNull(FieldKeyHolder.load(p).publicKeyPem());                       // 로컬: 임시 키
    }

    private String encryptUnchecked(String v, long ts, String n, String kid) {
        try {
            return encrypt(v, ts, n, kid);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static class Req {
        public String userId;
        @EncryptedField
        public String password;
    }
}
