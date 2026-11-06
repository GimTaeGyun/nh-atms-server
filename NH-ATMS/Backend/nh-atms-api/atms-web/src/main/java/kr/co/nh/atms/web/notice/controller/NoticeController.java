package kr.co.nh.atms.web.notice.controller;

import kr.co.nh.atms.common.page.PageResult;
import kr.co.nh.atms.common.response.ApiResponse;
import kr.co.nh.atms.web.notice.dto.NoticeSearchDto;
import kr.co.nh.atms.web.notice.entity.AttachedFile;
import kr.co.nh.atms.web.notice.entity.Notice;
import kr.co.nh.atms.web.notice.service.NoticeFileService;
import kr.co.nh.atms.web.notice.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notice")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService     noticeService;
    private final NoticeFileService fileService;

    /** 목록 조회 */
    @GetMapping
    public ApiResponse<PageResult<Notice>> list(@ModelAttribute NoticeSearchDto s) {
        return ApiResponse.ok(noticeService.search(s));
    }

    /** 조회수 증가 (아코디언 펼칠 때) */
    @PatchMapping("/{catCd}/{seqNo}/hit")
    public ApiResponse<Map<String, Long>> hit(
            @PathVariable String catCd, @PathVariable Long seqNo) {
        return ApiResponse.ok(Map.of("hit", noticeService.increaseHit(catCd, seqNo)));
    }

    /** 첨부파일 목록 */
    @GetMapping("/{catCd}/{seqNo}/files")
    public ApiResponse<List<AttachedFile>> files(
            @PathVariable String catCd, @PathVariable Long seqNo) {
        return ApiResponse.ok(fileService.getFiles(catCd, seqNo));
    }

    /** 등록 (JSON — 파일은 별도 업로드) */
    @PostMapping
    public ApiResponse<Map<String, Long>> create(@RequestBody Notice n) {
        n.setAttachedFileFlag("N");
        Long seq = noticeService.create(n);
        return ApiResponse.ok(Map.of("seqNo", seq), "공지사항 등록 성공");
    }

    /** 파일 첨부 업로드 (등록 후 별도 호출) */
    @PostMapping(value = "/{catCd}/{seqNo}/files",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Void> uploadFiles(
            @PathVariable String catCd, @PathVariable Long seqNo,
            @RequestPart("files") List<MultipartFile> files) throws IOException {
        fileService.saveFiles(catCd, seqNo, files);
        return ApiResponse.ok(null, "파일 업로드 성공");
    }

    /** 수정 */
    @PutMapping("/{catCd}/{seqNo}")
    public ApiResponse<Void> update(
            @PathVariable String catCd, @PathVariable Long seqNo,
            @RequestBody Notice n) {
        n.setCatCd(catCd);
        n.setSeqNo(seqNo);
        noticeService.update(n);
        return ApiResponse.ok(null, "공지사항 수정 성공");
    }

    /** 삭제 */
    @DeleteMapping("/{catCd}/{seqNo}")
    public ApiResponse<Void> delete(
            @PathVariable String catCd, @PathVariable Long seqNo) {
        fileService.deleteAllFiles(catCd, seqNo);
        noticeService.delete(catCd, seqNo);
        return ApiResponse.ok(null, "공지사항 삭제 성공");
    }

    /** 첨부파일 단건 삭제 */
    @DeleteMapping("/{catCd}/{seqNo}/files/{fileNo}")
    public ApiResponse<Void> deleteFile(
            @PathVariable String catCd, @PathVariable Long seqNo,
            @PathVariable Long fileNo) {
        fileService.deleteFile(catCd, seqNo, fileNo);
        return ApiResponse.ok(null, "파일 삭제 성공");
    }

    /** 파일 다운로드 */
    @GetMapping("/{catCd}/{seqNo}/files/{fileNo}/download")
    public ResponseEntity<Resource> download(
            @PathVariable String catCd, @PathVariable Long seqNo,
            @PathVariable Long fileNo) throws IOException {
        String[] outName = {""};
        Resource resource = fileService.download(catCd, seqNo, fileNo, outName);
        String encoded = URLEncoder.encode(outName[0], StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}
