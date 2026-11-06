package kr.co.nh.atms.file.image.service;

import kr.co.nh.atms.common.exception.BusinessException;
import kr.co.nh.atms.file.image.config.FileStorageProperties;
import kr.co.nh.atms.file.image.entity.JournalImage;
import kr.co.nh.atms.file.image.mapper.JournalImageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 저널 이미지 업로드/조회/다운로드 서비스 (atmr 1-C).
 *   - 실파일은 {base-dir}/img/{jnlDate}/ 하위에 저장
 *   - 메타는 T_JOURNAL_IMAGE upsert
 * 경로 traversal 방지를 위해 입력값 정규화 + 저장 루트 하위 여부 검증.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JournalImageService {

    private static final Pattern SAFE = Pattern.compile("[^A-Za-z0-9_-]");

    private final JournalImageMapper mapper;
    private final FileStorageProperties props;

    @Transactional
    public JournalImage store(MultipartFile file, String termGroupId, String termId,
                              String jnlDate, String jnlTime, String imageType) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("FILE_EMPTY", "업로드 파일이 비어 있습니다.");
        }
        String tg = require(termGroupId, "termGroupId");
        String tm = require(termId, "termId");
        String jd = require(jnlDate, "jnlDate");
        String jt = require(jnlTime, "jnlTime");
        String it = require(imageType, "imageType");

        Path root = storageRoot();
        Path dir = root.resolve("img").resolve(sanitize(jd)).normalize();
        ensureWithinRoot(root, dir);

        String filename = String.join(".",
                sanitize(tg), sanitize(tm), sanitize(jt), sanitize(it)) + ".jpg";
        Path target = dir.resolve(filename).normalize();
        ensureWithinRoot(root, target);

        try {
            Files.createDirectories(dir);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessException("FILE_WRITE_FAIL", "이미지 저장 실패: " + e.getMessage());
        }

        JournalImage img = JournalImage.builder()
                .termGroupId(tg).termId(tm).jnlDate(jd).jnlTime(jt)
                .imageType(it)
                .imagePath(target.toString())
                .build();
        mapper.upsert(img);
        log.info("[journal-image] stored term={}/{} {} type={} path={}", tg, tm, jt, it, target);
        return img;
    }

    @Transactional(readOnly = true)
    public List<JournalImage> list(String termGroupId, String termId, String jnlDate, String jnlTime) {
        return mapper.selectByJournal(termGroupId, termId, jnlDate, jnlTime);
    }

    @Transactional(readOnly = true)
    public byte[] load(String termGroupId, String termId, String jnlDate, String jnlTime, String imageType) {
        JournalImage img = mapper.selectOne(termGroupId, termId, jnlDate, jnlTime, imageType);
        if (img == null || img.getImagePath() == null) {
            throw new BusinessException("IMAGE_NOT_FOUND", "이미지 메타가 없습니다.");
        }
        Path root = storageRoot();
        Path file = Paths.get(img.getImagePath()).normalize();
        ensureWithinRoot(root, file);
        if (!Files.isReadable(file)) {
            throw new BusinessException("IMAGE_FILE_MISSING", "이미지 파일을 찾을 수 없습니다.");
        }
        try {
            return Files.readAllBytes(file);
        } catch (IOException e) {
            throw new BusinessException("IMAGE_READ_FAIL", "이미지 읽기 실패: " + e.getMessage());
        }
    }

    private Path storageRoot() {
        return Paths.get(props.getBaseDir()).toAbsolutePath().normalize();
    }

    private static void ensureWithinRoot(Path root, Path target) {
        if (!target.startsWith(root)) {
            throw new BusinessException("INVALID_PATH", "허용되지 않은 경로입니다.");
        }
    }

    private static String require(String v, String name) {
        if (v == null || v.isBlank()) {
            throw new BusinessException("REQUIRED", name + " 는 필수입니다.");
        }
        return v.trim();
    }

    /** 파일명 구성요소에서 경로 구분자/특수문자 제거. */
    private static String sanitize(String v) {
        return SAFE.matcher(v).replaceAll("");
    }
}
