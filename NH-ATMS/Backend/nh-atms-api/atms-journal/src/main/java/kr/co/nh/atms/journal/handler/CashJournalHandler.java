package kr.co.nh.atms.journal.handler;

import kr.co.nh.atms.journal.dto.JifHeadKey;
import kr.co.nh.atms.journal.dto.JifMessage;
import kr.co.nh.atms.journal.entity.AddCash;
import kr.co.nh.atms.journal.entity.AddCheck;
import kr.co.nh.atms.journal.entity.CassetteStatus;
import kr.co.nh.atms.journal.mapper.AddCashWriteMapper;
import kr.co.nh.atms.journal.mapper.AddCheckWriteMapper;
import kr.co.nh.atms.journal.mapper.AtmStatusWriteMapper;
import kr.co.nh.atms.journal.mapper.CassetteStatusWriteMapper;
import kr.co.nh.atms.journal.parser.JifFieldUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * jnl_kind=41 (현금/수표 시재) → T_ADD_CASH / T_ADD_CHECK INSERT.
 *
 * body 스펙(interface.journal.json "41"):
 *   cash_tp      : 1=보충, 2=회수
 *   cassettes_log: [{curr, denom, rem_qty, add_qty}, ...]
 *   checks       : [{curr, denom, ja_amt, ja_cnt, ta_amt, ta_cnt, iss_bk, iss_br, reg_no}, ...]
 *
 * 단순 누적 적재만 수행. 마스터(T_ATM_STATUS, T_CASSETTESTATUS) 동기화는 Phase B 에서.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CashJournalHandler implements JournalHandler {

    private final AddCashWriteMapper cashMapper;
    private final AddCheckWriteMapper checkMapper;
    private final AtmStatusWriteMapper atmStatusMapper;
    private final CassetteStatusWriteMapper cassetteStatusMapper;

    @Override
    public Set<String> supportedKinds() {
        return Set.of("41");
    }

    @Override
    public void handle(JifHeadKey key, JifMessage msg) {
        Map<String, Object> body = msg.getBody();
        if (body == null) {
            log.warn("[jnl 41] body is null term={}/{} time={}",
                    key.getTermGroupId(), key.getTermId(), key.getJnlTime());
            return;
        }

        String actionType = JifFieldUtils.s(body, "cash_tp");
        if (actionType == null) actionType = "1";

        // 논리 카세트(cassettes_log) + 물리 카세트(cassettes_phy) 모두 적재 (atmr Cash 2,3단계)
        int cashN = insertCashes(key, actionType, "1", asList(body.get("cassettes_log")));
        cashN += insertCashes(key, actionType, "2", asList(body.get("cassettes_phy")));
        int chkN  = insertChecks(key, actionType, asList(body.get("checks")));

        log.debug("[jnl 41] term={}/{} time={} action={} cash_rows={} check_rows={}",
                key.getTermGroupId(), key.getTermId(), key.getJnlTime(),
                actionType, cashN, chkN);

        // 마스터 동기화: 최종 현금보충일시 (atmr update_T_ATMSTATUS_at_Cash)
        atmStatusMapper.touchAddCash(key.getTermGroupId(), key.getTermId(),
                key.getJnlDate(), key.getJnlTime());
    }

    private int insertCashes(JifHeadKey key, String actionType, String cassetteType,
                             List<Map<String, Object>> rows) {
        int n = 0;
        int no = 0;
        for (Map<String, Object> r : rows) {
            no++;
            AddCash e = AddCash.builder()
                    .termGroupId(key.getTermGroupId())
                    .termId(key.getTermId())
                    .jnlDate(key.getJnlDate())
                    .jnlTime(key.getJnlTime())
                    .actionType(actionType)
                    .cassetteType(cassetteType)
                    .cassetteNo(String.valueOf(no))
                    .currency(JifFieldUtils.s(r, "curr"))
                    .denomination(toLong(r.get("denom")))
                    .remainQty(toInt(r.get("rem_qty")))
                    .addQty(toInt(r.get("add_qty")))
                    .build();
            n += cashMapper.insert(e);

            // 현황 동기화: 카세트 잔여매수 (atmr update_T_CASSETTESTATUS_logical/physical)
            //   논리=순번(no), 물리=물리번호(item.no) 기준.
            String cassetteNo = "2".equals(cassetteType)
                    ? orDefault(JifFieldUtils.s(r, "no"), String.valueOf(no))
                    : String.valueOf(no);
            cassetteStatusMapper.upsert(CassetteStatus.builder()
                    .termGroupId(key.getTermGroupId())
                    .termId(key.getTermId())
                    .cassetteType(cassetteType)
                    .cassetteNo(cassetteNo)
                    .denomCd(JifFieldUtils.s(r, "denom"))
                    .currencyCd(JifFieldUtils.s(r, "curr"))
                    .remainCount(toLong(r.get("rem_qty")))
                    .status("00")
                    .checkNo(null)
                    .build());
        }
        return n;
    }

    private int insertChecks(JifHeadKey key, String actionType, List<Map<String, Object>> rows) {
        // T_ADD_CHECK PK 는 (단말+시간+action_type) 단일. body.checks[] 가 여러 건이어도
        // 첫 건만 적재 (잔여는 별도 분배 키 없음). atmr 와 동일 정책.
        if (rows.isEmpty()) return 0;
        Map<String, Object> r = rows.get(0);
        AddCheck e = AddCheck.builder()
                .termGroupId(key.getTermGroupId())
                .termId(key.getTermId())
                .jnlDate(key.getJnlDate())
                .jnlTime(key.getJnlTime())
                .actionType(actionType)
                .currency(JifFieldUtils.s(r, "curr"))
                .denomination(toLong(r.get("denom")))
                .jaAmount(toLong(r.get("ja_amt")))
                .jaCount(toInt(r.get("ja_cnt")))
                .taAmount(toLong(r.get("ta_amt")))
                .taCount(toInt(r.get("ta_cnt")))
                .issueBank(JifFieldUtils.s(r, "iss_bk"))
                .issueBranch(JifFieldUtils.s(r, "iss_br"))
                .regNo(JifFieldUtils.s(r, "reg_no"))
                .build();
        return checkMapper.insert(e);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> asList(Object o) {
        if (o instanceof List<?> l) {
            return (List<Map<String, Object>>) l;
        }
        return List.of();
    }

    private static String orDefault(String s, String def) {
        return s == null ? def : s;
    }

    private static Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        try { return Long.parseLong(String.valueOf(o).trim()); } catch (Exception e) { return null; }
    }

    private static Integer toInt(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(o).trim()); } catch (Exception e) { return null; }
    }
}
