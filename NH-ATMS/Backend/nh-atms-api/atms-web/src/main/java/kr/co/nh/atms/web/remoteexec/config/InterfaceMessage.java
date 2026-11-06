package kr.co.nh.atms.web.remoteexec.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 원격명령 MIF 스펙(interface.message.json) 로더.
 * 구 새마을금고 atms InterfaceMessage.load(ServletContext) 이식.
 * atms-web 은 전문 바이너리 변환은 하지 않고, head/body 필드 정의만 참조하여
 * snd.msg.do 요청 JSON({ip, dat:{head, body}})을 구성한다.
 */
@Component
public class InterfaceMessage {

    private static final Logger log = LoggerFactory.getLogger(InterfaceMessage.class);
    private static final String SPEC_LOCATION = "remote/interface.message.json";

    private static volatile JsonObject definition;

    @PostConstruct
    public void load() {
        try {
            ClassPathResource resource = new ClassPathResource(SPEC_LOCATION);
            try (InputStream in = resource.getInputStream()) {
                String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                definition = JsonParser.parseString(json).getAsJsonObject();
            }
            log.info("[MIF] interface.message.json loaded: {}", SPEC_LOCATION);
        } catch (Exception e) {
            log.error("[MIF] interface.message.json load failed: {}", SPEC_LOCATION, e);
        }
    }

    public static JsonObject getDefinition() {
        return definition;
    }
}
