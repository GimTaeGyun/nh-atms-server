package kr.co.nh.atms.file.firmware.service;

import kr.co.nh.atms.common.exception.BusinessException;
import kr.co.nh.atms.file.firmware.config.FirmwareProperties;
import kr.co.nh.atms.file.firmware.dto.FirmwareFile;
import kr.co.nh.atms.file.firmware.dto.FirmwareInfo;
import kr.co.nh.atms.file.firmware.sftp.SftpFirmwareClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 펌웨어 다운로드 서비스 (구 atmf 펌웨어 채널).
 *
 * <ul>
 *   <li>Agent 다운로드 요청 시 로컬 저장소에서 파일 제공 + SHA-256 체크섬 산출</li>
 *   <li>로컬에 없고 SFTP 연동이 켜져 있으면 원격에서 끌어온 뒤 제공</li>
 * </ul>
 * 파일명은 경로 구분자/상위참조를 차단해 traversal 을 방지한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FirmwareService {

    /** 영문/숫자/점/언더스코어/하이픈만 허용 (경로 구분자·상위참조 차단). */
    private static final Pattern SAFE_NAME = Pattern.compile("^[A-Za-z0-9._-]+$");

    private final FirmwareProperties props;
    private final SftpFirmwareClient sftp;

    /**
     * Agent 다운로드: 로컬 저장소 우선, 없으면(설정 시) SFTP fetch 후 파일 참조 + 체크섬 반환.
     * 체크섬은 스트리밍(버퍼 단위)으로 계산해 대용량 파일도 메모리에 전체 적재하지 않는다.
     */
    public FirmwareFile resolve(String atmId, String fileName) {
        String name = sanitize(fileName);
        Path root = repoRoot();
        Path target = root.resolve(name).normalize();
        ensureWithinRoot(root, target);

        if (!Files.isReadable(target) && sftp.isEnabled()) {
            sftp.fetch(name, target);
        }
        if (!Files.isReadable(target)) {
            throw new BusinessException("FW_NOT_FOUND", "펌웨어 파일을 찾을 수 없습니다: " + name);
        }
        try {
            long size = Files.size(target);
            String checksum = sha256Hex(target);
            log.info("[firmware] serve atm={} file={} size={} sha256={}", atmId, name, size, checksum);
            return new FirmwareFile(name, target, size, checksum);
        } catch (IOException e) {
            throw new BusinessException("FW_READ_FAIL", "펌웨어 읽기 실패: " + e.getMessage());
        }
    }

    /** 운영자 화면용 펌웨어 목록 (로컬 저장소 기준). */
    public List<FirmwareInfo> list() {
        Path root = repoRoot();
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (Stream<Path> paths = Files.list(root)) {
            return paths.filter(Files::isRegularFile)
                    .map(FirmwareService::toInfo)
                    .sorted(Comparator.comparing(FirmwareInfo::fileName))
                    .toList();
        } catch (IOException e) {
            throw new BusinessException("FW_LIST_FAIL", "펌웨어 목록 조회 실패: " + e.getMessage());
        }
    }

    private static FirmwareInfo toInfo(Path p) {
        try {
            return new FirmwareInfo(p.getFileName().toString(), Files.size(p),
                    Files.getLastModifiedTime(p).toMillis());
        } catch (IOException e) {
            return new FirmwareInfo(p.getFileName().toString(), 0L, 0L);
        }
    }

    private Path repoRoot() {
        return Paths.get(props.getRepoDir()).toAbsolutePath().normalize();
    }

    private static void ensureWithinRoot(Path root, Path target) {
        if (!target.startsWith(root)) {
            throw new BusinessException("INVALID_PATH", "허용되지 않은 경로입니다.");
        }
    }

    private static String sanitize(String name) {
        if (name == null || name.isBlank() || !SAFE_NAME.matcher(name).matches()) {
            throw new BusinessException("INVALID_FILENAME", "허용되지 않은 파일명입니다.");
        }
        return name;
    }

    private static String sha256Hex(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[64 * 1024];
            int n;
            while ((n = in.read(buf)) != -1) {
                md.update(buf, 0, n);
            }
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new BusinessException("CHECKSUM_FAIL", "체크섬 계산 실패: " + e.getMessage());
        }
    }
}
