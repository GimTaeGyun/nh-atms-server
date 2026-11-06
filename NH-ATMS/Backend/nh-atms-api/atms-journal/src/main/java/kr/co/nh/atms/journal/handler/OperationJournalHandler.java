package kr.co.nh.atms.journal.handler;

import kr.co.nh.atms.journal.dto.JifHeadKey;
import kr.co.nh.atms.journal.dto.JifMessage;
import kr.co.nh.atms.journal.entity.Atm;
import kr.co.nh.atms.journal.entity.AtmDeviceStatus;
import kr.co.nh.atms.journal.entity.AtmGroup;
import kr.co.nh.atms.journal.entity.AtmHistory;
import kr.co.nh.atms.journal.entity.AtmMonitor;
import kr.co.nh.atms.journal.entity.AtmStopHistory;
import kr.co.nh.atms.journal.entity.AtmUnitStatus;
import kr.co.nh.atms.journal.entity.Branch;
import kr.co.nh.atms.journal.entity.OperationJournal;
import kr.co.nh.atms.journal.mapper.AtmDeviceStatusWriteMapper;
import kr.co.nh.atms.journal.mapper.AtmGroupWriteMapper;
import kr.co.nh.atms.journal.mapper.AtmHistoryWriteMapper;
import kr.co.nh.atms.journal.mapper.AtmMonitorWriteMapper;
import kr.co.nh.atms.journal.mapper.AtmStatusWriteMapper;
import kr.co.nh.atms.journal.mapper.AtmStopHistoryWriteMapper;
import kr.co.nh.atms.journal.mapper.AtmUnitStatusWriteMapper;
import kr.co.nh.atms.journal.mapper.AtmWriteMapper;
import kr.co.nh.atms.journal.mapper.BranchWriteMapper;
import kr.co.nh.atms.journal.mapper.OperationJournalWriteMapper;
import kr.co.nh.atms.journal.parser.JifFieldUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;

/**
 * jnl_kind=52 (운영저널) → T_OPERATION_JOURNAL INSERT.
 *
 * body 필드 매핑 (atmr RecvJournalMapper.xml insert_ignore_T_OPERATIONJOUNAL 참고):
 *   op_cd  → OPERATION_CD     (NULL/공백 시 '99' 기타)
 *   op_act → OPERATION_ACTION (NULL/공백 시 '기타')
 *
 * 운영코드 예시 (T_JOURNAL_DATA 원본 텍스트에서 관측된 패턴):
 *   11=AP시작, 12=AP종료, 21=POWER ON, 22=POWER OFF,
 *   31=서비스상태전환, 99=기타
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OperationJournalHandler implements JournalHandler {

    private static final String DEFAULT_CD     = "99";

    /** 52 운영저널 기본 액션 라벨 (body 에 op_act 없을 때 사용). */
    private static final String DEFAULT_ACTION = "운영";

    /** 개국 시 기기 서비스오픈 상태(atmr ATMRConstants.APP_SERVICE_OPEN). T_ATM_STATUS.SERVICE_STATUS 용. */
    private static final String APP_SERVICE_OPEN = "05";

    /** T_ATM.ATM_STATUS 운영 값(atmr update_T_ATM / insert_ignore_T_ATM 공통 '1'). '1'정상/'2'장애 체계. */
    private static final String ATM_OPERATING = "1";

    private static final DateTimeFormatter RECV_D = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter RECV_T = DateTimeFormatter.ofPattern("HHmmssSSS");

    private final OperationJournalWriteMapper mapper;
    private final AtmStatusWriteMapper atmStatusMapper;
    private final AtmWriteMapper atmMapper;
    private final AtmHistoryWriteMapper atmHistoryMapper;
    private final AtmDeviceStatusWriteMapper atmDeviceStatusMapper;
    private final AtmUnitStatusWriteMapper atmUnitStatusMapper;
    private final AtmMonitorWriteMapper atmMonitorMapper;
    private final AtmStopHistoryWriteMapper atmStopHistoryMapper;
    private final BranchWriteMapper branchMapper;
    private final AtmGroupWriteMapper atmGroupMapper;

    @Override
    public Set<String> supportedKinds() {
        // 11=AP시작, 12=개국, 52=운영(POWER ON/OFF 등 일반)
        return Set.of("11", "12", "52");
    }

    @Override
    public void handle(JifHeadKey key, JifMessage msg) {
        Map<String, Object> body = msg.getBody();
        String jnlKind = key.getJnlKind();

        // T_OPERATION_JOURNAL 은 52(운영저널)만 기록한다.
        // [변경 이유] 초기 포팅은 11(AP시작)/12(개국)도 운영이력으로 INSERT 했으나,
        // AS-IS(atmr)는 OperationProcessor(52)만 T_OPERATIONJOUNAL 에 기록하고
        // 11/12 는 APStartProcessor/OpenProcessor 가 버전/기기등록 처리만 수행한다.
        // 재검증(AS-IS 정합) 결과에 따라 11/12 의 INSERT 를 제거했다. (MIGRATION-NOTES.md 참고)
        if ("52".equals(jnlKind)) {
            String opCd  = JifFieldUtils.s(body, "op_cd");
            String opAct = JifFieldUtils.s(body, "op_act");
            if (opCd == null)  opCd  = DEFAULT_CD;
            if (opAct == null) opAct = DEFAULT_ACTION;

            OperationJournal o = OperationJournal.builder()
                    .termGroupId(key.getTermGroupId())
                    .termId(key.getTermId())
                    .jnlDate(key.getJnlDate())
                    .jnlTime(key.getJnlTime())
                    .operationCd(opCd)
                    .operationAction(opAct)
                    .build();

            int n = mapper.insert(o);
            log.debug("[jnl op] T_OPERATION_JOURNAL insert={} term={}/{} time={} kind={} op={}/{}",
                    n, key.getTermGroupId(), key.getTermId(),
                    key.getJnlTime(), jnlKind, o.getOperationCd(), o.getOperationAction());

            // 52 운영 → 운영상태 전환 시 마스터 동기화 (atmr update_T_ATMSTATUS_at_Operation)
            String svcStatus = serviceStatusOf(opCd);
            if (svcStatus != null) {
                atmStatusMapper.updateServiceStatus(key.getTermGroupId(), key.getTermId(), svcStatus);
            }
        }

        // 11 AP시작 → 디바이스 버전정보 갱신 (atmr APStartProcessor T_ATMUNITSTATUS)
        if ("11".equals(jnlKind)) {
            upsertApVersions(key, msg);
        }

        // 12 개국 → 기기 자동 등록/갱신 + 변경이력 + 기기상태 동기화 (atmr OpenProcessor)
        if ("12".equals(jnlKind)) {
            insertOpenMonitor(key, msg);   // 1. 기기 모니터정보 생성 (T_ATM_MONITOR)
            registerOrUpdateAtm(key, msg); // 2. 기기 등록/갱신 + 신규조합 + 이력 + branch/booth 후처리
            handleFirstOpen(key);           // 3~4. 최초개국 중지이력 + 개국정보(INIT) 갱신
            upsertDeviceStatus(key, body);  // 5. 기기모듈 장착정보 (T_ATM_DEVICE_STATUS)
        }
    }

    /**
     * 11 AP시작: devices[] 디바이스 버전정보를 T_ATM_UNIT_STATUS 로 upsert.
     * (atmr APStartProcessor upsert_T_ATMUNITSTATUS)
     *   id   → DEVICE_ID / LOGICAL_SERVICE
     *   ver1 → SP_VERSION, ver2 → EP_VERSION
     */
    @SuppressWarnings("unchecked")
    private void upsertApVersions(JifHeadKey key, JifMessage msg) {
        Map<String, Object> body = msg.getBody();
        if (body == null) return;
        Object devices = body.get("devices");
        if (!(devices instanceof Iterable<?> it)) return;

        String agentType = JifFieldUtils.s(msg.getHead(), "agent_type");
        if (agentType == null || agentType.isBlank()) agentType = "1";
        agentType = agentType.trim().substring(0, 1);

        int n = 0;
        for (Object o : it) {
            if (!(o instanceof Map<?, ?> row)) continue;
            String id = row.get("id") == null ? null : String.valueOf(row.get("id")).trim();
            if (id == null || id.isEmpty()) continue;
            if (id.length() > 8) id = id.substring(0, 8);
            String ver1 = row.get("ver1") == null ? null : String.valueOf(row.get("ver1")).trim();
            String ver2 = row.get("ver2") == null ? null : String.valueOf(row.get("ver2")).trim();
            AtmUnitStatus u = AtmUnitStatus.builder()
                    .termGroupId(key.getTermGroupId())
                    .termId(key.getTermId())
                    .deviceId(id)
                    .agentType(agentType)
                    .logicalService(id)
                    .jnlDate(key.getJnlDate())
                    .jnlTime(key.getJnlTime())
                    .spVersion(ver1)
                    .epVersion(ver2)
                    .build();
            atmUnitStatusMapper.upsert(u);
            n++;
        }
        log.debug("[jnl 11] T_ATM_UNIT_STATUS 버전갱신={} term={}/{}",
                n, key.getTermGroupId(), key.getTermId());
    }

    /**
     * 12 개국 1단계: 기기 모니터정보 생성 (atmr OpenProcessor insert_T_ATMMONITOR).
     * ATM_STATUS='05'(서비스오픈), 나머지 상태/에러는 null, SEND_FLAG='0'.
     */
    private void insertOpenMonitor(JifHeadKey key, JifMessage msg) {
        String agentType = JifFieldUtils.s(msg.getHead(), "agent_type");
        if (agentType == null || agentType.isBlank()) agentType = "1";
        LocalDateTime now = LocalDateTime.now();
        AtmMonitor m = AtmMonitor.builder()
                .termGroupId(key.getTermGroupId())
                .termId(key.getTermId())
                .jnlDate(key.getJnlDate())
                .jnlTime(key.getJnlTime())
                .agentType(agentType)
                .atmStatus(APP_SERVICE_OPEN)
                .sendFlag("0")
                .recvDate(now.format(RECV_D))
                .recvTime(now.format(RECV_T) + "0")
                .build();
        int n = atmMonitorMapper.insert(m);
        log.debug("[jnl 12] T_ATM_MONITOR 개국 insert={} term={}/{}",
                n, key.getTermGroupId(), key.getTermId());
    }

    /**
     * 12 개국 3~4단계: 오늘 최초 개국이면 운영중지이력 생성 후 개국정보(INIT) 갱신.
     * (atmr OpenProcessor select_count_T_ATMSTATUS → insert_T_ATMSTOPHISTORY → upsert_T_ATMSTATUS)
     * 주의: 오늘 최초 개국 여부는 INIT_DATE 갱신 전에 카운트해야 올바르다.
     */
    private void handleFirstOpen(JifHeadKey key) {
        int cnt = atmStatusMapper.countTodayOpen(
                key.getTermGroupId(), key.getTermId(), key.getJnlDate());
        if (cnt == 0) {
            AtmStopHistory stop = AtmStopHistory.builder()
                    .termGroupId(key.getTermGroupId())
                    .termId(key.getTermId())
                    .jnlDate(key.getJnlDate())
                    .jnlTime(shortTime(key.getJnlTime()))
                    .errGroupCd("FO")
                    .faultStatus("9")
                    .build();
            atmStopHistoryMapper.insertStop(stop);
            log.debug("[jnl 12] 오늘 최초개국 T_ATM_STOP_HISTORY insert term={}/{} date={}",
                    key.getTermGroupId(), key.getTermId(), key.getJnlDate());
        }
        atmStatusMapper.openInit(key.getTermGroupId(), key.getTermId(),
                key.getJnlDate(), shortTime(key.getJnlTime()));
    }

    /** CHAR(6) 용 — jnl_time(최대 10자리)을 6자리로 절삭. */
    private static String shortTime(String t) {
        if (t == null) return null;
        return t.length() > 6 ? t.substring(0, 6) : t;
    }

    /**
     * 개국(12) 5단계: 기기 디바이스정보 upsert (atmr OpenProcessor upsert_T_ATMDEVICESTATUS).
     * body 의 디바이스 지원 플래그(des_st/icc_st/.../pwd)와 etc_info(200byte) 를 파싱한다.
     * etc_info 구간: IP[0,20] OS[20,70] CPU[70,120] MEM[120,140]
     *                HDD1_TOT[140,150] HDD1_USED[150,160] HDD2_TOT[160,170]
     *                HDD2_USED[170,180] INSTALL_DATE[180,188]
     */
    private void upsertDeviceStatus(JifHeadKey key, Map<String, Object> body) {
        String etcInfo = JifFieldUtils.s(body, "etc_info");

        AtmDeviceStatus device = AtmDeviceStatus.builder()
                .termGroupId(key.getTermGroupId())
                .termId(key.getTermId())
                .desApply(JifFieldUtils.s(body, "des_st"))
                .icApply(JifFieldUtils.s(body, "icc_st"))
                .emvApply(JifFieldUtils.s(body, "emv_st"))
                .irApply(JifFieldUtils.s(body, "ir_st"))
                .rfApply(JifFieldUtils.s(body, "rf_st"))
                .fpApply(JifFieldUtils.s(body, "fp_st"))
                .omanApply(JifFieldUtils.s(body, "50k_st"))
                .encodeApply(JifFieldUtils.s(body, "enc_st"))
                .addInfo(JifFieldUtils.s(body, "vd_dt"))
                .barcodeApply(JifFieldUtils.s(body, "2d_bc"))
                .chkStatus(JifFieldUtils.s(body, "1000k_check"))
                .pbAutoTurn(JifFieldUtils.s(body, "pb_auto_flip"))
                .handicapAtm(JifFieldUtils.s(body, "pwd"))
                .ipAddress(JifFieldUtils.sub(etcInfo, 0, 20))
                .osInfo(JifFieldUtils.sub(etcInfo, 20, 50))
                .cpuInfo(JifFieldUtils.sub(etcInfo, 70, 50))
                .memInfo(JifFieldUtils.sub(etcInfo, 120, 20))
                .hdd1Tot(JifFieldUtils.sub(etcInfo, 140, 10))
                .hdd1Used(JifFieldUtils.sub(etcInfo, 150, 10))
                .hdd2Tot(JifFieldUtils.sub(etcInfo, 160, 10))
                .hdd2Used(JifFieldUtils.sub(etcInfo, 170, 10))
                .installDate(JifFieldUtils.sub(etcInfo, 180, 8))
                .build();

        int n = atmDeviceStatusMapper.upsert(device);
        log.debug("[jnl 12] T_ATM_DEVICE_STATUS upsert={} term={}/{} ip={}",
                n, key.getTermGroupId(), key.getTermId(), device.getIpAddress());
    }

    /**
     * 개국(12) 시 기기 마스터 자동 등록/갱신 (atmr OpenProcessor 2~2-5단계 이식).
     *   - bc_no(바코드) 앞 7자리 → owner_cd[0,3]/branch_cd[3,6]/booth_type_cd[6]
     *   - 미등록: T_ATM insert(asset_flag='2') + 신규조합이면 T_BRANCH/T_ATM_GROUP 자동생성
     *            + T_ATM_HISTORY(update_cd='1')
     *   - 기등록: T_ATM update(asset_flag='1') + 수신제조번호 변경 시 T_ATM_HISTORY(update_cd='3')
     *   - 공통 후처리: branch_cd(빈값/끝자리'0' 아닐 때) / booth_type_cd(빈값 아닐 때) 갱신
     * 기기 부가컬럼(asset_sno/status_flag/tran_flag/recv_atm_ip/processor_type/
     * settle_date/remote_flag/protocol_type)도 MG 와 동일하게 채운다.
     */
    private void registerOrUpdateAtm(JifHeadKey key, JifMessage msg) {
        Map<String, Object> body = msg.getBody();

        String vdCd  = JifFieldUtils.s(body, "vd_cd");        // 기기업체코드
        String mdNo  = JifFieldUtils.s(body, "md_no");        // 모델코드
        String snNo  = trim(JifFieldUtils.s(body, "sn_no"));  // 제조일련번호
        String apVer = trim(JifFieldUtils.s(body, "ap_ver")); // AP 버전
        String bcNo  = trim(JifFieldUtils.s(body, "bc_no"));  // 바코드
        String pcCd  = JifFieldUtils.s(body, "pc_cd");        // 프로세서타입
        String atmIp = parseEtcIp(JifFieldUtils.s(body, "etc_info"));

        // agent_type 은 head 에 위치(없으면 '1'). status/tran/remote_flag 공통값.
        String agentType = JifFieldUtils.s(msg.getHead(), "agent_type");
        if (agentType == null || agentType.isBlank()) agentType = "1";
        agentType = agentType.trim().substring(0, 1);

        String ownerCd     = bcSeg(bcNo, 0, 3);
        String branchCd    = bcSeg(bcNo, 3, 6);
        String boothTypeCd = bcSeg(bcNo, 6, 7);

        String enterTime = key.getJnlTime();
        if (enterTime != null && enterTime.length() > 6) enterTime = enterTime.substring(0, 6);
        String settleDate = LocalDateTime.now().format(RECV_D);

        boolean registered = atmMapper.existsByTerm(key.getTermGroupId(), key.getTermId()) > 0;

        if (!registered) {
            Atm atm = Atm.builder()
                    .termGroupId(key.getTermGroupId())
                    .termId(key.getTermId())
                    .atmStatus(ATM_OPERATING)
                    .assetFlag("2")
                    .enterDate(key.getJnlDate())
                    .enterTime(enterTime)
                    .atmCoCd(vdCd)
                    .modelCd(mdNo)
                    .assetSno(snNo)
                    .recvMachineSno(snNo)
                    .appVersion(apVer)
                    .statusFlag(agentType)
                    .tranFlag(agentType)
                    .recvAtmIp(atmIp)
                    .atmIp(atmIp)
                    .processorType(pcCd)
                    .settleDate(settleDate)
                    .remoteFlag(agentType)
                    .protocolType("T")
                    .ownerCd(ownerCd)
                    .build();
            atmMapper.insert(atm);

            // 신규 조합이면 임시 영업점/기기그룹 자동생성 (atmr select_count_T_BRANCH → insert)
            if (branchMapper.countByGroup(key.getTermGroupId()) == 0) {
                branchMapper.insertIgnore(Branch.builder()
                        .termGroupId(key.getTermGroupId())
                        .branchCd("01")
                        .branchName("미등록영업점")
                        .branchTypeCd("1")
                        .build());
                Integer groupLevel = parseGroupLevel(key.getTermGroupId());
                atmGroupMapper.insertIgnore(AtmGroup.builder()
                        .termGroupId(key.getTermGroupId())
                        .termGroupName("미등록영업점")
                        .groupLevel(groupLevel)
                        .parentId(null)   // CHAR(3) < termGroupId(5): 임시노드는 null
                        .superId(null)
                        .build());
                log.debug("[jnl 12] 신규조합 T_BRANCH/T_ATM_GROUP 자동생성 term_group={}",
                        key.getTermGroupId());
            }

            insertHistory(key, snNo, vdCd, mdNo, "1", "신규등록");
            log.debug("[jnl 12] T_ATM 신규등록 term={}/{} sn={}",
                    key.getTermGroupId(), key.getTermId(), snNo);
        } else {
            String oldSno = trim(atmMapper.selectRecvMachineSno(key.getTermGroupId(), key.getTermId()));
            Atm atm = Atm.builder()
                    .termGroupId(key.getTermGroupId())
                    .termId(key.getTermId())
                    .atmStatus(ATM_OPERATING)
                    .assetFlag("1")
                    .atmCoCd(vdCd)
                    .modelCd(mdNo)
                    .assetSno(snNo)
                    .recvMachineSno(snNo)
                    .appVersion(apVer)
                    .statusFlag(agentType)
                    .tranFlag(agentType)
                    .recvAtmIp(atmIp)
                    .atmIp(atmIp)
                    .processorType(pcCd)
                    .remoteFlag(agentType)
                    .protocolType("T")
                    .ownerCd(ownerCd)
                    .build();
            atmMapper.updateOnOpen(atm);
            if (snNo != null && !snNo.isEmpty() && !snNo.equals(oldSno)) {
                insertHistory(key, snNo, vdCd, mdNo, "3",
                        "제조번호변경 " + (oldSno == null ? "" : oldSno) + "->" + snNo);
                log.debug("[jnl 12] T_ATM 제조번호변경 term={}/{} {} -> {}",
                        key.getTermGroupId(), key.getTermId(), oldSno, snNo);
            }
        }

        // 공통 후처리 2-4: branch_cd 가 빈값이거나 끝자리 '0' 이면 갱신 안함
        if (branchCd != null && !branchCd.isEmpty() && !branchCd.endsWith("0")) {
            atmMapper.updateBranchCd(key.getTermGroupId(), key.getTermId(), branchCd);
        }
        // 공통 후처리 2-5: booth_type_cd 가 빈값이 아니면 갱신
        if (boothTypeCd != null && !boothTypeCd.isEmpty()) {
            atmMapper.updateBoothTypeCd(key.getTermGroupId(), key.getTermId(), boothTypeCd);
        }
    }

    /** term_group_id 를 group_level(숫자)로 변환. 비숫자면 null. (atmr Integer.parseInt) */
    private static Integer parseGroupLevel(String termGroupId) {
        if (termGroupId == null) return null;
        try {
            return Integer.parseInt(termGroupId.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** T_ATM_HISTORY 1건 적재(제조번호 없으면 skip). */
    private void insertHistory(JifHeadKey key, String snNo, String atmCoCd,
                               String modelCd, String updateCd, String desc) {
        if (snNo == null || snNo.isEmpty()) return;
        String upDate = key.getJnlDate();
        String upTime = key.getJnlTime();
        if (upTime != null && upTime.length() > 6) upTime = upTime.substring(0, 6);
        AtmHistory h = AtmHistory.builder()
                .machineSno(snNo)
                .updateDate(upDate)
                .updateTime(upTime)
                .termGroupId(key.getTermGroupId())
                .termId(key.getTermId())
                .atmCoCd(atmCoCd)
                .modelCd(modelCd)
                .updateCd(updateCd)
                .updateDesc(desc)
                .build();
        atmHistoryMapper.insert(h);
    }

    /** 바코드(bc_no) 구간 추출. 길이 부족 시 null. */
    private static String bcSeg(String bc, int from, int to) {
        if (bc == null || bc.length() < to) return null;
        String s = bc.substring(from, to).trim();
        return s.isEmpty() ? null : s;
    }

    /** etc_info(200byte) 의 IP 구간[0,20] 추출. ATM_IP(CHAR15) 한도. */
    private static String parseEtcIp(String etcInfo) {
        if (etcInfo == null || etcInfo.isBlank()) return null;
        String ip = etcInfo.length() >= 20 ? etcInfo.substring(0, 20) : etcInfo;
        ip = ip.trim();
        if (ip.isEmpty()) return null;
        return ip.length() > 15 ? ip.substring(0, 15) : ip;
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    /**
     * 운영코드(op_cd) → 기기 운영상태(SERVICE_STATUS) 매핑.
     * atmr ATMRConstants: OP_IN_SERVICE(10)→IN_SERVICE(00),
     * OP_OUT_SERVICE(11)→OUT_OF_SERVICE(02),
     * OP_OPERATOR(12)/OP_TECHNICIAN(13)→IN_SUPERVISOR(03).
     * 그 외 값은 상태를 건드리지 않음(null).
     */
    private static String serviceStatusOf(String opCd) {
        if (opCd == null) return null;
        return switch (opCd.trim()) {
            case "10" -> "00";
            case "11" -> "02";
            case "12", "13" -> "03";
            default -> null;
        };
    }

}
