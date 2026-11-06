package kr.co.nh.atms.common.security.crypto;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 민감필드 암호화 빈. enabled=false 면 키를 읽지 않고 복호화기는 평문을 그대로 통과시킨다. */
@Configuration
@EnableConfigurationProperties(FieldCryptoProperties.class)
public class FieldCryptoConfig {

    @Bean
    public FieldDecryptor fieldDecryptor(FieldCryptoProperties props) {
        FieldKeyHolder keys = props.isEnabled() ? FieldKeyHolder.load(props) : null;
        return new FieldDecryptor(props, keys);
    }

    @Bean
    public FieldPublicKeyProvider fieldPublicKeyProvider(FieldCryptoProperties props, FieldDecryptor decryptor) {
        return new FieldPublicKeyProvider(props, decryptor);
    }
}
