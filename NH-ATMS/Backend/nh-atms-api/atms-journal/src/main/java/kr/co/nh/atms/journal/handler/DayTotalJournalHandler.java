package kr.co.nh.atms.journal.handler;

import kr.co.nh.atms.journal.dto.JifHeadKey;
import kr.co.nh.atms.journal.dto.JifMessage;
import kr.co.nh.atms.journal.entity.DayTotal;
import kr.co.nh.atms.journal.mapper.AtmStatusWriteMapper;
import kr.co.nh.atms.journal.mapper.DayTotalWriteMapper;
import kr.co.nh.atms.journal.parser.JifFieldUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * jnl_kind=31 (마감저널) -> T_DAY_TOTAL INSERT.
 *
 * interface.journal.json "31" 명세를 충실히 따른다 (마감항목 1건 = 1행).
 *   tot_ty        : 마감종류 (5=소계, 6=합계)             -> ACTION_TYPE
 *   bgn_, end_ 필드 : 마감기간 시작/종료 (월/일/년/시/분/초)  -> BEGIN_, END_ 컬럼
 *   host_entries[]: host 집계항목 [{cd, val}]             -> TOTAL_SRC_CD='1'
 *   atm_entries[] :  atm 집계항목 [{cd, val}]             -> TOTAL_SRC_CD='2'
 *
 * 동일 저널(JNL_TIME) 안에서 host/atm 각 항목마다 한 행을 적재하며,
 * tot_ty 및 마감기간은 모든 행에 공통으로 비정규화 보관한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DayTotalJournalHandler implements JournalHandler {

    /** 집계원천 코드: host=1, atm=2 */
    private static final String SRC_HOST = "1";
    private static final String SRC_ATM  = "2";

    private final DayTotalWriteMapper mapper;
    private final AtmStatusWriteMapper atmStatusMapper;

    @Override
    public Set<String> supportedKinds() {
        return Set.of("31");
    }

    @Override
    public void handle(JifHeadKey key, JifMessage msg) {
        Map<String, Object> body = msg.getBody();
        if (body == null) {
            log.warn("[jnl 31] body is null term={}/{} time={}",
                    key.getTermGroupId(), key.getTermId(), key.getJnlTime());
            return;
        }

        String actionType = JifFieldUtils.s(body, "tot_ty");      // 5=소계, 6=합계
        String beginDate = assembleDate(JifFieldUtils.s(body, "bgn_yyyy"),
                JifFieldUtils.s(body, "bgn_mm"), JifFieldUtils.s(body, "bgn_dd"));
        String beginTime = assembleTime(JifFieldUtils.s(body, "bgn_hh"),
                JifFieldUtils.s(body, "bgn_mi"), JifFieldUtils.s(body, "bgn_ss"));
        String endDate = assembleDate(JifFieldUtils.s(body, "end_yyyy"),
                JifFieldUtils.s(body, "end_mm"), JifFieldUtils.s(body, "end_dd"));
        String endTime = assembleTime(JifFieldUtils.s(body, "end_hh"),
                JifFieldUtils.s(body, "end_mi"), JifFieldUtils.s(body, "end_ss"));

        int hostN = insertEntries(key, SRC_HOST, actionType, beginDate, beginTime, endDate, endTime,
                asList(body.get("host_entries")));
        int atmN = insertEntries(key, SRC_ATM, actionType, beginDate, beginTime, endDate, endTime,
                asList(body.get("atm_entries")));

        log.debug("[jnl 31] T_DAY_TOTAL term={}/{} time={} tot_ty={} host_rows={} atm_rows={}",
                key.getTermGroupId(), key.getTermId(), key.getJnlTime(),
                actionType, hostN, atmN);

        // 마스터 동기화: 최종 일마감일시 (atmr update_T_ATMSTATUS_at_Total)
        atmStatusMapper.touchDayTotal(key.getTermGroupId(), key.getTermId(),
                key.getJnlDate(), key.getJnlTime());
    }

    private int insertEntries(JifHeadKey key, String srcCd, String actionType,
                              String beginDate, String beginTime, String endDate, String endTime,
                              List<Map<String, Object>> entries) {
        int n = 0;
        for (Map<String, Object> e : entries) {
            String cd = JifFieldUtils.s(e, "cd");
            if (cd == null || cd.isEmpty()) continue;   // 항목코드 없으면 PK 구성 불가 → skip
            DayTotal t = DayTotal.builder()
                    .termGroupId(key.getTermGroupId())
                    .termId(key.getTermId())
                    .jnlDate(key.getJnlDate())
                    .jnlTime(key.getJnlTime())
                    .totalSrcCd(srcCd)
                    .totalCd(cd)
                    .totalVal(JifFieldUtils.s(e, "val"))
                    .actionType(actionType)
                    .beginDate(beginDate)
                    .beginTime(beginTime)
                    .endDate(endDate)
                    .endTime(endTime)
                    .build();
            n += mapper.insert(t);
        }
        return n;
    }

    /**
     * 저널의 분리된 날짜필드(yyyy 2자리, mm 1자리, dd 2자리)를 YYYYMMDD(8) 로 조립.
     * 모두 비었으면 null.
     */
    private static String assembleDate(String yy, String mm, String dd) {
        if (isBlank(yy) && isBlank(mm) && isBlank(dd)) return null;
        return "20" + pad(yy, 2) + pad(mm, 2) + pad(dd, 2);
    }

    /**
     * 저널의 분리된 시각필드(hh, mi, ss)를 HHMMSS(6) 로 조립.
     * hh 는 명세상 4자리이므로 우측 2자리를 시(時)로 사용한다.
     */
    private static String assembleTime(String hh, String mi, String ss) {
        if (isBlank(hh) && isBlank(mi) && isBlank(ss)) return null;
        return pad(right2(hh), 2) + pad(mi, 2) + pad(ss, 2);
    }

    /** 좌측 0-padding 후 길이 len 으로 맞춤(초과 시 우측 len 자리). */
    private static String pad(String s, int len) {
        String v = (s == null) ? "" : s.trim();
        if (v.length() == len) return v;
        if (v.length() > len) return v.substring(v.length() - len);
        return "0".repeat(len - v.length()) + v;
    }

    private static String right2(String s) {
        return (s == null || s.length() <= 2) ? s : s.substring(s.length() - 2);
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> asList(Object o) {
        if (o instanceof List<?> l) {
            return (List<Map<String, Object>>) l;
        }
        return List.of();
    }
}
