package kr.co.nh.atms.web.download.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 개별다운로드 업로드 배포 요청 (구 MG apDownLoadProc 정합).
 *
 * <p>운영자가 업로드한 ZIP 파일(multipart {@code file})과 함께 전송되는 메타정보.
 * MG 화면선택(수동) 방식과 동일하게, 선택 기기({@code targets})는 기종 제한 없이
 * 모두 배포 대상이 된다. {@code atmCoCd}/{@code modelCd}는 마스터에 기록되는
 * 대표 기종(선택)일 뿐, 일치 검증에는 사용하지 않는다.</p>
 */
@Getter
@Setter
public class IndividualUploadRequest {

    /** 패키지유형 : A=AP / G=일반 / M=매체. */
    @NotBlank(message = "패키지 유형은 필수입니다.")
    private String appType;

    /** 대표 기기업체(선택, 마스터 기록용). */
    private String atmCoCd;

    /** 대표 모델(선택, 마스터 기록용). */
    private String modelCd;

    @NotBlank(message = "다운로드 버전은 필수입니다.")
    private String appVer;

    /** 실행구분 : 0=개별 / 1=일반. */
    private String execType;

    /** 기기내 다운로드 경로(미지정 시 기본값 D:\\LGDOWN). */
    private String downloadPath;

    /** 적용내역(메모). */
    private String applyMemo;

    @NotEmpty(message = "대상 기기를 1대 이상 선택하세요.")
    private List<Target> targets;

    @Getter
    @Setter
    public static class Target {
        private String termGroupId;
        private String termId;
    }
}
