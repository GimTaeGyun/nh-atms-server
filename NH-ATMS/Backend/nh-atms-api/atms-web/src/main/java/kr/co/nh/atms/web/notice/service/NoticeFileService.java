package kr.co.nh.atms.web.notice.service;

import kr.co.nh.atms.common.exception.BusinessException;
import kr.co.nh.atms.web.notice.entity.AttachedFile;
import kr.co.nh.atms.web.notice.mapper.AttachedFileMapper;
import kr.co.nh.atms.web.notice.mapper.NoticeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeFileService {

    private final AttachedFileMapper fileMapper;
    private final NoticeMapper       noticeMapper;

    @Value("${notice.upload-dir:./uploads/notice}")
    private String uploadDir;

    private static final Set<String> ALLOWED_EXT =
            Set.of("pdf", "hwp", "hwpx", "xlsx", "xls", "docx", "zip",
                   "jpg", "jpeg", "png", "gif", "bmp", "webp");
    private static final int MAX_FILES = 3;

    public List<AttachedFile> getFiles(String catCd, Long seqNo) {
        try {
            return fileMapper.selectByNotice(catCd, seqNo);
        } catch (Exception e) {
            log.warn("첨부파일 조회 실패 catCd={} seqNo={}: {}", catCd, seqNo, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Transactional
    public void saveFiles(String catCd, Long seqNo, List<MultipartFile> files) throws IOException {
        if (files == null) return;
        List<MultipartFile> valid = files.stream().filter(f -> !f.isEmpty()).toList();
        if (valid.isEmpty()) return;
        if (valid.size() > MAX_FILES)
            throw new BusinessException("NTC_003", "첨부파일은 최대 " + MAX_FILES + "개까지 가능합니다.");

        Path dir = Paths.get(uploadDir, catCd, String.valueOf(seqNo));
        Files.createDirectories(dir);

        for (MultipartFile mf : valid) {
            String orig  = sanitize(mf.getOriginalFilename());
            validateExt(orig);
            Long fileNo  = fileMapper.nextFileNo(catCd, seqNo);
            mf.transferTo(dir.resolve(fileNo + "_" + orig));
            fileMapper.insert(AttachedFile.builder()
                    .catCd(catCd).seqNo(seqNo).fileNo(fileNo)
                    .attachedFileName(orig).attachedFileSize(mf.getSize())
                    .build());
        }

        /* 첨부파일 존재 플래그 업데이트 */
        var notice = noticeMapper.selectOne(catCd, seqNo);
        if (notice != null) {
            notice.setAttachedFileFlag("Y");
            noticeMapper.update(notice);
        }
    }

    @Transactional
    public void deleteFile(String catCd, Long seqNo, Long fileNo) {
        AttachedFile f = fileMapper.selectByNotice(catCd, seqNo).stream()
                .filter(af -> af.getFileNo().equals(fileNo)).findFirst()
                .orElseThrow(() -> new BusinessException("NTC_002", "파일을 찾을 수 없습니다."));
        fileMapper.delete(catCd, seqNo, fileNo);
        deletePhysical(catCd, seqNo, fileNo, f.getAttachedFileName());

        if (fileMapper.selectByNotice(catCd, seqNo).isEmpty()) {
            var notice = noticeMapper.selectOne(catCd, seqNo);
            if (notice != null) { notice.setAttachedFileFlag("N"); noticeMapper.update(notice); }
        }
    }

    @Transactional
    public void deleteAllFiles(String catCd, Long seqNo) {
        // 1) DB에서 파일 목록 조회 후 물리 파일 삭제
        try {
            List<AttachedFile> files = fileMapper.selectByNotice(catCd, seqNo);
            for (AttachedFile f : files) {
                deletePhysical(catCd, seqNo, f.getFileNo(), f.getAttachedFileName());
            }
        } catch (Exception e) {
            log.warn("물리 파일 삭제 실패 catCd={} seqNo={}: {}", catCd, seqNo, e.getMessage());
        }

        // 2) 게시글 디렉토리 삭제 (uploads/notice/{catCd}/{seqNo}/)
        deleteDirectory(Paths.get(uploadDir, catCd, String.valueOf(seqNo)));

        // 3) DB 레코드 삭제
        try { fileMapper.deleteByNotice(catCd, seqNo); } catch (Exception e) {
            log.warn("첨부파일 DB 삭제 실패 catCd={} seqNo={}: {}", catCd, seqNo, e.getMessage());
        }
    }

    /** 디렉토리 내 파일 전체 삭제 후 디렉토리 제거 */
    private void deleteDirectory(Path dir) {
        if (!Files.exists(dir)) return;
        try {
            // 하위 파일·폴더를 깊이 역순으로 삭제
            Files.walk(dir)
                 .sorted(Comparator.reverseOrder())
                 .forEach(p -> {
                     try { Files.deleteIfExists(p); }
                     catch (IOException e) {
                         log.warn("파일 삭제 실패: {} — {}", p, e.getMessage());
                     }
                 });
        } catch (IOException e) {
            log.warn("디렉토리 삭제 실패: {} — {}", dir, e.getMessage());
        }
    }

    public Resource download(String catCd, Long seqNo, Long fileNo, String[] outName)
            throws MalformedURLException {
        AttachedFile f = fileMapper.selectByNotice(catCd, seqNo).stream()
                .filter(af -> af.getFileNo().equals(fileNo)).findFirst()
                .orElseThrow(() -> new BusinessException("NTC_002", "파일을 찾을 수 없습니다."));

        outName[0] = f.getAttachedFileName();
        Path path = Paths.get(uploadDir, catCd, String.valueOf(seqNo),
                               fileNo + "_" + f.getAttachedFileName()).toAbsolutePath().normalize();

        if (!Files.exists(path) || !Files.isReadable(path)) {
            log.error("파일이 서버에 존재하지 않음: {}", path);
            throw new BusinessException("NTC_005",
                    "파일이 서버에 존재하지 않습니다. 관리자에게 문의하세요. (path: " + path + ")");
        }

        return new UrlResource(path.toUri());
    }

    private void deletePhysical(String catCd, Long seqNo, Long fileNo, String name) {
        try { Files.deleteIfExists(Paths.get(uploadDir, catCd, String.valueOf(seqNo), fileNo + "_" + name)); }
        catch (IOException ignored) {}
    }

    private String sanitize(String name) {
        return name == null ? "file" : name.replaceAll("[/\\\\&]", "");
    }

    private void validateExt(String name) {
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toLowerCase() : "";
        if (!ALLOWED_EXT.contains(ext))
            throw new BusinessException("NTC_004",
                    "허용되지 않는 파일 형식입니다. (" + String.join(", ", ALLOWED_EXT) + ")");
    }
}
