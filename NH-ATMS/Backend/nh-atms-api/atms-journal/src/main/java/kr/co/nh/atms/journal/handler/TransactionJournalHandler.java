package kr.co.nh.atms.journal.handler;

import kr.co.nh.atms.journal.dto.JifHeadKey;
import kr.co.nh.atms.journal.dto.JifMessage;
import kr.co.nh.atms.journal.entity.CashTransaction;
import kr.co.nh.atms.journal.entity.CassetteStatus;
import kr.co.nh.atms.journal.entity.CheckTransaction;
import kr.co.nh.atms.journal.entity.TransactionJournal;
import kr.co.nh.atms.journal.mapper.AtmStatusWriteMapper;
import kr.co.nh.atms.journal.mapper.CashTransactionWriteMapper;
import kr.co.nh.atms.journal.mapper.CassetteStatusWriteMapper;
import kr.co.nh.atms.journal.mapper.CheckTransactionWriteMapper;
import kr.co.nh.atms.journal.mapper.TransactionJournalWriteMapper;
import kr.co.nh.atms.journal.parser.JifFieldUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * jnl_kind=71 (거래저널) → T_TRANSACTION_JOURNAL INSERT.
 * body 필드 매핑은 09_ATMR §3.3 + 농협 거래저널 본문 표준에 따름.
 * (스펙 미확정 시점이라 누락 필드는 null 로 적재)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionJournalHandler implements JournalHandler {

    private final TransactionJournalWriteMapper mapper;
    private final AtmStatusWriteMapper atmStatusMapper;
    private final CashTransactionWriteMapper cashTrxMapper;
    private final CheckTransactionWriteMapper checkTrxMapper;
    private final CassetteStatusWriteMapper cassetteStatusMapper;

    @Override
    public Set<String> supportedKinds() {
        return Set.of("71");
    }

    @Override
    public void handle(JifHeadKey key, JifMessage msg) {
        Map<String, Object> body = msg.getBody();

        TransactionJournal t = TransactionJournal.builder()
                .termGroupId(key.getTermGroupId())
                .termId(key.getTermId())
                .jnlDate(key.getJnlDate())
                .jnlTime(key.getJnlTime())
                .trNo(JifFieldUtils.s(body, "trx_no"))
                .trType(JifFieldUtils.s(body, "trx_cd"))
                .trFromAccount(JifFieldUtils.s(body, "wth_acc"))
                .trToAccount(JifFieldUtils.s(body, "dep_acc"))
                .bankCd(bank3(JifFieldUtils.s(body, "bk_cd")))
                .accountNo(JifFieldUtils.s(body, "bk_acc"))
                .toBankCd(bank3(JifFieldUtils.s(body, "trx_bk")))
                .toAccountNo(JifFieldUtils.s(body, "trx_acc"))
                .trHostDate(date3(body, "yyyy", "mm", "dd"))
                .trHostTime(time3(body, "hh", "mi", "ss"))
                .trHostNo(JifFieldUtils.s(body, "host_ret_no"))
                .trHostAuditNo(JifFieldUtils.s(body, "host_ait_id"))
                .hostNetId(JifFieldUtils.s(body, "host_net_id"))
                .hostSettleDate(date3(body, "host_stl_yyyy", "host_stl_mm", "host_stl_dd"))
                .depositNo(JifFieldUtils.s(body, "dep_no"))
                .surcharge(num(body, "fee_amt"))
                .requestAmount(num(body, "req_amt"))
                .dispenseAmount(num(body, "trx_amt"))
                .remainAmount(num(body, "rem_amt"))
                .availAmount(num(body, "avail_amt"))
                .hostCashAmount(num(body, "host_cash_amt"))
                .hostCheckAmount(num(body, "host_check_amt"))
                .procCount(JifFieldUtils.s(body, "proc_cnt"))
                .trResult(JifFieldUtils.s(body, "trx_rst"))
                .errCd1(JifFieldUtils.s(body, "err_cd1"))
                .errCd2(JifFieldUtils.s(body, "err_cd2"))
                .errCd3(JifFieldUtils.s(body, "err_cd3"))
                .cardData(JifFieldUtils.s(body, "ms_data"))
                .nonCashValue(JifFieldUtils.s(body, "non_cash_val"))
                .nonCashType(JifFieldUtils.s(body, "non_cash_tp"))
                .trMediaCd(JifFieldUtils.s(body, "media_id"))
                .message(JifFieldUtils.s(body, "host_msg"))
                .totalTypeCd(JifFieldUtils.s(body, "close_status"))
                .transferName(JifFieldUtils.s(body, "cust_nm"))
                .build();

        int n = mapper.insert(t);
        log.debug("[jnl 71] T_TRANSACTION_JOURNAL insert={} term={}/{} time={} trType={} trResult={}",
                n, key.getTermGroupId(), key.getTermId(),
                key.getJnlTime(), t.getTrType(), t.getTrResult());

        // 권종별 현금/수표 상세 적재 (atmr insert_ignore_T_CASH/CHECKTRANSACTION)
        int cashN  = insertCashTrx(key, asList(body.get("trx_cash")));
        int checkN = insertCheckTrx(key, asList(body.get("trx_checks")));
        if (cashN > 0 || checkN > 0) {
            log.debug("[jnl 71] detail cash_rows={} check_rows={} term={}/{} time={}",
                    cashN, checkN, key.getTermGroupId(), key.getTermId(), key.getJnlTime());
        }

        // 현황 동기화: 카세트 잔여매수 (atmr update_T_CASSETTESTATUS_*_at_Transaction)
        syncCassettes(key, asList(body.get("cassettes_log")), "1", false);
        syncCassettes(key, asList(body.get("cassettes_phy")), "2", true);
        syncChecks(key, asList(body.get("atm_checks")));

        // 마스터 동기화: 최종 거래일시 (atmr update_T_ATMSTATUS_at_Transaction)
        atmStatusMapper.touchTransaction(key.getTermGroupId(), key.getTermId(),
                key.getJnlDate(), key.getJnlTime());
    }

    /**
     * cassettes_log(논리)/cassettes_phy(물리) -> T_CASSETTE_STATUS upsert.
     * useRowNo=true 면 항목의 no 필드를 카세트번호로, false 면 배열 순번(1부터)를 사용.
     */
    private void syncCassettes(JifHeadKey key, List<Map<String, Object>> rows,
                               String cassetteType, boolean useRowNo) {
        int seq = 0;
        for (Map<String, Object> r : rows) {
            seq++;
            String no = useRowNo ? orElse(JifFieldUtils.s(r, "no"), String.valueOf(seq))
                                 : String.valueOf(seq);
            cassetteStatusMapper.upsert(CassetteStatus.builder()
                    .termGroupId(key.getTermGroupId())
                    .termId(key.getTermId())
                    .cassetteType(cassetteType)
                    .cassetteNo(no)
                    .denomCd(JifFieldUtils.s(r, "denom"))
                    .currencyCd(JifFieldUtils.s(r, "curr"))
                    .remainCount(toLong(r.get("qty")))
                    .status(JifFieldUtils.s(r, "status"))
                    .checkNo(null)
                    .build());
        }
    }

    /** atm_checks(수표 권종) -> T_CASSETTE_STATUS upsert(수표 카세트 type=4). */
    private void syncChecks(JifHeadKey key, List<Map<String, Object>> rows) {
        int seq = 0;
        for (Map<String, Object> r : rows) {
            seq++;
            cassetteStatusMapper.upsert(CassetteStatus.builder()
                    .termGroupId(key.getTermGroupId())
                    .termId(key.getTermId())
                    .cassetteType("4")
                    .cassetteNo(String.valueOf(seq))
                    .denomCd(JifFieldUtils.s(r, "denom"))
                    .currencyCd(JifFieldUtils.s(r, "curr"))
                    .remainCount(toLong(r.get("qty")))
                    .status(null)
                    .checkNo(trunc(JifFieldUtils.s(r, "bgn_no"), 32))
                    .build());
        }
    }

    private static String orElse(String v, String def) {
        return (v == null || v.isBlank()) ? def : v.trim();
    }

    /** trx_cash[{curr,denom,qty}] -> T_CASH_TRANSACTION. denom 없는 행은 PK 구성 불가로 skip. */
    private int insertCashTrx(JifHeadKey key, List<Map<String, Object>> rows) {
        int n = 0;
        for (Map<String, Object> r : rows) {
            String denom = JifFieldUtils.s(r, "denom");
            if (denom == null || denom.isBlank()) continue;
            CashTransaction e = CashTransaction.builder()
                    .termGroupId(key.getTermGroupId())
                    .termId(key.getTermId())
                    .jnlDate(key.getJnlDate())
                    .jnlTime(key.getJnlTime())
                    .denomCd(denom.trim())
                    .denomCount(toLong(r.get("qty")))
                    .build();
            n += cashTrxMapper.insert(e);
        }
        return n;
    }

    /** trx_checks[{amt,bk_cd,no}] -> T_CHECK_TRANSACTION. 순번(check_seq)=배열 index+1. */
    private int insertCheckTrx(JifHeadKey key, List<Map<String, Object>> rows) {
        int n = 0;
        int seq = 0;
        for (Map<String, Object> r : rows) {
            seq++;
            CheckTransaction e = CheckTransaction.builder()
                    .termGroupId(key.getTermGroupId())
                    .termId(key.getTermId())
                    .jnlDate(key.getJnlDate())
                    .jnlTime(key.getJnlTime())
                    .checkSeq(seq)
                    .checkAmount(trunc(JifFieldUtils.s(r, "amt"), 10))
                    .bankCd(trunc(JifFieldUtils.s(r, "bk_cd"), 3))
                    .checkNo(trunc(JifFieldUtils.s(r, "no"), 32))
                    .build();
            n += checkTrxMapper.insert(e);
        }
        return n;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> asList(Object o) {
        if (o instanceof List<?> l) {
            return (List<Map<String, Object>>) l;
        }
        return List.of();
    }

    /** 권종 매수(qty) -> Long. 숫자 아니면 null. */
    private static Long toLong(Object o) {
        if (o == null) return null;
        String s = o.toString().trim();
        if (s.isEmpty()) return null;
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 컬럼 길이 초과 방지용 우측 절단(공백 trim 후). null -> null. */
    private static String trunc(String v, int max) {
        if (v == null) return null;
        String s = v.trim();
        return s.length() > max ? s.substring(0, max) : s;
    }

    /**
     * 전문 은행코드(bk_cd/trx_bk — JIF 스펙 4자리 lpad0) → BANK_CD 컬럼(3자리) 정규화.
     * "0011" → "011" 처럼 뒤 3자리 사용 (표준 은행코드 3자리 가정 — 스펙-스키마 불일치 검증 항목)
     */
    private static String bank3(String v) {
        if (v == null) return null;
        String s = v.trim();
        return s.length() > 3 ? s.substring(s.length() - 3) : s;
    }

    /** 거래일(yyyy/mm/dd 분리) → yyyyMMdd 조합. 하나라도 없으면 null. */
    private static String date3(Map<String, Object> body, String y, String m, String d) {
        String yy = JifFieldUtils.s(body, y);
        String mm = JifFieldUtils.s(body, m);
        String dd = JifFieldUtils.s(body, d);
        if (yy == null || mm == null || dd == null) return null;
        return yy + mm + dd;
    }

    /** 거래시각(hh/mi/ss 분리) → HHmmss 조합. 하나라도 없으면 null. */
    private static String time3(Map<String, Object> body, String h, String mi, String s) {
        String hh = JifFieldUtils.s(body, h);
        String nn = JifFieldUtils.s(body, mi);
        String ss = JifFieldUtils.s(body, s);
        if (hh == null || nn == null || ss == null) return null;
        return hh + nn + ss;
    }

    private static BigDecimal num(Map<String, Object> body, String key) {
        String v = JifFieldUtils.s(body, key);
        if (v == null) return null;
        try {
            return new BigDecimal(v);
        } catch (NumberFormatException e) {
            log.warn("invalid amount field {}={}", key, v);
            return null;
        }
    }
}
