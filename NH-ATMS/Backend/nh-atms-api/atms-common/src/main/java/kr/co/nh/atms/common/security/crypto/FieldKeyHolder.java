package kr.co.nh.atms.common.security.crypto;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * RSA 키쌍 보관. 서버는 파일(WAR 밖, /home/atmswas/keys)에서 읽고,
 * 로컬은 파일이 없으면 generate-if-missing 으로 메모리 임시 키를 만든다 (재기동마다 바뀜).
 */
@Slf4j
public class FieldKeyHolder {

    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final String publicKeyPem;

    public FieldKeyHolder(PrivateKey privateKey, PublicKey publicKey) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.publicKeyPem = toPem("PUBLIC KEY", publicKey.getEncoded());
    }

    public static FieldKeyHolder load(FieldCryptoProperties p) {
        Path priv = p.getPrivateKeyPath() == null ? null : Path.of(p.getPrivateKeyPath());
        Path pub = p.getPublicKeyPath() == null ? null : Path.of(p.getPublicKeyPath());
        if (priv != null && pub != null && Files.isReadable(priv) && Files.isReadable(pub)) {
            try {
                KeyFactory kf = KeyFactory.getInstance("RSA");
                PrivateKey pk = kf.generatePrivate(new PKCS8EncodedKeySpec(fromPem(Files.readString(priv, StandardCharsets.US_ASCII))));
                PublicKey pu = kf.generatePublic(new X509EncodedKeySpec(fromPem(Files.readString(pub, StandardCharsets.US_ASCII))));
                log.info("[field-crypto] 키 로드 kid={} ({})", p.getKid(), pub);
                return new FieldKeyHolder(pk, pu);
            } catch (Exception e) {
                throw new IllegalStateException("[field-crypto] 키 파일을 읽을 수 없습니다: " + priv + " / " + pub, e);
            }
        }
        if (p.isGenerateIfMissing()) {
            try {
                KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
                g.initialize(2048);
                KeyPair kp = g.generateKeyPair();
                log.warn("[field-crypto] 키 파일 없음 → 임시 키 생성 (로컬 전용, 재기동하면 바뀜) kid={}", p.getKid());
                return new FieldKeyHolder(kp.getPrivate(), kp.getPublic());
            } catch (Exception e) {
                throw new IllegalStateException("[field-crypto] 임시 키 생성 실패", e);
            }
        }
        throw new IllegalStateException("[field-crypto] crypto.field.enabled=true 인데 키 파일이 없습니다: "
                + priv + " / " + pub + " (docs/FIELD_ENCRYPTION_DESIGN.md 5장)");
    }

    public PrivateKey privateKey() { return privateKey; }
    public PublicKey publicKey() { return publicKey; }
    public String publicKeyPem() { return publicKeyPem; }

    static byte[] fromPem(String pem) {
        String body = pem.replaceAll("-----[A-Z ]+-----", "").replaceAll("\\s", "");
        return Base64.getDecoder().decode(body);
    }

    static String toPem(String type, byte[] der) {
        String b64 = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII)).encodeToString(der);
        return "-----BEGIN " + type + "-----\n" + b64 + "\n-----END " + type + "-----\n";
    }
}
