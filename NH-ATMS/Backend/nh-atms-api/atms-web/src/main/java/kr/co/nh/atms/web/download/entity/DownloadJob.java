package kr.co.nh.atms.web.download.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 다운로드 작업/배포 이력(T_DOWNLOAD_JOB) = "누구에게 어떻게 보냈는지".
 * 패키지를 특정 기기에 배포한 단위. 실제 전송은 T_AGENT_COMMAND 큐로 발행하고
 * 그 CMD_ID 를 연계 보관한다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DownloadJob {
    private Long jobId;
    private Long pkgId;
    private String termGroupId;
    private String termId;
    private String execType;   // I=개별, B=일괄
    private String status;     // W/S/C/F/X
    private String cmdId;
    private String reserveDate;
    private String reserveTime;
    private String resultCd;
    private String resultMsg;
    private LocalDateTime resultDt;
    private String regUserId;
    private LocalDateTime regDt;

    /** 조회 조인 부가정보(매핑 전용) */
    private String pkgName;
    private String pkgType;
    private String downVer;
    private String fileName;
}
