package kr.co.nh.atms.common.security.crypto;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 요청 DTO 의 String 필드에 붙이면, 화면이 보낸 ENC1 암호문을 역직렬화 단계에서 평문으로 바꾼다.
 * 컨트롤러·서비스는 평문만 본다. (Map 바디는 FieldDecryptor.decrypt 를 직접 호출)
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonDeserialize(using = EncryptedStringDeserializer.class)
public @interface EncryptedField {
}
