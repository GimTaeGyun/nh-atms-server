package kr.co.nh.atms.journal.handler;

import kr.co.nh.atms.journal.dto.JifHeadKey;
import kr.co.nh.atms.journal.dto.JifMessage;
import kr.co.nh.atms.journal.entity.AtmMonitor;
import kr.co.nh.atms.journal.entity.AtmStopHistory;
import kr.co.nh.atms.journal.entity.AtmUnitStatus;
import kr.co.nh.atms.journal.entity.CassetteStatus;
import kr.co.nh.atms.journal.entity.FaultAnalysis;
import kr.co.nh.atms.journal.entity.FaultMap;
import kr.co.nh.atms.journal.mapper.AtmDeviceStatusWriteMapper;
import kr.co.nh.atms.journal.mapper.AtmMonitorWriteMapper;
import kr.co.nh.atms.journal.mapper.AtmStatusWriteMapper;
import kr.co.nh.atms.journal.mapper.AtmStopHistoryWriteMapper;
import kr.co.nh.atms.journal.mapper.AtmUnitStatusWriteMapper;
import kr.co.nh.atms.journal.mapper.CassetteStatusWriteMapper;
import kr.co.nh.atms.journal.mapper.FaultAnalysisWriteMapper;
import kr.co.nh.atms.journal.mapper.FaultMapMapper;
import kr.co.nh.atms.journal.parser.JifFieldUtils;
import kr.co.nh.atms.journal.service.IfQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * jnl_kind 21(상태) / 22(장애) → T_ATM_MONITOR INSERT.
 * 매핑 근거: 09_ATMR_저널프로토콜_분석.md §3.3
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AtmMonitorJournalHandler implements JournalHandler {

    private static final DateTimeFormatter D = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter T = DateTimeFormatter.ofPattern("HHmmssSSS");

    private final AtmMonitorWriteMapper mapper;
    private final AtmUnitStatusWriteMapper unitStatusMapper;
    private final AtmStatusWriteMapper atmStatusMapper;
    private final AtmDeviceStatusWriteMapper deviceStatusMapper;
    private final CassetteStatusWriteMapper cassetteStatusMapper;
    private final FaultMapMapper faultMapMapper;
    private final FaultAnalysisWriteMapper faultAnalysisMapper;
    private final AtmStopHistoryWriteMapper stopHistoryMapper;
    private final IfQueueService ifQueueService;

    /** 디바이스 상태 정상 코드(1자리 폼). 1=정상 (구 atmr / 시뮬레이터 규약). */
    private static final String DEVICE_NORMAL_ST = "1";

    /** 카세트 구분 (atmr ATMRConstants). 1=논리, 2=물리, 4=수표(BCA). */
    private static final String LOGICAL_CASSETTE  = "1";
    private static final String PHYSICAL_CASSETTE = "2";
    private static final String CHECK_CASSETTE    = "4";

    /** 장애분석 상수 (atmr ATMRConstants). */
    private static final String LANG_CD          = "KOR";
    private static final String IN_SERVICE       = "00";
    private static final String OUT_OF_SERVICE   = "02";
    private static final String APP_SERVICE_OPEN = "05";
    private static final String ATM_ETC_ERROR    = "99";
    private static final String FAULT_MACHINE    = "1";  // 기기장애
    private static final String FAULT_OPERATION  = "2";  // 운영장애

    @Override
    public Set<String> supportedKinds() {
        return Set.of("21", "22");
    }

    @Override
    public void handle(JifHeadKey key, JifMessage msg) {
        Map<String, Object> body = msg.getBody();
        Map<String, Object> head = msg.getHead();

        LocalDateTime now = LocalDateTime.now();
        // T_ATM_MONITOR.AGENT_TYPE 은 NOT NULL → 저널 head 에 없으면 기본값 '1'(ATM)
        String agentType = JifFieldUtils.s(head, "agent_type");
        if (agentType == null || agentType.isBlank()) {
            agentType = "1";
        }
        AtmMonitor m = AtmMonitor.builder()
                .termGroupId(key.getTermGroupId())
                .termId(key.getTermId())
                .jnlDate(key.getJnlDate())
                .jnlTime(key.getJnlTime())
                .agentType(agentType)
                .atmStatus(JifFieldUtils.s(body, "atm_st"))
                .errCd(JifFieldUtils.s(body, "bk_ecd"))
                .atmErrCd(JifFieldUtils.s(body, "vd_ecd"))
                .processCount(trunc1(JifFieldUtils.s(body, "pc_cnt")))
                .manStatus(deviceStatus(body, "MAN"))
                .omanStatus(deviceStatus(body, "OMA"))
                .sprStatus(deviceStatus(body, "SPR"))
                .jprStatus(deviceStatus(body, "JPR"))
                .chkStatus(deviceStatus(body, "CHK"))
                .chaStatus(deviceStatus(body, "CHA"))
                .mcrStatus(deviceStatus(body, "MCR"))
                .pbStatus(deviceStatus(body, "PB"))
                .rjtStatus(deviceStatus(body, "RJT"))
                .sendFlag("Y")
                .recvDate(now.format(D))
                .recvTime(now.format(T) + "0")
                .build();

        // T_ATM_MONITOR 적재 가드: AS-IS(StatusFailure 9단계)는 atm_st 가
        // 00(사용가능)/02(중지)/05(서비스오픈)일 때만 INSERT — 그 외 상태는 스킵.
        String atmSt = m.getAtmStatus() == null ? null : m.getAtmStatus().trim();
        if (IN_SERVICE.equals(atmSt) || OUT_OF_SERVICE.equals(atmSt) || APP_SERVICE_OPEN.equals(atmSt)) {
            int n = mapper.insert(m);
            log.debug("[jnl {}] T_ATM_MONITOR insert={} term={}/{} time={} errCd={}",
                    key.getJnlKind(), n, key.getTermGroupId(), key.getTermId(),
                    key.getJnlTime(), m.getErrCd());
        } else {
            log.debug("[jnl {}] T_ATM_MONITOR skip(atm_st={}) term={}/{}",
                    key.getJnlKind(), atmSt, key.getTermGroupId(), key.getTermId());
        }

        // 디바이스 단위 상태/버전 현황 갱신 (atmr 1-B). T_ATM_UNIT_STATUS upsert.
        upsertUnitStatuses(key, body, m.getAgentType());

        // 장애분석 (atmr StatusFailureProcessor): devices[] → T_FAULT_MAP 대조
        //   → T_FAULT_ANALYSIS 적재 + 기기/운영장애 여부 집계
        //   + 모니터링 표시대상(display_flag=1) 디바이스 상태(monitor_flag) 수집.
        FaultSummary fs = analyzeFaults(key, body, m);

        // 거래모드 상태정보 갱신 (atmr update_T_ATMDEVICESTATUS) — D
        updateDeviceTxMode(key, body);

        // 카세트 현황 전건 삭제 후 재생성 (atmr delete + insert_T_CASSETTESTATUS) — E
        //   동시에 만/오만원 권종별 상태 수집.
        Map<String, String> manOman = recreateCassettes(key, body);

        // 만/오만원 부족 운영장애 (atmr insert_T_FAULTANALYSIS_at_ManOman) — F
        //   부족 시 fs.opFault/lastErrGroup 에 반영되어 아래 syncAtmStatus 한 번으로
        //   AS-IS update_T_ATMSTATUS + update_T_ATMSTATUS_at_ManOman 를 모두 커버한다.
        analyzeManOmanShortage(key, m, fs, manOman);

        // 마스터 동기화: 서비스상태/장애여부/에러그룹/에러코드/세션상태/디바이스별상태/장애발생일시
        // (atmr update_T_ATMSTATUS + update_T_ATMSTATUS_at_ManOman).
        syncAtmStatus(key, m.getAtmStatus(), m.getErrCd(), fs, manOman);

        // 운영중지/복구 이력 (atmr insert/update_T_ATMSTOPHISTORY).
        syncStopHistory(key, m.getAtmStatus(), fs);

        // jnl_kind 22(장애) 이고 실제 장애코드면 대내/대외 IF 큐 적재 (atmr 1-D).
        if ("22".equals(key.getJnlKind())) {
            ifQueueService.enqueueOnFault(key, m.getErrCd());
        }
    }

    /**
     * 21/22 상태·장애 → T_ATM_STATUS 갱신 (atmr update_T_ATMSTATUS 정합).
     *   - 서비스상태(atm_st)는 값이 있을 때만 갱신, 에러코드(bk_ecd)는 매 저널 덮어쓰기(복구 시 클리어).
     *   - 기기/운영장애 여부: T_FAULT_MAP 대조 결과. AS-IS 와 동일하게 '1'/'0'.
     *   - 에러그룹코드: 마지막 장애모듈의 err_group_cd (만/오만 부족 시 73/81/82 로 덮어씀).
     *   - ATM 세션상태: 00(사용가능)→'0', 02(중지)→'1', 그 외 null.
     *   - 디바이스별 상태: CDU(7)/CHK(9)/MCR(6)/SPR(1)/JPR(2)=T_FAULT_MAP monitor_flag,
     *                     만/오만=논리카세트 stat 2번째 자리.
     *   - 장애발생일시: 기기장애 또는 운영장애(만/오만 부족 포함) 시 저널 키(jnl_date/time).
     *     → jnl_kind=21 이어도 만/오만 부족이면 기록 (AS-IS update_T_ATMSTATUS_at_ManOman 정합).
     */
    private void syncAtmStatus(JifHeadKey key, String atmStatus, String errCd,
                               FaultSummary fs, Map<String, String> manOman) {
        boolean fault = fs.machineFault || fs.opFault;
        String faDate = fault ? key.getJnlDate() : null;
        String faTime = fault ? key.getJnlTime() : null;
        String svc = (atmStatus == null || atmStatus.isBlank()) ? null : atmStatus.trim();
        String atmFault = fs.machineFault ? "1" : "0";
        String opFault  = fs.opFault ? "1" : "0";
        String session  = IN_SERVICE.equals(svc) ? "0"
                        : OUT_OF_SERVICE.equals(svc) ? "1" : null;
        atmStatusMapper.updateMonitorStatus(key.getTermGroupId(), key.getTermId(),
                svc, atmFault, opFault,
                fs.lastErrGroup, errCd, session,
                fs.deviceStatus.get("7"),          // CDU (현금부)
                manOman.get("10000"),              // 만원권
                manOman.get("50000"),              // 오만원권
                fs.deviceStatus.get("9"),          // CHK (수표출금부)
                fs.deviceStatus.get("6"),          // MCR (카드부)
                fs.deviceStatus.get("1"),          // SPR (명세표부)
                fs.deviceStatus.get("2"),          // JPR (저널부)
                faDate, faTime);
    }

    /**
     * devices[] 의 각 장치 상태 8바이트를 모듈별로 풀어 T_FAULT_MAP 과 대조한다.
     * 각 바이트 위치 i(0~7) = 모듈구분코드(MODULE_TYPE_CD = "01"~"08"),
     * 값 = 상태코드(STATUS_CD). FAULT_FLAG 가 1(기기)/2(운영)면 장애로
     * 판정하여 T_FAULT_ANALYSIS 를 적재한다.
     * (atmr select_T_ATMUNIT JOIN T_FAULTMAP + insert_T_FAULTANALYSIS 정합)
     *
     * 참고: T_FAULT_MAP 기준정보가 비어있으면 조회 결과가 없어 장애판정이 동작하지
     * 않는다. 운영 전 T_FAULT_MAP 적재가 필수.
     */
    @SuppressWarnings("unchecked")
    private FaultSummary analyzeFaults(JifHeadKey key, Map<String, Object> body, AtmMonitor m) {
        FaultSummary fs = new FaultSummary();
        if (body == null) return fs;
        Object devices = body.get("devices");
        if (!(devices instanceof Iterable<?> it)) return fs;

        for (Object o : it) {
            if (!(o instanceof Map<?, ?> row)) continue;
            String deviceId = row.get("id") == null ? null : String.valueOf(row.get("id")).trim();
            String status   = row.get("status") == null ? null : String.valueOf(row.get("status")).trim();
            if (deviceId == null || deviceId.isEmpty() || status == null || status.isEmpty()) continue;
            if (deviceId.length() > 1) deviceId = deviceId.substring(0, 1);

            int len = Math.min(status.length(), 8);
            for (int i = 0; i < len; i++) {
                String moduleTypeCd = String.format("%02d", i + 1);
                String statusCd = String.valueOf(status.charAt(i));
                FaultMap fm = faultMapMapper.select(LANG_CD, deviceId, moduleTypeCd, statusCd);
                if (fm == null) continue;

                // 모니터링 표시대상 디바이스 상태 수집 (atmr display_flag=1 → deviceStatusMap).
                // 장애 여부와 무관하게 수집하며, 뒤 모듈이 앞 값을 덮어쓴다(AS-IS 동일).
                if ("1".equals(trimOrNull(fm.getDisplayFlag()))) {
                    fs.deviceStatus.put(deviceId, fm.getMonitorFlag());
                }

                if (fm.getFaultFlag() == null) continue;
                String flag = fm.getFaultFlag().trim();
                if (!FAULT_MACHINE.equals(flag) && !FAULT_OPERATION.equals(flag)) continue;

                FaultAnalysis fa = FaultAnalysis.builder()
                        .termGroupId(key.getTermGroupId())
                        .termId(key.getTermId())
                        .jnlDate(key.getJnlDate())
                        .jnlTime(key.getJnlTime())
                        .deviceId(deviceId)
                        .errGroupCd(fm.getErrGroupCd())
                        .moduleTypeCd(moduleTypeCd)
                        .faultClassCd(fm.getFaultClassCd())
                        .errCd(m.getErrCd())
                        .atmCoCd(null)
                        .modelCd(null)
                        .status("0")
                        .autoRequest(fm.getAutoRequest())
                        .requestFlag("N")
                        .build();
                faultAnalysisMapper.insert(fa);

                if (FAULT_MACHINE.equals(flag)) {
                    fs.machineFault = true;
                } else {
                    fs.opFault = true;
                }
                if (fs.firstErrGroup == null) {
                    fs.firstErrGroup = fm.getErrGroupCd();
                    fs.firstDeviceId = deviceId;
                }
                // AS-IS(StatusFailure)의 errorGroupCd 는 마지막 장애모듈 값으로 덮어씀 → T_ATM_STATUS.ERR_GROUP_CD
                fs.lastErrGroup = fm.getErrGroupCd();
            }
        }
        if (fs.machineFault || fs.opFault) {
            log.debug("[jnl {}] 장애분석 term={}/{} machine={} op={} errGroup={}",
                    key.getJnlKind(), key.getTermGroupId(), key.getTermId(),
                    fs.machineFault, fs.opFault, fs.lastErrGroup);
        }
        return fs;
    }

    private static String trimOrNull(String s) {
        return s == null ? null : s.trim();
    }

    /**
     * 운영중지/복구 이력 동기화.
     *   - 정상운영(IN_SERVICE) → 미완료 중지건 복구 + 장애이력 조치완료.
     *   - 운영중지(OUT_OF_SERVICE) → 중지장애 적재(장애그룹 미산출 시 기타 '99').
     */
    private void syncStopHistory(JifHeadKey key, String atmStatus, FaultSummary fs) {
        String st = atmStatus == null ? null : atmStatus.trim();
        if (IN_SERVICE.equals(st)) {
            stopHistoryMapper.recover(key.getTermGroupId(), key.getTermId(),
                    key.getJnlDate(), shortTime(key.getJnlTime()));
            faultAnalysisMapper.relieve(key.getTermGroupId(), key.getTermId(),
                    key.getJnlDate(), shortTime(key.getJnlTime()));
        } else if (OUT_OF_SERVICE.equals(st)) {
            String errGroup = fs.firstErrGroup != null ? fs.firstErrGroup : ATM_ETC_ERROR;
            AtmStopHistory stop = AtmStopHistory.builder()
                    .termGroupId(key.getTermGroupId())
                    .termId(key.getTermId())
                    .jnlDate(key.getJnlDate())
                    .jnlTime(key.getJnlTime())
                    .errGroupCd(errGroup)
                    .deviceId(fs.firstDeviceId)
                    .faultStatus(OUT_OF_SERVICE)
                    .exceptFlag("N")
                    .build();
            stopHistoryMapper.insertStop(stop);
        }
    }

    /** CHAR(6) FINISHED_TIME 용 — jnl_time(최대 10자리)을 6자리로 절삭. */
    private static String shortTime(String t) {
        if (t == null) return null;
        return t.length() > 6 ? t.substring(0, 6) : t;
    }

    /**
     * D. 거래모드 상태정보 갱신 (atmr update_T_ATMDEVICESTATUS).
     * body 의 입·출금 가능여부 필드 8개를 T_ATM_DEVICE_STATUS 에 반영한다.
     * 개국(12) 전이면 row 가 없어 0건 갱신(무시).
     */
    private void updateDeviceTxMode(JifHeadKey key, Map<String, Object> body) {
        if (body == null) return;
        int n = deviceStatusMapper.updateTxMode(
                key.getTermGroupId(), key.getTermId(),
                JifFieldUtils.s(body, "cash_wth"),
                JifFieldUtils.s(body, "cash_dep"),
                JifFieldUtils.s(body, "50k_cash_wth"),
                JifFieldUtils.s(body, "50k_cash_dep"),
                JifFieldUtils.s(body, "check_wth"),
                JifFieldUtils.s(body, "check_dep"),
                JifFieldUtils.s(body, "pb_tx"),
                JifFieldUtils.s(body, "1k_5k_dep"));
        if (n == 0) {
            log.debug("[jnl {}] T_ATM_DEVICE_STATUS 거래모드 갱신 대상없음 term={}/{}",
                    key.getJnlKind(), key.getTermGroupId(), key.getTermId());
        }
    }

    /**
     * E. 카세트 현황 전건 삭제 후 논리/물리/수표 카세트를 재생성한다.
     * (atmr delete_T_CASSETTESTATUS → insert_T_CASSETTESTATUS ×3)
     * 리턴: 논리카세트의 권종(denom) → 상태(stat 2번째 자리) 맵 (만/오만 부족 판정용).
     */
    private Map<String, String> recreateCassettes(JifHeadKey key, Map<String, Object> body) {
        Map<String, String> manOman = new HashMap<>();
        if (body == null) return manOman;

        // 7-1. 단말 카세트 현황 전건 삭제
        cassetteStatusMapper.deleteByTerm(key.getTermGroupId(), key.getTermId());

        // 7-2. 논리카세트 재생성 + 권종별 상태 수집
        int no = 0;
        for (Map<String, Object> cst : asList(body.get("cassettes_log"))) {
            no++;
            String denom = JifFieldUtils.s(cst, "denom");
            String stat  = JifFieldUtils.s(cst, "stat");
            upsertCassette(key, LOGICAL_CASSETTE, String.valueOf(no),
                    denom, JifFieldUtils.s(cst, "curr"), cst.get("qty"), stat, null);
            if (denom != null && stat != null && stat.length() >= 2) {
                manOman.put(denom, stat.substring(1, 2));
            }
        }

        // 7-3. 물리카세트 재생성 (CASSETTE_NO = 물리번호 no)
        for (Map<String, Object> cst : asList(body.get("cassettes_phy"))) {
            upsertCassette(key, PHYSICAL_CASSETTE, JifFieldUtils.s(cst, "no"),
                    JifFieldUtils.s(cst, "denom"), JifFieldUtils.s(cst, "curr"),
                    cst.get("qty"), JifFieldUtils.s(cst, "stat"), null);
        }

        // 7-4. 수표카세트 재생성
        int chkNo = 0;
        for (Map<String, Object> cst : asList(body.get("checks"))) {
            chkNo++;
            upsertCassette(key, CHECK_CASSETTE, String.valueOf(chkNo),
                    JifFieldUtils.s(cst, "denom"), JifFieldUtils.s(cst, "curr"),
                    cst.get("qty"), JifFieldUtils.s(cst, "stat"), JifFieldUtils.s(cst, "bgn_no"));
        }
        return manOman;
    }

    private void upsertCassette(JifHeadKey key, String type, String no,
                                String denom, String curr, Object qty,
                                String stat, String checkNo) {
        if (no == null || no.isEmpty()) return;
        cassetteStatusMapper.upsert(CassetteStatus.builder()
                .termGroupId(key.getTermGroupId())
                .termId(key.getTermId())
                .cassetteType(type)
                .cassetteNo(no)
                .denomCd(denom)
                .currencyCd(curr)
                .remainCount(toLong(qty))
                .status(stat)
                .checkNo(checkNo)
                .build());
    }

    /**
     * F. 만/오만원 부족 운영장애 생성 (atmr insert_not_exists_T_FAULTANALYSIS_at_ManOman).
     * 논리카세트 상태(stat 2번째 자리)가 '2'면 해당 권종 부족으로 판정.
     *   만원(10000)만 부족 → 73, 오만원(50000)만 부족 → 81, 둘 다 부족 → 82.
     * T_FAULT_MAP(device_id=7, module_type_cd=7, status_cd=2)에서 분류/자동출동 취득.
     * 운영장애로 집계(fs.opFault=true)하여 syncAtmStatus 에 반영된다.
     */
    private void analyzeManOmanShortage(JifHeadKey key, AtmMonitor m,
                                        FaultSummary fs, Map<String, String> manOman) {
        boolean manShort  = "2".equals(manOman.get("10000"));
        boolean omanShort = "2".equals(manOman.get("50000"));
        if (!manShort && !omanShort) return;

        String errGroupCd = (manShort && omanShort) ? "82" : (manShort ? "73" : "81");
        FaultMap fm = faultMapMapper.select(LANG_CD, "7", "7", "2");
        String faultClassCd = fm != null ? fm.getFaultClassCd() : null;
        String autoRequest  = fm != null ? fm.getAutoRequest()  : null;

        FaultAnalysis fa = FaultAnalysis.builder()
                .termGroupId(key.getTermGroupId())
                .termId(key.getTermId())
                .jnlDate(key.getJnlDate())
                .jnlTime(key.getJnlTime())
                .deviceId("7")
                .errGroupCd(errGroupCd)
                .moduleTypeCd("7")
                .faultClassCd(faultClassCd)
                .errCd(m.getErrCd())
                .atmCoCd(null)
                .modelCd(null)
                .status("0")
                .autoRequest(autoRequest)
                .requestFlag("N")
                .build();
        faultAnalysisMapper.insert(fa);

        fs.opFault = true;          // 운영장애로 집계 (atmr update_T_ATMSTATUS_at_ManOman 의 운영장애여부=1 정합)
        fs.lastErrGroup = errGroupCd; // ERR_GROUP_CD 를 73/81/82 로 덮어씀 + 장애발생일시 기록 트리거(21 저널 포함)
        log.debug("[jnl {}] 만/오만 부족 운영장애 term={}/{} man={} oman={} errGroup={}",
                key.getJnlKind(), key.getTermGroupId(), key.getTermId(),
                manShort, omanShort, errGroupCd);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> asList(Object o) {
        if (o instanceof List<?> l) {
            return (List<Map<String, Object>>) l;
        }
        return List.of();
    }

    private static Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number num) return num.longValue();
        try { return Long.parseLong(String.valueOf(o).trim()); } catch (Exception e) { return null; }
    }

    /**
     * 장애분석 요약.
     *   - machineFault/opFault : 기기/운영장애 여부 집계
     *   - firstErrGroup/firstDeviceId : 최초 장애그룹/장치 (운영중지 이력용)
     *   - lastErrGroup : 마지막 장애그룹 (T_ATM_STATUS.ERR_GROUP_CD — AS-IS errorGroupCd 덮어쓰기 정합)
     *   - deviceStatus : 표시대상(display_flag=1) 디바이스ID → monitor_flag (T_ATM_STATUS 디바이스별 상태용)
     */
    private static final class FaultSummary {
        private boolean machineFault;
        private boolean opFault;
        private String firstErrGroup;
        private String firstDeviceId;
        private String lastErrGroup;
        private final Map<String, String> deviceStatus = new HashMap<>();
    }

    /**
     * body 의 디바이스 상태를 T_ATM_UNIT_STATUS 로 upsert.
     * 두 가지 입력 포맷을 모두 수용:
     *   1) deviceStatus : { "7":"31000000", "9":"..." }  (key=장치ID, value=상태문자열)  ← C# 시뮬/agent status
     *   2) devices      : [ {"id":"MAN","status":"10000000"}, ... ] (id=장치명, status=8자리 상태) ← JIF 저널 body
     * FAULT_FLAG 는 디바이스 단위 장애 판정 표준이 미확정이라 1차 규약:
     *   - devices[] : status 첫 바이트가 정상코드(1)면 'N', 아니면 'Y'
     *   - deviceStatus 맵 : 1자리 상태값을 동일 규약 적용(정상=1 → 'N'), 그 외 'Y'
     */
    @SuppressWarnings("unchecked")
    private void upsertUnitStatuses(JifHeadKey key, Map<String, Object> body, String agentType) {
        if (body == null) return;
        String at = (agentType == null || agentType.isBlank()) ? "A" : agentType.trim().substring(0, 1);

        Object deviceStatus = body.get("deviceStatus");
        Object devices = body.get("devices");

        if (deviceStatus instanceof Map<?, ?> dsMap) {
            for (Map.Entry<?, ?> en : dsMap.entrySet()) {
                String id = en.getKey() == null ? null : String.valueOf(en.getKey()).trim();
                String st = en.getValue() == null ? null : String.valueOf(en.getValue()).trim();
                upsertOne(key, at, id, st);
            }
        } else if (devices instanceof Iterable<?> it) {
            for (Object o : it) {
                if (o instanceof Map<?, ?> row) {
                    String id = row.get("id") == null ? null : String.valueOf(row.get("id")).trim();
                    String st = row.get("status") == null ? null : String.valueOf(row.get("status")).trim();
                    upsertOne(key, at, id, st);
                }
            }
        }
    }

    private void upsertOne(JifHeadKey key, String agentType, String deviceId, String status) {
        if (deviceId == null || deviceId.isEmpty()) return;
        if (deviceId.length() > 8) deviceId = deviceId.substring(0, 8);
        String st = (status == null) ? null : (status.length() > 8 ? status.substring(0, 8) : status);
        String faultFlag = isDeviceNormal(st) ? "N" : "Y";

        AtmUnitStatus u = AtmUnitStatus.builder()
                .termGroupId(key.getTermGroupId())
                .termId(key.getTermId())
                .deviceId(deviceId)
                .agentType(agentType)
                .status(st)
                .faultFlag(faultFlag)
                .jnlDate(key.getJnlDate())
                .jnlTime(key.getJnlTime())
                .build();
        unitStatusMapper.upsert(u);
    }

    /** 정상 판정: 상태값이 비었거나 정상코드(1)로 시작하면 정상. */
    private static boolean isDeviceNormal(String st) {
        if (st == null || st.isEmpty()) return true;
        return st.startsWith(DEVICE_NORMAL_ST);
    }

    /**
     * CHAR(1) 컬럼 안전 처리 — null 이거나 이미 1자면 그대로, 2자 이상이면 첫 문자만 사용.
     */
    private static String trunc1(String s) {
        if (s == null || s.length() <= 1) return s;
        return s.substring(0, 1);
    }

    /**
     * body.devices[] 는 [{id:"MAN", status:"10000000"}, ...] 형태로 들어옴 (status=8자리).
     * 없거나 비정상이면 null. CHAR(1) 컴럼이므로 status 첫 바이트만 사용.
     */
    @SuppressWarnings("unchecked")
    private static String deviceStatus(Map<String, Object> body, String deviceId) {
        if (body == null) return null;
        Object devs = body.get("devices");
        if (!(devs instanceof Iterable<?> it)) return null;
        for (Object o : it) {
            if (o instanceof Map<?, ?> row) {
                Object id = row.get("id");
                if (id != null && deviceId.equalsIgnoreCase(String.valueOf(id).trim())) {
                    Object st = row.get("status");
                    if (st == null) return null;
                    String s = String.valueOf(st).trim();
                    return s.isEmpty() ? null : s.substring(0, 1);
                }
            }
        }
        return null;
    }
}
