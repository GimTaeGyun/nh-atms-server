package kr.co.nh.atms.web.download.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * T_ATM_APPLY_HISTORY — 기기별 개별다운로드 적용이력 (구 MG T_ATMAPPLYHISTORY).
 *
 * <p>개별다운로드 1건(DOWN_NO)을 선택 기기들에 배포할 때 기기별로 1행이 적재된다.
 * APPLY_FLAG(0:미적용/1:적용완료/2:다운로드중), APPLY_RESULT(0:실패/1:성공),
 * ASYNC_UID(비동기 상관 ID)로 진행 상태를 추적한다.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AtmApplyHistory {
    private String downNo;          // 다운로드일련번호(PK)
    private String termGroupId;     // 영업점(금고)코드(PK)
    private String termId;          // 단말번호(기번)(PK)
    private String cmdId;           // 원격명령 ID(T_AGENT_COMMAND)
    private String startDate;       // 시작일자(YYYYMMDD)
    private String startTime;       // 시작시간(HH24MISS)
    private String endDate;         // 종료일자
    private String endTime;         // 종료시간
    private String applyFlag;       // 적용여부 0=미적용/1=적용완료/2=다운로드중
    private String applyResult;     // 적용결과 0=실패/1=성공
    private Long asyncUid;          // 비동기 상관 ID(T_MSG_ASYNC)
    private String regUserId;       // 등록자ID
    private LocalDateTime regDt;    // 등록일시

    // 조회 JOIN 필드(개별다운로드 이력 화면용)
    private String appTypeCd;       // T_ATM_DOWNLOAD.APP_TYPE_CD
    private String appFileName;     // T_ATM_DOWNLOAD.APP_FILE_NAME
    private String appVer;          // T_ATM_DOWNLOAD.APP_VER
    private String atmCoCd;         // T_ATM_DOWNLOAD.ATM_CO_CD
    private String modelCd;         // T_ATM_DOWNLOAD.MODEL_CD
}
