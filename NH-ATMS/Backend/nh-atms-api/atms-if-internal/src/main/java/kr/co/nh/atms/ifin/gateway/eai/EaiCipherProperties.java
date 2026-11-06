package kr.co.nh.atms.ifin.gateway.eai;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * EAI 전문 바디 암복호화 설정.
 * 구 atmi.lib Gateway.EAI (AES/CBC/PKCS5Padding + Base64, 키 파일 로드) 외부화.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "eai.cipher")
public class EaiCipherProperties {

    /**
     * 직렬화된 java.security.Key 파일 경로 (구 WEB-INF/config/eai.cipher.client.key).
     * 미지정/미존재 시 암호화 비활성.
     */
    private String keyPath = "";

    /** AES/CBC IV (16자). 구 AS-IS 하드코딩 값 기본 적용 */
    private String iv = "14D7A984685CA8B0";

    /** 전문 바디 암호화 여부. 구 config.json eai.packet-body-encrypt */
    private boolean packetBodyEncrypt = false;
}
