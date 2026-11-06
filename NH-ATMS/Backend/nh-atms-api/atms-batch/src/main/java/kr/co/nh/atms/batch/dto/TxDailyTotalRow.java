package kr.co.nh.atms.batch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * batday02 Chunk Step 아이템 — 거래실적 집계 1행 (T_TRANSACTION_DAILY_TOTAL).
 *
 * AS-IS atmb t_transactiondailytotal 의 논리 차원을 유지한다:
 *   거래코드(trCd) / 자행·타행(trBankType) / 마감구분(closingType) / 요일(dayWeek)
 *   + 성공 거래만 집계(TR_RESULT='0000' ↔ AS-IS 'OK'), 건수·금액·수수료 합계.
 * AS-IS 의 24시간 pivot 컬럼(tr_cnt_00_07 …)은 statHour 행 구조로 정규화(정보량 동일).
 * AS-IS tr_type_cd(거래종류) 차원은 T_TRANSACTION_CODE 미이관으로 제외 — 필요 시
 * trCd 를 코드매핑하여 상향집계 가능 (MIGRATION-NOTES.md 참조).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TxDailyTotalRow {
    private String statDate;        // 거래일자 yyyyMMdd (AS-IS tr_date)
    private String termGroupId;     // 영업점코드
    private String branchCd;        // 지점코드 (T_ATM.BRANCH_CD)
    private String termId;          // 단말번호
    private String statHour;        // 시간대 (00~23)
    private String trCd;            // 거래코드 (T_TRANSACTION_JOURNAL.TR_TYPE)
    private String trBankType;      // 자행/타행 (1:자행 2:타행)
    private String closingType;     // 마감구분 (TOTAL_TYPE_CD)
    private String dayWeek;         // 요일 (MON~SUN)
    private Long trCnt;             // 성공 거래건수
    private Long trAmt;             // 거래금액 합 (DISPENSE_AMOUNT, AS-IS tr_amount)
    private Long surchargeAmt;      // 수수료 합
}
