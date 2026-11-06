package kr.co.nh.atms.ifin.gateway.eai;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import java.util.Base64;

/**
 * EAI 전문 바디 암복호화.
 * 구 atmi.lib Gateway.EAI 이식 (AES/CBC/PKCS5Padding + Base64, 직렬화 Key 파일 로드).
 * 키 경로/IV/암호화 플래그는 {@link EaiCipherProperties}(eai.cipher.*)로 외부화.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "internal.gateway.mode", havingValue = "eai")
public class EaiCipher {

    /** 표준전문 공통부 길이 (구 FI_COMM_HEAD_LEN) */
    public static final int FI_COMM_HEAD_LEN = 300;
    /** 데이터부 헤더 길이 (구 FI_DATA_HEAD_LEN) */
    public static final int FI_DATA_HEAD_LEN = 35;
    /** 전문 종료부 ZZ 길이 (구 FI_COMM_END_EN) */
    public static final int FI_COMM_END_EN = 2;

    /** 표준전문길이 필드 offset/len (구 FI_STND_MSG_LEN) */
    public static final int[] FI_STND_MSG_LEN = new int[]{0, 8};
    /** 데이터전문길이 필드 offset/len (구 FI_DATA_LENGTH) */
    public static final int[] FI_DATA_LENGTH = new int[]{300 + 2, 8};

    private final EaiCipherProperties props;

    private Key cipherKey;

    @PostConstruct
    void loadKey() {
        String keyPath = props.getKeyPath();
        if (keyPath == null || keyPath.isBlank()) {
            log.info("[EAI] cipher key path 미지정 - 전문 바디 암호화 비활성");
            return;
        }
        try {
            Path path = Paths.get(keyPath);
            if (!Files.exists(path)) {
                log.warn("[EAI] cipher key file not found: {} - 전문 바디 암호화 비활성", keyPath);
                return;
            }
            // AS-IS: 직렬화된 java.security.Key 객체 파일 (eai.cipher.client.key)
            try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(path))) {
                cipherKey = (Key) in.readObject();
            }
            log.info("[EAI] cipher key loaded: {} (algorithm={})", keyPath, cipherKey.getAlgorithm());
        } catch (Exception ex) {
            log.error("[EAI][runtime exception] cipher key load 실패: {}", keyPath, ex);
        }
    }

    boolean enabled() {
        return props.isPacketBodyEncrypt() && cipherKey != null;
    }

    /**
     * encrypt (구 Gateway.EAI.encrypt)
     */
    public byte[] encrypt(byte[] plainData) {
        try {
            // 2017.7.11 암호화방식 변경 DES/ECB-->AES/CBC (AS-IS 주석 유지)
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, cipherKey,
                    new IvParameterSpec(props.getIv().getBytes(StandardCharsets.UTF_8)));
            String amalgam = new String(plainData); // AS-IS 동작 유지 (플랫폼 기본 charset 경유)
            byte[] inputBytes = amalgam.getBytes(StandardCharsets.UTF_8);
            byte[] outputBytes = cipher.doFinal(inputBytes);
            String outputStr = Base64.getEncoder().encodeToString(outputBytes);
            return outputStr.getBytes();
        } catch (Exception ex) {
            log.error("[EAI][runtime exception]", ex);
            return plainData;
        }
    }

    /**
     * decrypt (구 Gateway.EAI.decrypt)
     */
    public byte[] decrypt(byte[] cipherData) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, cipherKey,
                    new IvParameterSpec(props.getIv().getBytes(StandardCharsets.UTF_8)));
            byte[] inputBytes = Base64.getDecoder().decode(cipherData);
            byte[] outputBytes = cipher.doFinal(inputBytes);
            String strResult = new String(outputBytes, StandardCharsets.UTF_8);
            return strResult.getBytes();
        } catch (Exception ex) {
            log.error("[EAI][runtime exception]", ex);
            return cipherData;
        }
    }

    /**
     * packetEncrypt (구 Gateway.EAI.packet_encrypt)
     * 헤더(300+35)는 평문 유지, 바디만 암호화 후 길이 필드 재계산.
     */
    public byte[] packetEncrypt(byte[] plainData) {
        if (!enabled()) return plainData;
        return repack(plainData, true);
    }

    /**
     * packetDecrypt (구 Gateway.EAI.packet_decrypt)
     */
    public byte[] packetDecrypt(byte[] cipherData) {
        if (!enabled()) return cipherData;
        return repack(cipherData, false);
    }

    private byte[] repack(byte[] data, boolean encrypt) {
        Charset charset = Converter.IIF.charset() != null ? Converter.IIF.charset() : Charset.forName("euc-kr");
        ByteBuffer bb = ByteBuffer.wrap(data);
        byte[] head = new byte[FI_COMM_HEAD_LEN + FI_DATA_HEAD_LEN];
        byte[] body = new byte[data.length - head.length - FI_COMM_END_EN];
        bb.get(head);
        bb.get(body);
        body = encrypt ? encrypt(body) : decrypt(body);
        bb = ByteBuffer.allocate(head.length + body.length + FI_COMM_END_EN);
        bb.put(head);
        bb.put(body);
        bb.put((byte) 'Z');
        bb.put((byte) 'Z');
        bb.position(FI_STND_MSG_LEN[0]);
        bb.put(Converter.lpad(String.valueOf(bb.limit() - FI_STND_MSG_LEN[1]), FI_STND_MSG_LEN[1], '0', charset));
        bb.position(FI_DATA_LENGTH[0]);
        bb.put(Converter.lpad(String.valueOf(body.length), FI_DATA_LENGTH[1], '0', charset));
        return bb.array();
    }
}
