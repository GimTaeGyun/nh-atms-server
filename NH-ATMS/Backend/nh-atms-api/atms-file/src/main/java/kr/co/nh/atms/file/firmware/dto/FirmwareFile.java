package kr.co.nh.atms.file.firmware.dto;

import java.nio.file.Path;

/**
 * Agent 에 제공할 펌웨어 파일 참조 + 무결성 체크섬.
 * (대용량 OOM 방지: 바이너리 전체를 메모리에 올리지 않고 경로/크기만 보유 — 응답은 스트리밍)
 *
 * @param fileName 파일명
 * @param path     로컬 저장소 내 파일 경로
 * @param size     파일 크기(byte)
 * @param checksum SHA-256 hex (X-Checksum-SHA256 헤더로 전달)
 */
public record FirmwareFile(String fileName, Path path, long size, String checksum) {
}
