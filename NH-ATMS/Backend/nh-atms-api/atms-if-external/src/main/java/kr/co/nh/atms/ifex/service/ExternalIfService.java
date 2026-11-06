package kr.co.nh.atms.ifex.service;

import kr.co.nh.atms.common.exception.BusinessException;
import kr.co.nh.atms.common.util.GlobalIdGenerator;
import kr.co.nh.atms.ifex.dto.AutoServiceCallRequest;
import kr.co.nh.atms.ifex.dto.AutoServiceCallResult;
import kr.co.nh.atms.ifex.dto.CornerChangeRequest;
import kr.co.nh.atms.ifex.dto.ExternalIfResult;
import kr.co.nh.atms.ifex.dto.MachineInfoRequest;
import kr.co.nh.atms.ifex.dto.ManyFailureRequest;
import kr.co.nh.atms.ifex.dto.ServiceCallCancelRequest;
import kr.co.nh.atms.ifex.dto.ServiceCallChangeRequest;
import kr.co.nh.atms.ifex.dto.ServiceCallRequest;
import kr.co.nh.atms.ifex.dto.StatusNotifyRequest;
import kr.co.nh.atms.ifex.entity.FaultManagement;
import kr.co.nh.atms.ifex.mapper.AutoDispatchMapper;
import kr.co.nh.atms.ifex.mapper.FaultManagementMapper;
import kr.co.nh.atms.ifex.mapper.MsgEnrichMapper;
import kr.co.nh.atms.ifex.mapper.StatusEnrichMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 대외(atme) 송신(OUT) 인터페이스 서비스.
 * 상태통보/출동요청·취소·변경/동시다발/코너변경/기기정보 전문을 AS-IS proc 클래스와 정합하게
 * 서버측에서 조립하여 게이트웨이로 송신하고, 장애관리(T_FAULT_MANAGEMENT)·기기상태이력(T_ATM_MONITOR)을
 * 갱신한다. 송수신 이력 적재/채번은 {@link ExternalDispatchService} 가 담당한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalIfService {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TF = DateTimeFormatter.ofPattern("HHmmss");
    private static final String ORG_CODE = "045";
    private static final String LANG_CD = "KOR";

    /** 출동업체구분 (레거시 ATMRConstants) */
    private static final String PROVIDER_ATM = "1";      // 기기업체
    private static final String PROVIDER_SECURITY = "2"; // 경비업체
    private static final String PROVIDER_OUTSRC = "3";   // 용역업체
    private static final String PROVIDER_PARTNER = "4";  // 협력업체(자체 출동시스템, 기기업체로 대체 송신)

    /** 레거시 ATMEConstants 출동상태 */
    private static final String STATUS_INIT = "0";     // 미처리
    private static final String STATUS_REQUEST = "1";  // 출동요청
    private static final String STATUS_CANCEL = "6";   // 출동취소

    private final ExternalDispatchService dispatchService;
    private final FaultManagementMapper faultMapper;
    private final AutoDispatchMapper autoMapper;
    private final StatusEnrichMapper enrichMapper;
    private final MsgEnrichMapper msgMapper;

    /**
     * 개국/장애/상태 통보 (EM_01001110).
     *
     * 호출키(termGroupId/termId/jnlDate/jnlTime)로 DB 를 조회해 47필드 전문 바디를 서버측에서
     * 조립한 뒤, 수신업체 2곳(기기업체 ATM_CO_CD + 용역업체 OUTSRC_CO_CD)에 각각 송신한다.
     * 레거시 atme EM_01001110_OUT 정합.
     *
     * 전문 47필드 구성:
     *  - 01~16,18 발생일시/금고/코너/기기 기본정보  : T_ATM_MONITOR + 코너/금고/자산 마스터 조인
     *      (운영시간구분 1->3/2->2/3->1 변환, 가동시간 HH:MM~HH:MM 포맷)
     *  - 16 전문구분(ATM_STATUS)  0:상태 2:장애 5:개국
     *  - 22~31 디바이스 상태(현금/수표/명세표/저널/카드/통장 등) : 개국(5) 아닐 때만 세팅
     *      (회수함/지로는 '00' 고정)
     *  - 32 중지여부 : ATM_STATUS=2 면 '1' 아니면 '0'
     *  - 35~43 개국정보(AP버전/DES/IC/EMV/IR/RF/지문/오만원/암호화) : T_ATM_DEVICE_STATUS, 개국(5)일 때만
     *  - 44~46 잔여매수(만원/오만원/수표) : T_CASSETTE_STATUS, 개국 아닐 때만
     *  - 17 표준장애/19 집계분류/20 장치ID : T_FAULT_ANALYSIS(대표장애 1건)
     *  - 21 장애구분 산정(레거시 규칙):
     *      ATM_STATUS=0(상태): 집계분류 00->0,01->2,02->4,03->3,04->3,05->0,99->0
     *      ATM_STATUS=2(장애): 무조건 2(기기장애)
     *      ATM_STATUS=5(개국): 00->0,02->4
     *  - 33 HW장애여부 : 표준장애='01' 이면 '1'
     *  - 47 filler
     */
    @Transactional
    public ExternalIfResult statusNotify(StatusNotifyRequest req) {
        Map<String, Object> key = new HashMap<>();
        key.put("termGroupId", req.getTermGroupId());
        key.put("termId", req.getTermId());
        key.put("jnlDate", req.getJnlDate());
        key.put("jnlTime", req.getJnlTime());

        Map<String, Object> s = enrichMapper.selectAtmStatusInfo(key);
        if (s == null) {
            throw new BusinessException("STATUS_INFO_NOT_FOUND",
                    "기기 상태정보를 찾을 수 없습니다: " + req.getTermGroupId() + "/" + req.getTermId()
                            + " " + req.getJnlDate() + "/" + req.getJnlTime());
        }

        String atmStatus = str(s.get("atmStatus"));
        boolean isOpen = "5".equals(atmStatus);  // 개국(서비스오픈)

        // 전문 47필드 (순서 보존)
        Map<String, String> f = new LinkedHashMap<>();
        f.put("occurDate", str(s.get("jnlDate")));         // 01 발생일자
        f.put("occurTime", str(s.get("jnlTime")));         // 02 발생시각
        f.put("branchCd", str(s.get("termGroupId")));      // 03 금고코드
        f.put("branchName", str(s.get("termGroupName")));  // 04 금고명
        f.put("branchPhone", "");                          // 05 금고전화번호
        f.put("cornerCd", str(s.get("cornerCd")));         // 06 코너코드
        f.put("cornerName", str(s.get("cornerName")));     // 07 코너명
        f.put("termId", str(s.get("termId")));             // 08 단말번호
        f.put("locationTypeCd", str(s.get("cornerTypeCd")));     // 09 장소구분코드
        f.put("operationTypeCd", str(s.get("operationTypeCd"))); // 10 운영시간구분코드
        f.put("serviceTime", str(s.get("serviceTime")));   // 11 가동시간
        f.put("machineSno", str(s.get("assetSno")));       // 12 제조일련번호
        f.put("atmCoCd", str(s.get("atmCoCd")));           // 13 기기업체코드
        f.put("modelCd", str(s.get("modelCd")));           // 14 기종코드
        f.put("processStatus", str(s.get("processCount"))); // 15 진행상태
        f.put("msgTypeCd", atmStatus);                     // 16 전문구분코드
        f.put("stdErrCd", "");                             // 17 표준장애코드 (장애조회 후 세팅)
        f.put("atmErrCd", str(s.get("atmErrCd")));         // 18 기기장애코드
        f.put("faultClassCd", "");                         // 19 집계분류코드 (장애조회 후 세팅)
        f.put("deviceId", "");                             // 20 장치ID (장애조회 후 세팅)
        f.put("faultType", "0");                           // 21 장애구분 (장애조회 후 세팅)

        // 22~31 디바이스 상태 (개국 아닐 때만)
        if (!isOpen) {
            f.put("manStatus", str(s.get("manStatus")));   // 22 현금1만원부상태
            f.put("omanStatus", str(s.get("omanStatus"))); // 23 현금5만원부상태
            f.put("chkOutStatus", str(s.get("chkStatus"))); // 24 수표출금부상태
            f.put("chkInStatus", str(s.get("chaStatus")));  // 25 수표입금부상태
            f.put("sprStatus", str(s.get("sprStatus")));   // 26 명세표부상태
            f.put("jprStatus", str(s.get("jprStatus")));   // 27 저널부상태
            f.put("rjtStatus", "00");                      // 28 회수함상태 (고정)
            f.put("mcrStatus", str(s.get("mcrStatus")));   // 29 카드부상태
            f.put("pbStatus", str(s.get("pbStatus")));     // 30 통장부상태
            f.put("giroStatus", "00");                     // 31 지로부상태 (고정)
        } else {
            f.put("manStatus", "");
            f.put("omanStatus", "");
            f.put("chkOutStatus", "");
            f.put("chkInStatus", "");
            f.put("sprStatus", "");
            f.put("jprStatus", "");
            f.put("rjtStatus", "");
            f.put("mcrStatus", "");
            f.put("pbStatus", "");
            f.put("giroStatus", "");
        }

        f.put("stopFlag", "2".equals(atmStatus) ? "1" : "0"); // 32 중지여부
        f.put("hwFaultFlag", "0");                            // 33 HW장애여부 (장애조회 후 세팅)
        f.put("maintFlag", "0");                              // 34 보수모드여부

        // 35~43 개국정보 (개국일 때만)
        if (isOpen) {
            f.put("appVersion", str(s.get("appVersion")));  // 35 자동기AP버전
            f.put("desApply", str(s.get("desApply")));      // 36 DES보드가능구분
            f.put("icApply", str(s.get("icApply")));        // 37 IC카드가능구분
            f.put("emvApply", str(s.get("emvApply")));      // 38 EMV가능구분
            f.put("irApply", str(s.get("irApply")));        // 39 IR가능구분
            f.put("rfApply", str(s.get("rfApply")));        // 40 RF가능구분
            f.put("fpApply", str(s.get("fpApply")));        // 41 지문인식가능구분
            f.put("oman5Apply", str(s.get("omanApply")));   // 42 현금5만원가능구분
            f.put("encodeApply", str(s.get("encodeApply"))); // 43 암호화상태
        } else {
            f.put("appVersion", "");
            f.put("desApply", "");
            f.put("icApply", "");
            f.put("emvApply", "");
            f.put("irApply", "");
            f.put("rfApply", "");
            f.put("fpApply", "");
            f.put("oman5Apply", "");
            f.put("encodeApply", "");
        }

        // 44~46 잔여매수 (개국 아닐 때만)
        if (!isOpen) {
            Map<String, Object> remain = enrichMapper.selectCassetteRemain(key);
            if (remain != null) {
                f.put("manRemain", str(remain.get("manRemain")));     // 44 현금1만원권잔여매수
                f.put("omanRemain", str(remain.get("omanRemain")));   // 45 현금5만원권잔여매수
                f.put("checkRemain", str(remain.get("checkRemain"))); // 46 수표잔여매수
            } else {
                f.put("manRemain", "");
                f.put("omanRemain", "");
                f.put("checkRemain", "");
            }
        } else {
            f.put("manRemain", "");
            f.put("omanRemain", "");
            f.put("checkRemain", "");
        }

        f.put("filler", ""); // 47 filler

        // 장애이력 대표 1건 (17/19/20/21/33)
        Map<String, Object> fault = enrichMapper.selectFaultAnalysis(key);
        if (fault != null) {
            String stdErrCd = str(fault.get("errGroupCd"));
            String faultClassCd = str(fault.get("faultClassCd"));
            f.put("stdErrCd", stdErrCd);                 // 17 표준장애코드
            f.put("faultClassCd", faultClassCd);         // 19 집계분류코드
            f.put("deviceId", str(fault.get("deviceId"))); // 20 장치ID
            f.put("faultType", resolveFaultType(atmStatus, faultClassCd)); // 21 장애구분
            if ("01".equals(stdErrCd)) {
                f.put("hwFaultFlag", "1");               // 33 HW장애여부
            }
        }

        // 수신업체 2곳 전송: 기기업체(ATM_CO_CD) + 용역업체(OUTSRC_CO_CD)
        List<String> vendorCds = new ArrayList<>();
        vendorCds.add(str(s.get("atmCoCd")));
        vendorCds.add(str(s.get("outsrcCoCd")));

        ExternalIfResult firstResult = null;
        int sent = 0;
        for (String vendorCd : vendorCds) {
            if (vendorCd == null || vendorCd.isBlank()) {
                continue;
            }
            ExternalIfResult r = dispatchService.dispatchOutbound("EM_01001110", "STATUS_NOTIFY",
                    req.getTermGroupId(), req.getTermId(), vendorCd, f, null);
            if (firstResult == null) {
                firstResult = r;
            }
            sent++;
        }

        if (firstResult == null) {
            throw new BusinessException("NO_VENDOR",
                    "전송 대상 업체(기기/용역)가 없습니다: " + req.getTermGroupId() + "/" + req.getTermId());
        }

        log.info("[status-notify] term={}/{} jnl={}/{} atmStatus={} sent={}",
                req.getTermGroupId(), req.getTermId(), req.getJnlDate(), req.getJnlTime(), atmStatus, sent);
        return firstResult;
    }

    /**
     * 장애구분(전문 21) 산정 (레거시 EM_01001110_OUT 규칙).
     * atmStatus=0(상태): 집계분류 00->0,01->2,02->4,03->3,04->3,05->0,99->0
     * atmStatus=2(장애): 무조건 2(기기장애)
     * atmStatus=5(개국): 00->0,02->4
     */
    private static String resolveFaultType(String atmStatus, String faultClassCd) {
        if ("0".equals(atmStatus)) {
            switch (faultClassCd) {
                case "00": return "0"; // 정상
                case "01": return "2"; // 기기장애
                case "02": return "4"; // 운영장애
                case "03": return "3"; // 계원미숙 -> 고객유발장애
                case "04": return "3"; // 고객미숙 -> 고객유발장애
                case "05": return "0"; // 통신장애 -> 정상
                case "99": return "0"; // 기타장애 -> 정상
                default:   return "0";
            }
        } else if ("2".equals(atmStatus)) {
            return "2"; // 기기장애
        } else if ("5".equals(atmStatus)) {
            if ("00".equals(faultClassCd)) return "0"; // 정상
            if ("02".equals(faultClassCd)) return "4"; // 운영장애
        }
        return "0";
    }

    /** 출동 요청 (EM_01001120) — 장애관리 신규 등록 후 25필드 전문 조립·송신 */
    @Transactional
    public ExternalIfResult serviceCall(ServiceCallRequest req) {
        String globalId = GlobalIdGenerator.generate(ORG_CODE);
        LocalDateTime now = LocalDateTime.now();

        FaultManagement fault = FaultManagement.builder()
                .globalId(globalId)
                .termGroupId(req.getTermGroupId())
                .termId(req.getTermId())
                .faultType("SERVICE_CALL")
                .errCd(req.getErrCd())
                .faultStatus("O")
                .callDate(now.format(DF))
                .callTime(now.format(TF))
                .vendorCd(req.getVendorCd())
                .faultDesc(req.getFaultDesc())
                .providerTypeCd(req.getProviderTypeCd())
                .requestTypeCd(req.getRequestTypeCd() == null ? "1" : req.getRequestTypeCd())  // 1:수동
                .requestReasonCd(req.getRequestReasonCd())
                .customerWaitFlag(req.getCustomerWaitFlag() == null ? "0" : req.getCustomerWaitFlag())
                .servicePerson(req.getServicePerson())
                .servicePhoneNo(req.getServicePhoneNo())
                .errGroupCd(req.getErrGroupCd())
                .faultClassCd(req.getFaultClassCd())
                .jnlDate(req.getJnlDate() == null ? now.format(DF) : req.getJnlDate())
                .jnlTime(req.getJnlTime() == null ? now.format(TF) : req.getJnlTime())
                .manageNo(1)
                .build();
        faultMapper.insert(fault);

        return dispatchServiceCall(fault.getFaultSeq(), globalId, "SERVICE_CALL", false);
    }

    /**
     * 출동요청 전문(EM_01001120) 25필드 조립·송신 (레거시 EM_01001120_OUT.procOutbound 정합).
     *
     * <p>T_FAULT_MANAGEMENT + T_ATM/T_ATM_GROUP/T_CORNER 조인 조회 후 25필드를 조립하고,
     * 수신업체는 PROVIDER_TYPE_CD='4'(협력, 자체출동시스템으로 대외 I/F 없음)면 기기업체(ATM_CO_CD)로
     * 대체, 2차 자동출동(secondary)이면 무조건 기기업체로 송신한다.
     * 송신 후 T_FAULT_MANAGEMENT 상태(성공 1:출동요청 / 실패 0:미처리) 갱신과
     * T_ATM_MONITOR 기기상태이력(AGENT_TYPE '9')을 생성한다.</p>
     */
    @Transactional
    public ExternalIfResult dispatchServiceCall(Long faultSeq, String globalId, String ifType, boolean secondary) {
        Map<String, Object> m = msgMapper.selectServiceCallInfo(faultSeq);
        if (m == null) {
            throw new BusinessException("FAULT_NOT_FOUND", "장애관리(출동) 정보를 찾을 수 없습니다: " + faultSeq);
        }

        // 전문 25필드 (스펙 0100/1120 순서 보존)
        Map<String, String> f = new LinkedHashMap<>();
        f.put("requestDate", str(m.get("requestDate")));        // 01 출동요청일자
        f.put("requestTime", str(m.get("requestTime")));        // 02 출동요청시각
        f.put("jnlDate", str(m.get("jnlDate")));                // 03 장애발생일자
        f.put("jnlTime", str(m.get("jnlTime")));                // 04 장애발생시각
        f.put("requestSeqNo", str(m.get("jnlTime")));           // 05 출동요청일련번호 (레거시: JNL_TIME)
        f.put("manageNo", str(m.get("manageNo")));              // 06 출동요청차수 (LPAD 2)
        f.put("dispatchOrderType", str(m.get("dispatchOrderType"))); // 07 출동차수구분 (1:1차 2:2차)
        f.put("termGroupId", str(m.get("termGroupId")));        // 08 금고코드
        f.put("termGroupName", str(m.get("termGroupName")));    // 09 금고명
        f.put("cornerCd", str(m.get("cornerCd")));              // 10 코너코드
        f.put("cornerName", str(m.get("cornerName")));          // 11 코너명
        f.put("termId", str(m.get("termId")));                  // 12 단말번호
        f.put("atmCoCd", str(m.get("atmCoCd")));                // 13 기기업체코드
        f.put("modelCd", str(m.get("modelCd")));                // 14 기종코드
        f.put("requestTypeCd", str(m.get("requestTypeCd")));    // 15 출동요청구분코드 (1수동/2민원/3자동/4이상개국)
        f.put("requestReasonCd", str(m.get("requestReasonCd"))); // 16 출동요청사유코드
        f.put("servicePerson", str(m.get("servicePerson")));    // 17 출동요원명
        f.put("servicePhoneNo", str(m.get("servicePhoneNo")));  // 18 출동요원전화번호
        f.put("errGroupCd", str(m.get("errGroupCd")));          // 19 표준장애코드
        f.put("errCd", str(m.get("errCd")));                    // 20 기기장애코드 (레거시 ERR_CD2)
        f.put("faultClassCd", str(m.get("faultClassCd")));      // 21 집계분류코드
        f.put("requestComment", str(m.get("requestComment")));  // 22 출동요청특이사항
        f.put("crtNo", "");                                     // 23 crt-no
        f.put("customerWaitFlag", str(m.get("customerWaitFlag"))); // 24 고객대기여부
        f.put("filler", "");                                    // 25 filler

        // 수신업체 선정: 협력업체(4)는 기기업체(ATM_CO_CD) 대체, 2차 자동출동도 기기업체
        String vendorCd = PROVIDER_PARTNER.equals(str(m.get("providerTypeCd")))
                ? str(m.get("atmCoCd"))
                : str(m.get("providerCd"));
        if (secondary) {
            vendorCd = str(m.get("atmCoCd"));
        }

        ExternalIfResult result = dispatchService.dispatchOutbound(globalId, "EM_01001120", ifType,
                str(m.get("termGroupId")), str(m.get("termId")), vendorCd, f, faultSeq);

        // 장애출동이력 상태 갱신: 성공 1(출동요청) / 실패 0(미처리)
        boolean success = "0000".equals(result.getResultCd());
        faultMapper.updateDispatchStatus(faultSeq, success ? STATUS_REQUEST : STATUS_INIT);

        // 기기상태이력 생성 (T_ATM_MONITOR)
        insertAtmMonitor(str(m.get("termGroupId")), str(m.get("termId")), STATUS_REQUEST);

        return result;
    }

    /**
     * 자동출동 요청 (EM_01001120) — 출동업체 선정 로직 (레거시 statusFailureJnlExtIFAutoServiceCallHandler).
     * 저널키로 T_FAULT_ANALYSIS 의 자동출동 대상 장애를 조회하여, 장애그룹 설정/휴일/관리·운영구분/
     * 영업시간/계약여부/중복출동을 단계적으로 판정하고, 통과한 건만 장애관리 등록 후 대외로 송신한다.
     */
    @Transactional
    public AutoServiceCallResult autoServiceCall(AutoServiceCallRequest req) {
        Map<String, Object> base = new HashMap<>();
        base.put("termGroupId", req.getTermGroupId());
        base.put("termId", req.getTermId());
        base.put("jnlDate", req.getJnlDate());
        base.put("jnlTime", req.getJnlTime());

        List<Map<String, Object>> faults = autoMapper.selectFaultAnalysis(base);
        List<ExternalIfResult> results = new ArrayList<>();
        int requested = 0;

        LocalDate today = LocalDate.now();
        String todayStr = today.format(DF);
        String fromDate = today.minusDays(2).format(DF);

        for (Map<String, Object> fa : faults) {
            Map<String, Object> p = new HashMap<>(fa);
            p.put("langCd", LANG_CD);
            p.put("bankCd", ORG_CODE);
            p.put("today", todayStr);
            p.put("fromDate", fromDate);
            String jnlTime = str(fa.get("jnlTime"));
            p.put("jnlTime6", jnlTime.length() >= 6 ? jnlTime.substring(0, 6) : jnlTime);

            // 1) 장애그룹별 출동업체 설정 — 셋 다 '0'이면 자동출동 대상 아님
            Map<String, Object> eg = autoMapper.selectErrorGroup(p);
            if (eg == null) {
                continue;
            }
            String serviceCall = str(eg.get("serviceCall"));
            String securityCall = str(eg.get("securityCall"));
            String atmCall = str(eg.get("atmCall"));
            if ("0".equals(serviceCall) && "0".equals(securityCall) && "0".equals(atmCall)) {
                continue;
            }

            // 2) 휴일 여부
            boolean isHoliday = autoMapper.existsHoliday(p);
            p.put("isHoliday", isHoliday);

            // 3) 코너 관리/운영 구분 판정 룰 조회
            Map<String, Object> arc = autoMapper.selectAutoRequestCheck(p);
            if (arc == null) {
                continue;
            }
            String manageType = str(arc.get("manageTypeCd"));

            // 4) 출동업체구분(providerType) 선정
            String providerType;
            if ("2".equals(manageType)) {                       // 용역관리기기
                if ("1".equals(str(arc.get("oursrcFlag"))) && "1".equals(serviceCall)) {
                    providerType = PROVIDER_OUTSRC;
                } else {
                    continue;
                }
            } else if ("1".equals(manageType)) {                // 금고관리기기
                if (isHoliday && "1".equals(securityCall)) {
                    providerType = PROVIDER_SECURITY;           // 휴일 → 경비
                } else if ("1".equals(str(arc.get("cctvSetFlag"))) && "1".equals(securityCall)) {
                    providerType = PROVIDER_SECURITY;           // 주간통보(CCTV)대상 → 경비
                } else if ("1".equals(str(arc.get("atmCoFlag"))) && "1".equals(atmCall)) {
                    providerType = PROVIDER_ATM;                // 기기업체
                } else if ("1".equals(str(arc.get("guardFlag"))) && "1".equals(securityCall)) {
                    providerType = PROVIDER_SECURITY;           // 경비업체
                } else {
                    continue;
                }
            } else {
                continue;                                       // 관리구분 오류
            }
            p.put("providerType", providerType);

            // 5) 출동업체 코드 조회 (미계약/기타경비(29)/null·empty 는 출동 제외)
            String providerCd = autoMapper.selectProviderCd(p);
            if (providerCd == null || providerCd.isEmpty() || "29".equals(providerCd)) {
                continue;
            }
            p.put("providerCd", providerCd);

            // 6) 최근 2일 내 진행중 동일 출동요청 존재 시 중복 방지
            if (autoMapper.existsRecentFaultManagement(p)) {
                continue;
            }

            // 7) 출동요청 코멘트 (장애그룹명 + 장애코드명 or 장치명)
            String errGroupName = str(eg.get("errGroupName"));
            String errName = autoMapper.selectErrName(p);
            String comment;
            if (errName != null && !errName.isBlank()) {
                comment = errGroupName + "(" + errName + ")";
            } else {
                String deviceName = autoMapper.selectDeviceName(p);
                comment = errGroupName + "(" + (deviceName == null ? "" : deviceName) + ")";
            }

            // 8) 장애관리(T_FAULT_MANAGEMENT) 등록
            String globalId = GlobalIdGenerator.generate(ORG_CODE);
            LocalDateTime now = LocalDateTime.now();
            String errCd = str(fa.get("errCd"));
            String faultClassCd = str(fa.get("faultClassCd"));
            String errGroupCd = str(fa.get("errGroupCd"));
            FaultManagement fault = FaultManagement.builder()
                    .globalId(globalId)
                    .termGroupId(req.getTermGroupId())
                    .termId(req.getTermId())
                    .faultType("AUTO_SERVICE_CALL")
                    .errCd(errCd)
                    .faultStatus("O")
                    .callDate(now.format(DF))
                    .callTime(now.format(TF))
                    .vendorCd(providerCd)
                    .faultDesc(comment)
                    .providerTypeCd(providerType)
                    .faultClassCd(faultClassCd)
                    .errGroupCd(errGroupCd)
                    .jnlDate(req.getJnlDate())
                    .jnlTime(req.getJnlTime())
                    .requestTypeCd("3")
                    .customerWaitFlag("0")
                    .manageNo(1)
                    .build();
            faultMapper.insert(fault);

            // 9) 기기상태마스터 최종 출동요청 정보 갱신
            p.put("faultSeq", fault.getFaultSeq());
            autoMapper.updateAtmStatus(p);

            // 대외 송신 (EM_01001120 25필드 조립)
            results.add(dispatchServiceCall(fault.getFaultSeq(), globalId, "AUTO_SERVICE_CALL", false));
            requested++;
        }

        log.info("[auto-dispatch] term={}/{} jnl={}/{} candidate={} requested={}",
                req.getTermGroupId(), req.getTermId(), req.getJnlDate(), req.getJnlTime(),
                faults.size(), requested);

        return AutoServiceCallResult.builder()
                .candidateCount(faults.size())
                .requestedCount(requested)
                .results(results)
                .build();
    }

    /** 출동 취소 (EM_01001160) — 18필드 전문 (레거시 EM_01001160_OUT 정합) */
    @Transactional
    public ExternalIfResult serviceCallCancel(ServiceCallCancelRequest req) {
        FaultManagement fault = requireFault(req.getFaultSeq());
        fault.setFaultStatus("X");
        fault.setFaultDesc(append(fault.getFaultDesc(), "취소:" + nvl(req.getReason())));
        faultMapper.update(fault);
        faultMapper.updateDispatchStatus(fault.getFaultSeq(), STATUS_CANCEL);   // 6:출동취소

        Map<String, Object> m = msgMapper.selectServiceCallInfo(req.getFaultSeq());
        LocalDateTime now = LocalDateTime.now();

        // 전문 18필드 (스펙 0100/1160 순서 보존)
        Map<String, String> f = new LinkedHashMap<>();
        f.put("termGroupId", str(m.get("termGroupId")));        // 01 금고코드
        f.put("cornerCd", str(m.get("cornerCd")));              // 02 코너코드
        f.put("termId", str(m.get("termId")));                  // 03 단말번호
        f.put("jnlDate", str(m.get("jnlDate")));                // 04 장애발생일자
        f.put("requestSeqNo", str(m.get("jnlTime")));           // 05 출동요청일련번호
        f.put("manageNo", str(m.get("manageNo")));              // 06 출동요청차수
        f.put("requestTypeCd", str(m.get("requestTypeCd")));    // 07 출동요청구분코드
        f.put("dispatchOrderType", str(m.get("dispatchOrderType"))); // 08 출동차수구분
        f.put("cancelDate", now.format(DF));                    // 09 취소요청일자
        f.put("cancelTime", now.format(TF));                    // 10 취소요청시각
        f.put("cancelReasonCd", nvl(req.getCancelReasonCd()));  // 11 출동취소사유코드
        f.put("cancelComment", nvl(req.getReason()));           // 12 특이사항
        f.put("crtNo", "");                                     // 13 crt-no
        f.put("requestReasonCd", str(m.get("requestReasonCd"))); // 14 출동요청사유코드
        f.put("errGroupCd", str(m.get("errGroupCd")));          // 15 표준장애코드
        f.put("errCd", str(m.get("errCd")));                    // 16 기기장애코드
        f.put("faultClassCd", str(m.get("faultClassCd")));      // 17 집계분류코드
        f.put("filler", "");                                    // 18 filler

        // 기기상태이력 + 자동화기기 상태 출동정보 클리어 (레거시 순서 동일)
        insertAtmMonitor(str(m.get("termGroupId")), str(m.get("termId")), STATUS_CANCEL);
        Map<String, Object> clear = new HashMap<>();
        clear.put("termGroupId", str(m.get("termGroupId")));
        clear.put("termId", str(m.get("termId")));
        clear.put("vendorType", str(m.get("providerTypeCd")));
        clear.put("faultClassCd", str(m.get("faultClassCd")));
        msgMapper.updateAtmStatusClear(clear);

        // 수신업체: PROVIDER_CD (레거시 정합 — 취소는 대체 없이 원 수신업체)
        return dispatchService.dispatchOutbound(fault.getGlobalId(), "EM_01001160", "SERVICE_CALL_CANCEL",
                fault.getTermGroupId(), fault.getTermId(), str(m.get("providerCd")), f, fault.getFaultSeq());
    }

    /** 출동 변경 (EM_01001170) — 13필드 전문 (레거시 EM_01001170_OUT 정합, 변경구분=고객대기여부) */
    @Transactional
    public ExternalIfResult serviceCallChange(ServiceCallChangeRequest req) {
        FaultManagement fault = requireFault(req.getFaultSeq());
        if (req.getErrCd() != null) fault.setErrCd(req.getErrCd());
        if (req.getVendorCd() != null) fault.setVendorCd(req.getVendorCd());
        if (req.getFaultDesc() != null) fault.setFaultDesc(req.getFaultDesc());
        if (req.getCustomerWaitFlag() != null) fault.setCustomerWaitFlag(req.getCustomerWaitFlag());
        fault.setFaultStatus("P");
        faultMapper.update(fault);

        Map<String, Object> m = msgMapper.selectServiceCallInfo(req.getFaultSeq());

        // 전문 13필드 (스펙 0100/1170 순서 보존)
        Map<String, String> f = new LinkedHashMap<>();
        f.put("termGroupId", str(m.get("termGroupId")));        // 01 금고코드
        f.put("cornerCd", str(m.get("cornerCd")));              // 02 코너코드
        f.put("termId", str(m.get("termId")));                  // 03 단말번호
        f.put("jnlDate", str(m.get("jnlDate")));                // 04 장애발생일자
        f.put("requestSeqNo", str(m.get("jnlTime")));           // 05 출동요청일련번호 (레거시 REQUEST_SEQNO=JNL_TIME)
        f.put("manageNo", str(m.get("manageNo")));              // 06 출동요청차수
        f.put("requestTypeCd", str(m.get("requestTypeCd")));    // 07 출동요청구분코드
        f.put("dispatchOrderType", str(m.get("dispatchOrderType"))); // 08 출동차수구분
        f.put("changeTypeCd", str(m.get("customerWaitFlag")));  // 09 변경구분코드 (레거시: CUSTOMER_WAIT_FLAG)
        f.put("changeComment", str(m.get("requestComment")));   // 10 특이사항
        f.put("crtNo", "");                                     // 11 crt-no
        f.put("faultClassCd", str(m.get("faultClassCd")));      // 12 집계분류코드
        f.put("filler", "");                                    // 13 filler

        return dispatchService.dispatchOutbound(fault.getGlobalId(), "EM_01001170", "SERVICE_CALL_CHANGE",
                fault.getTermGroupId(), fault.getTermId(), str(m.get("providerCd")), f, fault.getFaultSeq());
    }

    /**
     * 동시다발 장애 (EM_01001180) — 14필드 전문 (레거시 EM_01001180_OUT 정합).
     * AS-IS T_REPEATEDLYFAULTATM 폐지에 따라 대표 단말 기준 장애관리 등록 후
     * FAULT_SEQ 를 출동요청번호(7자리)로 실어 송신한다. 임계치/발생매체는 요청 파라미터.
     */
    @Transactional
    public ExternalIfResult manyFailure(ManyFailureRequest req) {
        String globalId = GlobalIdGenerator.generate(ORG_CODE);
        LocalDateTime now = LocalDateTime.now();
        String repTermId = req.getTermIds().get(0);

        FaultManagement fault = FaultManagement.builder()
                .globalId(globalId)
                .termGroupId(req.getTermGroupId())
                .termId(repTermId)
                .faultType("MANY_FAILURE")
                .errCd(req.getErrCd())
                .faultStatus("O")
                .callDate(now.format(DF))
                .callTime(now.format(TF))
                .faultDesc(nvl(req.getFaultDesc()) + " (대상 " + req.getTermIds().size() + "대)")
                .manageNo(1)
                .build();
        faultMapper.insert(fault);

        Map<String, Object> key = new HashMap<>();
        key.put("termGroupId", req.getTermGroupId());
        key.put("termId", repTermId);
        Map<String, Object> m = msgMapper.selectManyFailureAtmInfo(key);
        if (m == null) {
            m = new HashMap<>();
        }

        // 전문 14필드 (스펙 0100/1180 순서 보존)
        Map<String, String> f = new LinkedHashMap<>();
        f.put("sendDate", now.format(DF));                      // 01 전문전송일자
        f.put("sendTime", now.format(TF));                      // 02 전문전송시각
        f.put("baseDays", "01");                                // 03 관리기준일 (레거시 고정 01)
        f.put("faultThreshold", lpad(nvl(req.getFaultThreshold()), 3)); // 04 관리기준임계치
        f.put("faultCount", lpad(String.valueOf(req.getTermIds().size()), 3)); // 05 장애발생건수
        f.put("termGroupId", req.getTermGroupId());             // 06 금고코드
        f.put("termId", repTermId);                             // 07 단말번호
        f.put("branchCd", str(m.get("branchCd")));              // 08 코너코드 (레거시: BRANCH_CD 세팅)
        f.put("cornerName", str(m.get("cornerName")));          // 09 코너명
        f.put("modelCd", str(m.get("modelCd")));                // 10 기종코드
        f.put("comment", nvl(req.getFaultDesc()));              // 11 특이사항
        f.put("requestNo", lpad(String.valueOf(fault.getFaultSeq()), 7)); // 12 출동요청번호 (FAULT_SEQ)
        f.put("errDevice", nvl(req.getErrDevice()));            // 13 장애발생매체
        f.put("filler", "");                                    // 14 filler

        String vendorCd = req.getVendorCd() != null && !req.getVendorCd().isBlank()
                ? req.getVendorCd()
                : str(m.get("atmCoCd"));

        return dispatchService.dispatchOutbound(globalId, "EM_01001180", "MANY_FAILURE",
                req.getTermGroupId(), repTermId, vendorCd, f, fault.getFaultSeq());
    }

    /**
     * 코너 변경 (SM_02001160) — 20필드 전문 (레거시 SM_02001160_OUT 정합).
     * 관리업체(OUTSRC_CO_CD)가 없으면 변경요청 대상이 아니므로 송신하지 않는다.
     */
    @Transactional
    public ExternalIfResult cornerChange(CornerChangeRequest req) {
        Map<String, Object> key = new HashMap<>();
        key.put("termGroupId", req.getTermGroupId());
        key.put("cornerCd", req.getCornerCd());
        Map<String, Object> m = msgMapper.selectCorner(key);
        if (m == null) {
            throw new BusinessException("CORNER_NOT_FOUND",
                    "코너정보를 찾을 수 없습니다: " + req.getTermGroupId() + "/" + req.getCornerCd());
        }

        String outsrcCoCd = str(m.get("outsrcCoCd"));
        if (outsrcCoCd.isBlank()) {
            log.info("[corner-change] 관리업체코드 없음 — 변경요청 대상 아님: {}/{}",
                    req.getTermGroupId(), req.getCornerCd());
            return ExternalIfResult.builder()
                    .resultCd("0097")
                    .resultMsg("관리업체코드 없음 — 변경요청 대상 아님(미전송)")
                    .build();
        }

        // 전문 20필드 (스펙 0200/1160 순서 보존)
        Map<String, String> f = new LinkedHashMap<>();
        f.put("termGroupId", str(m.get("termGroupId")));        // 01 금고코드
        f.put("termGroupName", str(m.get("termGroupName")));    // 02 금고명
        f.put("cornerCd", str(m.get("cornerCd")));              // 03 코너코드
        f.put("cornerName", str(m.get("cornerName")));          // 04 코너명
        f.put("changeType", nvl(req.getChangeType()));          // 05 변경구분 (1:신규 2:변경 3:폐쇄)
        f.put("cornerTypeCd", str(m.get("cornerTypeCd")));      // 06 장소구분코드
        f.put("operationTypeCd", str(m.get("operationTypeCd"))); // 07 운영시간구분코드
        f.put("address", str(m.get("address")));                // 08 설치주소
        f.put("branchCd", str(m.get("branchCd")));              // 09 관리점코드
        f.put("beforeCornerCd", nvl(req.getBeforeCornerCd()));  // 10 변경전코너코드
        f.put("beforeCornerName", nvl(req.getBeforeCornerName())); // 11 변경전코너명
        f.put("beforeCornerTypeCd", nvl(req.getBeforeCornerTypeCd())); // 12 변경전장소구분코드
        f.put("beforeOperationTypeCd", nvl(req.getBeforeOperationTypeCd())); // 13 변경전운영시간구분코드
        f.put("beforeAddress", nvl(req.getBeforeAddress()));    // 14 변경전설치주소
        f.put("beforeBranchCd", nvl(req.getBeforeBranchCd()));  // 15 변경전관리점코드
        f.put("outsrcCoCd", outsrcCoCd);                        // 16 관리업체코드
        f.put("changeDate", LocalDateTime.now().format(DF));    // 17 변경일자
        f.put("operDate", "");                                  // 18 운영일자
        f.put("comment", "");                                   // 19 특이사항
        f.put("filler", "");                                    // 20 filler

        return dispatchService.dispatchOutbound("SM_02001160", "CORNER_CHANGE",
                req.getTermGroupId(), req.getTermId(), outsrcCoCd, f, null);
    }

    /**
     * 기기정보 통보 (IQ_04001140) — 6필드 전문 (레거시 IQ_04001140_OUT 정합).
     * 기기정보(ADD_INFO)/협력업체명이 없으면 통보 대상이 아니므로 송신하지 않는다.
     */
    @Transactional
    public ExternalIfResult machineInfo(MachineInfoRequest req) {
        Map<String, Object> key = new HashMap<>();
        key.put("termGroupId", req.getTermGroupId());
        key.put("termId", req.getTermId());
        Map<String, Object> m = msgMapper.selectMachineInfo(key);
        if (m == null) {
            log.info("[machine-info] 기기정보/협력업체 조회 결과 없음 — 미전송: {}/{}",
                    req.getTermGroupId(), req.getTermId());
            return ExternalIfResult.builder()
                    .resultCd("0097")
                    .resultMsg("기기정보/협력업체 없음 — 통보 대상 아님(미전송)")
                    .build();
        }

        // 전문 6필드 (스펙 0400/1140 순서 보존)
        Map<String, String> f = new LinkedHashMap<>();
        f.put("termGroupId", req.getTermGroupId());             // 01 금고코드
        f.put("termId", req.getTermId());                       // 02 단말번호
        f.put("infoType", "1");                                 // 03 구분 (1:기기정보)
        f.put("machineInfo", str(m.get("addInfo")));            // 04 기기정보
        f.put("providerName", str(m.get("providerName")));      // 05 협력업체명
        f.put("filler", "");                                    // 06 filler

        return dispatchService.dispatchOutbound("IQ_04001140", "MACHINE_INFO",
                req.getTermGroupId(), req.getTermId(), str(m.get("atmCoCd")), f, null);
    }

    // ---- 공통 ----

    /** 기기상태이력 생성 (T_ATM_MONITOR — AGENT_TYPE '9'). 이력 실패가 본 처리에 영향 주지 않도록 로깅만. */
    private void insertAtmMonitor(String termGroupId, String termId, String atmStatus) {
        try {
            Map<String, Object> p = new HashMap<>();
            p.put("termGroupId", termGroupId);
            p.put("termId", termId);
            p.put("atmStatus", atmStatus);
            msgMapper.insertAtmMonitor(p);
        } catch (Exception e) {
            log.warn("[atm-monitor] 기기상태이력 생성 실패 term={}/{} status={}: {}",
                    termGroupId, termId, atmStatus, e.getMessage());
        }
    }

    private FaultManagement requireFault(Long faultSeq) {
        FaultManagement fault = faultMapper.selectBySeq(faultSeq);
        if (fault == null) {
            throw new BusinessException("FAULT_NOT_FOUND", "존재하지 않는 장애관리 건입니다: " + faultSeq);
        }
        return fault;
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString();
    }

    private static String lpad(String s, int len) {
        String v = nvl(s);
        if (v.length() >= len) return v;
        return "0".repeat(len - v.length()) + v;
    }

    private static String append(String base, String add) {
        if (base == null || base.isBlank()) return add;
        return base + " / " + add;
    }
}
