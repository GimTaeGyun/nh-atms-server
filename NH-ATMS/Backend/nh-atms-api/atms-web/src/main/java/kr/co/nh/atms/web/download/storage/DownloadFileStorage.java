package kr.co.nh.atms.web.download.storage;

import kr.co.nh.atms.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.regex.Pattern;

/**
 * 개별다운로드 업로드 파일 저장소 (구 MG repository.temp 정합).
 *
 * <p>운영자가 업로드한 AP/일반 ZIP 파일을 서버 저장소 하위 {@code temp/} 에 보관한다.
 * 보관 파일은 에이전트가 다운로드받을 수 있도록 atms-file(파일 서버)이 노출하는 경로와
 * 동일 볼륨이어야 한다. 푸시 전문에는 저장소 루트 기준 상대경로(예: {@code /temp/0000007_ap.zip})를
 * 내려 보낸다.</p>
 *
 * <p>보안: 확장자 화이트리스트(zip), 파일명 정규화, 경로 이탈(Path Traversal) 차단.</p>
 */
@Slf4j
@Component
public class DownloadFileStorage {

    /** 허용 확장자(개별다운로드 AP/일반 파일은 ZIP 패키지). */
    private static final String ALLOWED_EXTENSION = "zip";
    private static final Pattern UNSAFE = Pattern.compile("[^A-Za-z0-9._-]");

    private final Path rootDir;

    public DownloadFileStorage(@Value("${download.storage.base-dir:./data/atms-download}") String baseDir) {
        this.rootDir = Paths.get(baseDir).toAbsolutePath().normalize();
    }

    /** 저장 결과(서버 절대경로 + 저장소 루트 기준 상대경로). */
    public record Stored(String absolutePath, String relativePath, String storedFileName) {}

    /**
     * 업로드 파일을 {@code {root}/temp/{downNo}_{원본파일명}} 으로 저장한다.
     *
     * @param file   업로드 ZIP 파일
     * @param downNo 다운로드일련번호(파일명 충돌 방지 접두)
     */
    public Stored store(MultipartFile file, String downNo) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("DL_010", "업로드 파일이 비어 있습니다.");
        }
        String origin = file.getOriginalFilename();
        if (origin == null || origin.isBlank()) {
            throw new BusinessException("DL_011", "파일명을 확인할 수 없습니다.");
        }
        String ext = extensionOf(origin);
        if (!ALLOWED_EXTENSION.equalsIgnoreCase(ext)) {
            throw new BusinessException("DL_012", "허용되지 않는 파일 형식입니다. (zip 만 가능)");
        }

        String safeName = downNo + "_" + UNSAFE.matcher(origin).replaceAll("_");
        Path tempDir = rootDir.resolve("temp").normalize();
        Path target = tempDir.resolve(safeName).normalize();
        if (!target.startsWith(rootDir)) {
            throw new BusinessException("DL_013", "잘못된 저장 경로입니다.");
        }

        try {
            Files.createDirectories(tempDir);
            try (var in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.error("[download] 파일 저장 실패 downNo={} name={}", downNo, safeName, e);
            throw new BusinessException("DL_014", "파일 저장에 실패했습니다.");
        }

        String relative = "/temp/" + safeName;
        log.info("[download] file stored downNo={} path={} size={}", downNo, target, file.getSize());
        return new Stored(target.toString(), relative, safeName);
    }

    private String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1);
    }
}
