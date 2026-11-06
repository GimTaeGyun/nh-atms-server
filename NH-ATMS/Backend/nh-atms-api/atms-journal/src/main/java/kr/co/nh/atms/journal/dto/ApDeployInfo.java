package kr.co.nh.atms.journal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 에이전트 open 응답의 AP 자동배포 파일 정보 (조회 전용 프로젝션).
 *
 * T_DOWNLOAD_SERVER(서버측 배포본) + T_AP_LAST_VERSION(승인버전) + T_ATM(기기 기종) 조인 결과.
 * 레거시 atmr AgentMapper.selectApLastVersion 대응.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApDeployInfo {
    private String lastDownVer;  // 최종다운로드버전 (T_DOWNLOAD_SERVER.LAST_DOWN_VER)
    private String filePath;     // 서버측 파일경로 (T_DOWNLOAD_SERVER.FILE_PATH)
    private Long   fileSize;     // 파일크기 (T_DOWNLOAD_SERVER.FILE_SIZE)
    private String downPath;     // 기기측 다운로드 대상경로 (T_AP_LAST_VERSION.FILE_PATH + '\' + FILE_NAME)
}
