package kr.co.nh.atms.web.download.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * T_ATM_DOWNLOAD — 개별다운로드 업로드 파일 마스터 (구 MG T_ATMDOWNLOAD).
 *
 * <p>운영자가 개별다운로드관리 화면에서 직접 업로드한 AP/일반 ZIP 파일의 메타정보.
 * 일괄다운로드(T_DOWNLOAD_PACKAGE, 결재 필수)와 분리된 전용 마스터이며 결재 개념이 없다.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AtmDownload {
    private String downNo;          // 다운로드일련번호(PK, 7자리 0패딩)
    private String appTypeCd;       // 패키지유형 A=AP/G=일반/M=매체
    private String atmCoCd;         // 기기업체코드
    private String modelCd;         // 모델코드
    private String appFileName;     // 업로드 원본 파일명
    private Long appFileSize;       // 파일사이즈(byte)
    private String appVer;          // 파일(다운로드) 버전
    private String fileCompress;    // 압축여부(0:비압축)
    private String exeTypeCd;       // 실행구분 0=개별/1=일반
    private String exeFilePath;     // 서버 보관 절대경로
    private String downloadPath;    // 기기내 다운로드 경로
    private String svrUploadDate;   // 서버 업로드일자(YYYYMMDD)
    private String svrUploadTime;   // 서버 업로드시간(HH24MISS)
    private String applyCause;      // 적용원인
    private String applyMemo;       // 적용내역
    private String crcValue;        // CRC값
    private String enterDate;       // 등록일자(YYYYMMDD)
    private String enterTime;       // 등록시간(HH24MISS)
    private String enterUserId;     // 등록자ID

    // 조회 JOIN 필드
    private String modelName;       // T_ATM_MODEL.MODEL_NAME
    private LocalDateTime regDt;    // 조회 편의용(미사용 가능)
}
