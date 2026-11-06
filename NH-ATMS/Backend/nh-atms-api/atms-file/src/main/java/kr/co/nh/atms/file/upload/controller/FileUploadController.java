package kr.co.nh.atms.file.upload.controller;

import kr.co.nh.atms.common.response.ApiResponse;
import kr.co.nh.atms.file.upload.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 에이전트→서버 파일 업로드 엔드포인트 (계약: file 태그).
 *
 *   POST /api/file/upload/journal : 저널 IDX/TXT/IMG 파일 업로드
 *   POST /api/file/upload/log     : AP/Agent 로그 업로드(FCOL 명령 회수)
 */
@Slf4j
@RestController
@RequestMapping("/api/file/upload")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileUploadService service;

    @PostMapping("/journal")
    public ApiResponse<Map<String, Object>> uploadJournal(
            @RequestParam("file") MultipartFile file,
            @RequestParam String atmId,
            @RequestParam String fileType,
            @RequestParam String fileDate,
            @RequestParam(defaultValue = "false") boolean compressed) {
        return ApiResponse.ok(service.uploadJournal(file, atmId, fileType, fileDate, compressed));
    }

    @PostMapping("/log")
    public ApiResponse<Map<String, Object>> uploadLog(
            @RequestParam("file") MultipartFile file,
            @RequestParam String atmId,
            @RequestParam String logType,
            @RequestParam String logDate,
            @RequestParam(defaultValue = "false") boolean compressed) {
        return ApiResponse.ok(service.uploadLog(file, atmId, logType, logDate, compressed));
    }
}
