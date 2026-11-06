package kr.co.nh.atms.file.upload.service;

import kr.co.nh.atms.common.exception.BusinessException;
import kr.co.nh.atms.file.image.config.FileStorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 에이전트→서버 파일 업로드 서비스 (계약: file/upload/journal, file/upload/log).
 *   - 저널 IDX/TXT/IMG : {base-dir}/upload/journal/{atmId}/{fileDate}/
 *   - AP/Agent 로그    : {base-dir}/upload/log/{atmId}/{logDate}/
 * 경로 traversal 방지를 위해 입력값 정규화 + 저장 루트 하위 여부 검증.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadService {

    private static final Pattern SAFE = Pattern.compile("[^A-Za-z0-9_-]");

    private static final Set<String> JOURNAL_TYPES =
            Set.of("HDX", "IDX_STA", "IDX_TRA", "IDX_BAL", "IDX_TOT", "IDX_OPA", "TXT", "IMG");
    private static final Set<String> LOG_TYPES =
            Set.of("AP_LOG", "AGENT_LOG", "GIRO_IMAGE");

    private final FileStorageProperties props;

    /** 저널 IDX/TXT/IMG 파일 업로드. */
    public Map<String, Object> uploadJournal(MultipartFile file, String atmId, String fileType,
                                             String fileDate, boolean compressed) {
        String id = requireAtmId(atmId);
        String type = requireType(fileType, JOURNAL_TYPES, "fileType");
        String date = require(fileDate, "fileDate");
        return store(file, "journal", id, type, date, compressed);
    }

    /** AP/Agent 로그 업로드(FCOL 명령 회수). */
    public Map<String, Object> uploadLog(MultipartFile file, String atmId, String logType,
                                         String logDate, boolean compressed) {
        String id = requireAtmId(atmId);
        String type = requireType(logType, LOG_TYPES, "logType");
        String date = require(logDate, "logDate");
        return store(file, "log", id, type, date, compressed);
    }

    private Map<String, Object> store(MultipartFile file, String category, String atmId,
                                      String type, String date, boolean compressed) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("FILE_EMPTY", "업로드 파일이 비어 있습니다.");
        }
        Path root = storageRoot();
        Path dir = root.resolve("upload").resolve(category)
                .resolve(sanitize(atmId)).resolve(sanitize(date)).normalize();
        ensureWithinRoot(root, dir);

        String original = file.getOriginalFilename();
        String ext = compressed ? ".zip" : extOf(original);
        String filename = sanitize(type) + "_" + System.currentTimeMillis() + ext;
        Path target = dir.resolve(filename).normalize();
        ensureWithinRoot(root, target);

        try {
            Files.createDirectories(dir);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessException("FILE_WRITE_FAIL", "파일 저장 실패: " + e.getMessage());
        }

        long size = file.getSize();
        log.info("[file-upload] {} atmId={} type={} date={} size={} path={}",
                category, atmId, type, date, size, target);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("atmId", atmId);
        res.put("category", category);
        res.put("type", type);
        res.put("fileDate", date);
        res.put("storedName", filename);
        res.put("size", size);
        res.put("compressed", compressed);
        return res;
    }

    private Path storageRoot() {
        return Paths.get(props.getBaseDir()).toAbsolutePath().normalize();
    }

    private static void ensureWithinRoot(Path root, Path target) {
        if (!target.startsWith(root)) {
            throw new BusinessException("INVALID_PATH", "허용되지 않은 경로입니다.");
        }
    }

    private static String requireAtmId(String atmId) {
        String id = atmId == null ? "" : atmId.trim();
        if (id.length() != 8) {
            throw new BusinessException("INVALID_ATM_ID", "atmId 는 8자리여야 합니다.");
        }
        return id;
    }

    private static String requireType(String v, Set<String> allowed, String name) {
        String t = require(v, name);
        if (!allowed.contains(t)) {
            throw new BusinessException("INVALID_TYPE", "지원하지 않는 " + name + " 입니다: " + t);
        }
        return t;
    }

    private static String require(String v, String name) {
        if (v == null || v.isBlank()) {
            throw new BusinessException("REQUIRED", name + " 는 필수입니다.");
        }
        return v.trim();
    }

    private static String sanitize(String v) {
        return SAFE.matcher(v).replaceAll("");
    }

    /** 원본 파일명 확장자(없으면 .bin). 경로/특수문자는 제거. */
    private static String extOf(String original) {
        if (original == null) return ".bin";
        int dot = original.lastIndexOf('.');
        if (dot < 0 || dot == original.length() - 1) return ".bin";
        String ext = original.substring(dot + 1);
        return "." + SAFE.matcher(ext).replaceAll("");
    }
}
