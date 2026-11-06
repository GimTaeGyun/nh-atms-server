package kr.co.nh.atms.file.image.controller;

import kr.co.nh.atms.common.response.ApiResponse;
import kr.co.nh.atms.file.image.entity.JournalImage;
import kr.co.nh.atms.file.image.service.JournalImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 저널 첨부 이미지 업로드/조회/다운로드 엔드포인트 (atmr 1-C, 구 atmf 이미지 채널).
 *
 *   POST /api/file/journal-image           : 이미지 업로드(multipart) → 저장 + 메타 적재
 *   GET  /api/file/journal-image           : 저널 4PK 단위 이미지 메타 목록
 *   GET  /api/file/journal-image/download  : 이미지 바이너리 다운로드
 */
@Slf4j
@RestController
@RequestMapping("/api/file/journal-image")
@RequiredArgsConstructor
public class JournalImageController {

    private final JournalImageService service;

    @PostMapping
    public ApiResponse<JournalImage> upload(@RequestParam("file") MultipartFile file,
                                            @RequestParam String termGroupId,
                                            @RequestParam String termId,
                                            @RequestParam String jnlDate,
                                            @RequestParam String jnlTime,
                                            @RequestParam String imageType) {
        return ApiResponse.ok(service.store(file, termGroupId, termId, jnlDate, jnlTime, imageType));
    }

    @GetMapping
    public ApiResponse<List<JournalImage>> list(@RequestParam String termGroupId,
                                                @RequestParam String termId,
                                                @RequestParam String jnlDate,
                                                @RequestParam String jnlTime) {
        return ApiResponse.ok(service.list(termGroupId, termId, jnlDate, jnlTime));
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> download(@RequestParam String termGroupId,
                                           @RequestParam String termId,
                                           @RequestParam String jnlDate,
                                           @RequestParam String jnlTime,
                                           @RequestParam String imageType) {
        byte[] body = service.load(termGroupId, termId, jnlDate, jnlTime, imageType);
        String filename = termGroupId + "." + termId + "." + jnlTime + "." + imageType + ".jpg";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .contentType(MediaType.IMAGE_JPEG)
                .body(body);
    }
}
