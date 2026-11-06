package kr.co.nh.atms.file.image.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 저널 이미지 저장소 설정 (atmr 1-C).
 * file.storage.base-dir : 이미지 루트 디렉터리(하위에 img/yyyyMMdd/ 로 저장)
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "file.storage")
public class FileStorageProperties {
    private String baseDir = "./data/atms-img";
}
