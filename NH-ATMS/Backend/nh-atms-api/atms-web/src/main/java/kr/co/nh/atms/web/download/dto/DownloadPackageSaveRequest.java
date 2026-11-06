package kr.co.nh.atms.web.download.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/** 다운로드 패키지 등록 요청. */
@Getter
@Setter
public class DownloadPackageSaveRequest {

    private String pkgName;

    @NotBlank(message = "유형은 필수입니다.")
    @Pattern(regexp = "A|G|M", message = "유형은 A(AP)/G(일반)/M(매체) 중 하나여야 합니다.")
    private String pkgType;

    @NotBlank(message = "기기업체는 필수입니다.")
    private String atmCoCd;

    @NotBlank(message = "모델은 필수입니다.")
    private String modelCd;

    @NotBlank(message = "다운로드버전은 필수입니다.")
    private String downVer;

    @NotBlank(message = "파일명은 필수입니다.")
    private String fileName;

    private Long fileSize;
    private String filePath;
    private String downloadPath;
    private String execCmd;
    private String applyDate;   // YYYYMMDD
    private String applyTime;   // HH24MI
    private String crcValue;
}
