package kr.co.nh.atms.common.security.crypto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;

import java.io.IOException;

/** {@link EncryptedField} 용 역직렬화기. 복호화기가 아직 없으면(테스트 등) 값을 그대로 둔다. */
public class EncryptedStringDeserializer extends StdScalarDeserializer<String> {

    public EncryptedStringDeserializer() {
        super(String.class);
    }

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getValueAsString();
        FieldDecryptor d = FieldDecryptor.current();
        return d == null ? value : d.decrypt(value);
    }
}
