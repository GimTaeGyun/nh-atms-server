package kr.co.nh.atms.ifex.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 코너 변경 통보 (cornerChange.do / SM_02001160).
 */
@Data
public class CornerChangeRequest {

    @NotBlank(message = "termGroupId(영업점코드)는 필수입니다.")
    private String termGroupId;

    @NotBlank(message = "termId(단말번호)는 필수입니다.")
    private String termId;

    /** 코너유형(IN=점내, OUT=점외 등) */
    private String cornerType;
    private String changeDesc;

    /** 코너코드 (레거시 호출 corner_cd) — 미지정 시 단말 기준 조회 불가하므로 필수 권장 */
    private String cornerCd;
    /** 변경구분 (1:신규 2:변경 3:폐쇄) */
    private String changeType;
    /** 변경전 코너코드 (변경구분=2 인 경우만) */
    private String beforeCornerCd;
    /** 변경전 코너명 */
    private String beforeCornerName;
    /** 변경전 장소구분코드 (1:점내 2:점두 3:점외 Z:기타) */
    private String beforeCornerTypeCd;
    /** 변경전 운영시간구분코드 (1:영업시간 2:365 3:24/365 Z:기타) */
    private String beforeOperationTypeCd;
    /** 변경전 설치주소 */
    private String beforeAddress;
    /** 변경전 관리점코드(본지점코드) */
    private String beforeBranchCd;
}
