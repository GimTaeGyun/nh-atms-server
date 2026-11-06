package kr.co.nh.atms.file.firmware.dto;

/**
 * 운영자 화면용 펌웨어 목록 항목.
 *
 * @param fileName     파일명
 * @param size         바이트 크기
 * @param lastModified 최종 수정 시각(epoch millis)
 */
public record FirmwareInfo(String fileName, long size, long lastModified) {
}
