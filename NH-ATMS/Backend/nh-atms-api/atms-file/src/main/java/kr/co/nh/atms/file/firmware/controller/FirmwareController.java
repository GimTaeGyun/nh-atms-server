package kr.co.nh.atms.file.firmware.controller;

import kr.co.nh.atms.common.response.ApiResponse;
import kr.co.nh.atms.file.firmware.dto.FirmwareFile;
import kr.co.nh.atms.file.firmware.dto.FirmwareInfo;
import kr.co.nh.atms.file.firmware.service.FirmwareService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 펌웨어 다운로드 엔드포인트 (구 atmf 펌웨어 채널).
 *
 *   GET /api/file/download/{atmId}/{fileName} : Agent 펌웨어 바이너리 다운로드 (octet-stream)
 *                                               응답 헤더 X-Checksum-SHA256 로 무결성 검증
 *                                               (Resource 기반 스트리밍 — 전체 메모리 적재 없음)
 *   GET /api/file/firmware/list               : 운영자 화면용 펌웨어 목록
 *
 * 계약: Document/agent-api-openapi.yaml — /api/file/download/{atmId}/{fileName}
 */
@Slf4j
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FirmwareController {

    private final FirmwareService service;

    @GetMapping("/download/{atmId}/{fileName}")
    public ResponseEntity<Resource> download(@PathVariable String atmId,
                                             @PathVariable String fileName) {
        FirmwareFile fw = service.resolve(atmId, fileName);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fw.fileName() + "\"")
                .header("X-Checksum-SHA256", fw.checksum())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(fw.size())
                .body(new FileSystemResource(fw.path()));
    }

    @GetMapping("/firmware/list")
    public ApiResponse<List<FirmwareInfo>> list() {
        return ApiResponse.ok(service.list());
    }
}
