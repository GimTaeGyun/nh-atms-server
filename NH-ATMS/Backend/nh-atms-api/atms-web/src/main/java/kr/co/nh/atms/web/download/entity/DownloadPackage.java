package kr.co.nh.atms.web.download.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 다운로드 패키지 마스터(T_DOWNLOAD_PACKAGE) = "무엇을 보낼지".
 * 기종(업체+모델)별 파일 묶음. 유형(PKG_TYPE) 단일 컬럼으로 AP/일반/매체를 통합한다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DownloadPackage {
    private Long pkgId;
    private String pkgName;
    private String pkgType;        // A=AP, G=일반, M=매체
    private String atmCoCd;
    private String modelCd;
    private String downVer;
    private String fileName;
    private Long fileSize;
    private String filePath;
    private String downloadPath;
    private String execCmd;
    private String applyDate;      // YYYYMMDD
    private String applyTime;      // HH24MI
    private String crcValue;
    private String confirmStatus;  // N=미결재, Y=결재완료, R=반려
    private String confirmUserId;
    private LocalDateTime confirmDt;
    private String useYn;
    private String regUserId;
    private LocalDateTime regDt;
    private LocalDateTime updDt;

    /** 조회 조인 부가정보(매핑 전용) */
    private String modelName;
}
