package kr.co.nh.atms.web.download.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 개별다운로드관리 대상 기기 목록 행(조회 전용).
 * T_ATM + T_AGENT_STATUS + T_ATM_MODEL 조인 결과로 현재버전 vs 최종버전을 비교한다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DownloadDevice {
    private String termGroupId;     // 영업점(금고)코드
    private String termId;          // 기번
    private String branchCd;        // 본지점코드
    private String cornerCd;        // 코너코드
    private String locationName;    // 설치장소명
    private String atmCoCd;         // 기기업체코드
    private String modelCd;         // 모델코드
    private String modelName;       // 모델명
    private String atmIp;           // 기기IP
    private String appVersion;      // AP버전(T_ATM)
    private String agentVer;        // 에이전트버전(T_AGENT_STATUS)
    private String apDownVer;       // 현재 다운로드버전(T_AGENT_STATUS)
    private String lastAppVer;      // 최종 다운로드버전(T_ATM_MODEL)
    private String connectFlag;     // 접속상태(Y/N)
}
