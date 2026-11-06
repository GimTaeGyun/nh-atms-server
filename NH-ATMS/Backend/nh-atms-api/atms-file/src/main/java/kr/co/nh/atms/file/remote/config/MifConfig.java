package kr.co.nh.atms.file.remote.config;

import jakarta.annotation.PostConstruct;
import kr.co.nh.atms.file.remote.mif.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 기동 시 MIF 스펙(interface.message.json)을 Converter.MIF 싱글톤에 로딩.
 * 구 atmf InterfaceMessage.load(ServletContext) 역할.
 */
@Component
public class MifConfig {

    private static final Logger log = LoggerFactory.getLogger(MifConfig.class);

    private final RemoteProperties props;

    public MifConfig(RemoteProperties props) {
        this.props = props;
    }

    @PostConstruct
    public void load() {
        try {
            ClassPathResource resource = new ClassPathResource(props.getSpecLocation());
            try (InputStream in = resource.getInputStream()) {
                String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                Converter.MIF.load(props.getInterfaceName(), json);
            }
            log.info("[MIF] spec loaded: name={}, charset={}, location={}",
                    Converter.MIF.name(), Converter.MIF.charset(), props.getSpecLocation());
        } catch (Exception e) {
            log.error("[MIF] spec load failed: location={}", props.getSpecLocation(), e);
        }
    }
}
