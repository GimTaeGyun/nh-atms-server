package kr.co.nh.atms.ifex.service;

import kr.co.nh.atms.common.exception.BusinessException;
import kr.co.nh.atms.common.util.GlobalIdGenerator;
import kr.co.nh.atms.ifex.dto.ArrivalRequest;
import kr.co.nh.atms.ifex.dto.CleanManageRequest;
import kr.co.nh.atms.ifex.dto.CornerTimeRequest;
import kr.co.nh.atms.ifex.dto.DeparturePlanRequest;
import kr.co.nh.atms.ifex.dto.ExternalIfResult;
import kr.co.nh.atms.ifex.dto.FacilityReportRequest;
import kr.co.nh.atms.ifex.dto.ManyFailureResultRequest;
import kr.co.nh.atms.ifex.dto.MissingMsgRequest;
import kr.co.nh.atms.ifex.dto.RepairResultRequest;
import kr.co.nh.atms.ifex.entity.FaultManagement;
import kr.co.nh.atms.ifex.entity.NetSendData;
import kr.co.nh.atms.ifex.mapper.AutoDispatchMapper;
import kr.co.nh.atms.ifex.mapper.FaultManagementMapper;
import kr.co.nh.atms.ifex.mapper.MsgEnrichMapper;
import kr.co.nh.atms.ifex.mapper.NetSendDataMapper;
import kr.co.nh.atms.ifex.mapper.SequenceNoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.MessageFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 대외(atme) 수신(IN) 인터페이스 서비스.
 *
 * <p>출동업체 -> ATMS 로 들어오는 전문을 처리하여 장애관리(T_FAULT_MANAGEMENT)의 후반부 상태를
 * 갱신하고, 수신 이력(T_NET_RECV_DATA — 수신등록 채번 "004"/netrecv_reg + 업체 일련번호 동기화)과
 * 응답전문 역송신(responseCd/atms_sn/vend_sn 반향 — 레거시 procResponseForInbound)을 수행한다.</p>
 *
 * <ul>
 *   <li>도착예정 (EM_01001130) : 출동예정일시/접수자/출동요원 기록, 상태 3(도착예정)</li>
 *   <li>도착통보 (EM_01001140) : 도착일시 기록 + 출동~도착 경과시간, 상태 4(도착)</li>
 *   <li>조치결과 (EM_01001150) : 조치일시/결과 기록 + 도착~조치 경과시간, 상태 5(조치완료)/8(미복구).
 *       1차출동 & 미조치 & 조치불가사유 1(기기A/S) 이면 기기업체 2차 자동출동(EM_01001120, 차수+1)</li>
 *   <li>동시다발 조치결과 (EM_01001190) : 기기사(vend_cd 선두 '1')만 반영</li>
 *   <li>결번요청 (IQ_04001110) : 원 송신전문 재전송 또는 최종일련번호 응답(0098)</li>
 *   <li>코너입퇴실 (IQ_04001130) : T_CORNER_TIME_INFO 생성 + T_CORNER.GUARD_STATUS +
 *       (업무구분 1/2/3/5 & 입실) 장애관리 경비해제일시</li>
 *   <li>시설물보고 (SM_02001180) / 환경관리 (SM_02001190) : AS-IS 저장 테이블 폐지 — 이력+응답만</li>
 * </ul>
 *
 * <p>레거시는 복합키(금고/단말/장애일/요청일련/분류/차수)로 갱신했으나, 신규는 출동요청 응답에서
 * 발급한 FAULT_SEQ 단일키로 매칭한다. 경과시간은 단순 시간차(분)로 산출하며, 영업시간 기준
 * 산출(레거시 F_GET_ELAPSEDTIME_VENDOR)은 SLA 통계 이관 시점에 별도 반영한다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalIfInboundService {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TF = DateTimeFormatter.ofPattern("HHmmss");
    private static final String ORG_CODE = "045";

    /** 레거시 ATMEConstants 응답코드 */
    private static final String RESPONSE_OK = "0000";
    private static final String RESPONSE_ERROR = "0099";
    private static final String RESPONSE_MISSING = "0098";

    /** 레거시 ATMEConstants 출동상태코드 */
    private static final String STATUS_DEPARTURE = "3"; // 도착예정
    private static final String STATUS_ARRIVAL = "4";   // 도착
    private static final String STATUS_FINISH = "5";    // 조치완료
    private static final String STATUS_NOT_FINISH = "8"; // 미복구

    /** 신규 FAULT_STATUS (O=접수, P=진행, C=완료, X=취소) */
    private static final String FS_PROGRESS = "P";
    private static final String FS_COMPLETE = "C";

    private final FaultManagementMapper faultMapper;
    private final MsgEnrichMapper msgMapper;
    private final NetSendDataMapper sendMapper;
    private final SequenceNoMapper seqNoMapper;
    private final AutoDispatchMapper autoDispatchMapper;
    private final ExternalDispatchService dispatchService;
    private final ExternalIfService externalIfService;

    /** 도착예정 수신 (EM_01001130) */
    @Transactional
    public ExternalIfResult arrivalPlan(DeparturePlanRequest req) {
        recordInbound("EM_01001130", req.getVendorCd(), req.getAtmsSn(), req.getVendSn(), req.toString());

        FaultManagement fault = requireFault(req.getFaultSeq());
        fault.setDispatchStatusCd(STATUS_DEPARTURE);
        fault.setFaultStatus(FS_PROGRESS);
        fault.setDepartureDate(req.getDepartureDate());
        fault.setDepartureTime(req.getDepartureTime());
        fault.setResponsePerson(req.getResponsePerson());
        fault.setServicePerson(req.getServicePerson());
        fault.setServicePhoneNo(req.getServicePhoneNo());
        faultMapper.updateDeparture(fault);

        respond("EM_01101130", req.getVendorCd(), RESPONSE_OK, req.getAtmsSn(), req.getVendSn(),
                echo("faultSeq", String.valueOf(req.getFaultSeq())));

        log.info("[inbound] departure-plan faultSeq={} person={}", req.getFaultSeq(), req.getServicePerson());
        return result(fault);
    }

    /** 도착통보 수신 (EM_01001140) */
    @Transactional
    public ExternalIfResult arrival(ArrivalRequest req) {
        recordInbound("EM_01001140", req.getVendorCd(), req.getAtmsSn(), req.getVendSn(), req.toString());

        FaultManagement fault = requireFault(req.getFaultSeq());
        fault.setDispatchStatusCd(STATUS_ARRIVAL);
        fault.setFaultStatus(FS_PROGRESS);
        fault.setArrivalDate(req.getArrivalDate());
        fault.setArrivalTime(req.getArrivalTime());
        fault.setArrivalElapsedTime(
                elapsedMinutes(fault.getCallDate(), fault.getCallTime(), req.getArrivalDate(), req.getArrivalTime()));
        faultMapper.updateArrival(fault);

        respond("EM_01101140", req.getVendorCd(), RESPONSE_OK, req.getAtmsSn(), req.getVendSn(),
                echo("faultSeq", String.valueOf(req.getFaultSeq())));

        log.info("[inbound] arrival faultSeq={} arrivalElapsed={}min", req.getFaultSeq(), fault.getArrivalElapsedTime());
        return result(fault);
    }

    /**
     * 조치결과 수신 (EM_01001150) — 레거시 EM_01001150_IN.procInbound 정합.
     *
     * <p>조치일시 공백 시 전문 헤더 일시(msgDate/msgTime, 없으면 현재시각)로 보정하고,
     * T_ATM_MONITOR 이력 생성·T_ATM_STATUS 출동정보 클리어 후,
     * 1차출동(출동차수구분 1) & 미조치(0) & 조치불가사유 기기A/S(1) 이면
     * 기기업체 2차 자동출동(EM_01001120, manage_no+1)을 기동한다.</p>
     */
    @Transactional
    public ExternalIfResult repairResult(RepairResultRequest req) {
        recordInbound("EM_01001150", req.getVendorCd(), req.getAtmsSn(), req.getVendSn(), req.toString());

        FaultManagement fault = requireFault(req.getFaultSeq());

        // 조치일시 공백 보정 (레거시 20251010 보완: 전문 헤더 일시 fallback)
        LocalDateTime now = LocalDateTime.now();
        String finishedDate = isBlank(req.getFinishedDate())
                ? (isBlank(req.getMsgDate()) ? now.format(DF) : req.getMsgDate().trim())
                : req.getFinishedDate();
        String finishedTime = isBlank(req.getFinishedTime())
                ? (isBlank(req.getMsgTime()) ? now.format(TF) : req.getMsgTime().trim())
                : req.getFinishedTime();

        boolean repaired = "1".equals(req.getRepairFlag());
        fault.setDispatchStatusCd(repaired ? STATUS_FINISH : STATUS_NOT_FINISH);
        fault.setFaultStatus(repaired ? FS_COMPLETE : FS_PROGRESS);
        fault.setRepairFlag(req.getRepairFlag());
        fault.setFinishedDate(finishedDate);
        fault.setFinishedTime(finishedTime);
        fault.setFailureReasonCd(req.getFailureReasonCd());
        fault.setRepairComment(req.getRepairComment());
        fault.setFinishElapsedTime(
                elapsedMinutes(fault.getArrivalDate(), fault.getArrivalTime(), finishedDate, finishedTime));
        int updated = faultMapper.updateRepairResult(fault);

        // 기기상태이력 생성 (T_ATM_MONITOR — 조치완료 5)
        insertAtmMonitor(fault.getTermGroupId(), fault.getTermId(), STATUS_FINISH);

        // 자동화기기 상태 출동정보 클리어 (T_ATM_STATUS — 업체구분: vend_cd 선두 1자리)
        String vendorCd = nvl(req.getVendorCd());
        if (!vendorCd.isBlank()) {
            try {
                Map<String, Object> clear = new HashMap<>();
                clear.put("termGroupId", fault.getTermGroupId());
                clear.put("termId", fault.getTermId());
                clear.put("vendorType", vendorCd.substring(0, 1));
                clear.put("faultClassCd", nvl(fault.getFaultClassCd()));
                msgMapper.updateAtmStatusClear(clear);
            } catch (Exception e) {
                log.warn("[inbound] T_ATM_STATUS 출동정보 클리어 실패 faultSeq={}: {}",
                        req.getFaultSeq(), e.getMessage());
            }
        }

        // 1차출동 & 미조치(0) & 조치불가사유 기기A/S(1) → 기기업체 2차 자동출동
        Long secondFaultSeq = null;
        boolean firstDispatch = !"1".equals(nvl(fault.getProviderTypeCd()))
                && !"4".equals(nvl(fault.getProviderTypeCd()));   // 출동차수구분 '1' (레거시 CASE 규칙)
        if (firstDispatch && "0".equals(req.getRepairFlag()) && "1".equals(nvl(req.getFailureReasonCd()))) {
            secondFaultSeq = dispatchSecondServiceCall(fault);
        }

        respond("EM_01101150", req.getVendorCd(),
                updated == 1 ? RESPONSE_OK : RESPONSE_ERROR, req.getAtmsSn(), req.getVendSn(),
                echo("faultSeq", String.valueOf(req.getFaultSeq())));

        log.info("[inbound] repair-result faultSeq={} repaired={} finishElapsed={}min secondFaultSeq={}",
                req.getFaultSeq(), repaired, fault.getFinishElapsedTime(), secondFaultSeq);
        return result(fault);
    }

    /**
     * 기기업체 2차 자동출동 (레거시 EM_01001150_IN 하단 로직).
     * 원 출동 건 복제 + manage_no+1, 출동요청구분 3(자동), 업체구분 1(기기), 수신업체 = ATM_CO_CD.
     */
    private Long dispatchSecondServiceCall(FaultManagement origin) {
        Map<String, Object> m = msgMapper.selectServiceCallInfo(origin.getFaultSeq());
        String atmCoCd = m == null ? "" : str(m.get("atmCoCd"));
        if (atmCoCd.isBlank()) {
            log.warn("[inbound] 2차출동 불가 — 기기업체코드 없음: faultSeq={}", origin.getFaultSeq());
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        String comment = MessageFormat.format("[1차출동] {0} [2차보수출동] [{1}:{2}:{3}] {4}",
                nvl(origin.getFaultDesc()), nvl(origin.getServicePerson()), nvl(origin.getServicePhoneNo()),
                nvl(origin.getFailureReasonCd()), nvl(origin.getRepairComment()));

        FaultManagement second = FaultManagement.builder()
                .globalId(GlobalIdGenerator.generate(ORG_CODE))
                .termGroupId(origin.getTermGroupId())
                .termId(origin.getTermId())
                .faultType("AUTO_SERVICE_CALL")
                .errCd(origin.getErrCd())
                .faultStatus("O")
                .callDate(now.format(DF))
                .callTime(now.format(TF))
                .vendorCd(atmCoCd)
                .faultDesc(comment)
                .providerTypeCd("1")                    // 기기업체
                .faultClassCd(origin.getFaultClassCd())
                .errGroupCd(origin.getErrGroupCd())
                .jnlDate(origin.getJnlDate())
                .jnlTime(origin.getJnlTime())
                .requestTypeCd("3")                     // 자동
                .customerWaitFlag(nvl(origin.getCustomerWaitFlag()))
                .requestReasonCd(origin.getRequestReasonCd())
                .manageNo((origin.getManageNo() == null ? 1 : origin.getManageNo()) + 1)
                .build();
        faultMapper.insert(second);

        // 기기상태마스터 최종 출동요청 정보 갱신 (레거시 updateAtmStatus — 기기업체)
        try {
            Map<String, Object> p = new HashMap<>();
            p.put("termGroupId", second.getTermGroupId());
            p.put("termId", second.getTermId());
            p.put("providerType", "1");
            p.put("jnlDate", second.getJnlDate());
            p.put("jnlTime", second.getJnlTime());
            p.put("faultSeq", second.getFaultSeq());
            p.put("faultClassCd", second.getFaultClassCd());
            autoDispatchMapper.updateAtmStatus(p);
        } catch (Exception e) {
            log.warn("[inbound] 2차출동 T_ATM_STATUS 갱신 실패: {}", e.getMessage());
        }

        // EM_01001120 재호출 (2차: 수신업체 = 기기업체)
        externalIfService.dispatchServiceCall(second.getFaultSeq(), second.getGlobalId(),
                "AUTO_SERVICE_CALL_2ND", true);
        return second.getFaultSeq();
    }

    /** 동시다발장애 조치결과 수신 (EM_01001190) — 기기사(vend_cd 선두 '1')만 반영 */
    @Transactional
    public ExternalIfResult manyFailureResult(ManyFailureResultRequest req) {
        recordInbound("EM_01001190", req.getVendorCd(), req.getAtmsSn(), req.getVendSn(), req.toString());

        String vendorCd = nvl(req.getVendorCd());
        String responseCd = RESPONSE_OK;
        FaultManagement fault = null;

        if (!vendorCd.isEmpty() && vendorCd.charAt(0) == '1') {   // 기기사일 경우만 처리 (레거시 정합)
            fault = faultMapper.selectBySeq(req.getFaultSeq());
            if (fault == null) {
                responseCd = RESPONSE_ERROR;
            } else {
                fault.setDispatchStatusCd(STATUS_FINISH);
                fault.setFaultStatus(FS_COMPLETE);
                fault.setRepairFlag("1");
                fault.setFinishedDate(req.getFinishedDate());
                fault.setFinishedTime(req.getFinishedTime());
                fault.setRepairComment(buildManyFailureComment(req));
                fault.setFinishElapsedTime(null);
                faultMapper.updateRepairResult(fault);
            }
        } else {
            log.info("[inbound] many-failure-result 기기사 아님 — 미반영: vendorCd={}", vendorCd);
        }

        respond("EM_01101190", req.getVendorCd(), responseCd, req.getAtmsSn(), req.getVendSn(),
                echo("faultSeq", String.valueOf(req.getFaultSeq())));

        return ExternalIfResult.builder()
                .globalId(fault == null ? null : fault.getGlobalId())
                .resultCd(responseCd)
                .resultMsg(RESPONSE_OK.equals(responseCd) ? "OK" : "대상 없음/미반영")
                .faultSeq(req.getFaultSeq())
                .build();
    }

    private static String buildManyFailureComment(ManyFailureResultRequest req) {
        StringBuilder sb = new StringBuilder();
        if (!isBlank(req.getRepairPerson())) {
            sb.append("[조치자:").append(req.getRepairPerson().trim()).append("] ");
        }
        sb.append(nvl(req.getMemo()));
        return sb.toString().trim();
    }

    /**
     * 결번요청 수신 (IQ_04001110) — 레거시 IQ_04001110_IN 정합.
     *
     * <ul>
     *   <li>요청전문일련번호 공백: T_SEQ_NO_MASTER("004"/vendorCd/"netsend_snd") 최종일련번호를
     *       message 로 응답 (응답코드 0098, 채번이력 없으면 0099)</li>
     *   <li>요청전문일련번호 존재: T_NET_SEND_DATA 재조회 후 원 전문 재전송 (응답전문 없음),
     *       없으면 0099 응답</li>
     * </ul>
     */
    @Transactional
    public ExternalIfResult missingMsg(MissingMsgRequest req) {
        recordInbound("IQ_04001110", req.getVendorCd(), req.getAtmsSn(), req.getVendSn(), req.toString());

        String reqSeqNo = nvl(req.getReqSeqNo()).trim();

        if (reqSeqNo.isEmpty()) {
            // 최종일련번호 응답 (헤더 atms_sn/vend_sn 은 0 세팅 — 레거시 정합)
            Long lastNo = seqNoMapper.selectBaseNo("004", req.getVendorCd(), "netsend_snd");
            String responseCd = lastNo == null ? RESPONSE_ERROR : RESPONSE_MISSING;
            Map<String, String> body = new LinkedHashMap<>();
            body.put("reqDate", nvl(req.getReqDate()));                                // 01 요청전문일자
            body.put("reqSeqNo", "");                                                  // 02 요청전문일련번호
            body.put("message", lastNo == null ? "" : String.format("%07d", lastNo));  // 03 최종일련번호
            ExternalIfResult r = respond("IQ_04101110", req.getVendorCd(), responseCd,
                    zeroSn(), zeroSn(), body);
            log.info("[inbound] missing-msg 최종일련번호 응답: vendor={} last={} rsp={}",
                    req.getVendorCd(), lastNo, responseCd);
            return r != null ? r
                    : ExternalIfResult.builder().resultCd(responseCd).resultMsg("응답 역송신 생략").build();
        }

        // 원 전문 재전송
        NetSendData origin = sendMapper.selectForResend(req.getVendorCd(), nvl(req.getReqDate()), reqSeqNo);
        if (origin == null || origin.getSendMsg() == null || origin.getSendMsg().isEmpty()) {
            log.warn("[inbound] missing-msg 원 송신전문 없음: vendor={} date={} sn={}",
                    req.getVendorCd(), req.getReqDate(), reqSeqNo);
            Map<String, String> body = new LinkedHashMap<>();
            body.put("reqDate", nvl(req.getReqDate()));
            body.put("reqSeqNo", reqSeqNo);
            body.put("message", "");
            ExternalIfResult r = respond("IQ_04101110", req.getVendorCd(), RESPONSE_ERROR,
                    nvl(req.getAtmsSn()), nvl(req.getVendSn()), body);
            return r != null ? r
                    : ExternalIfResult.builder().resultCd(RESPONSE_ERROR).resultMsg("원 송신전문 없음").build();
        }

        ExternalIfResult r = dispatchService.resend(origin);
        log.info("[inbound] missing-msg 재전송: vendor={} date={} sn={} tr={}",
                req.getVendorCd(), req.getReqDate(), reqSeqNo, origin.getTrCode());
        return r;
    }

    /**
     * 코너입퇴실시간정보 수신 (IQ_04001130) — 레거시 IQ_04001130_IN 정합.
     * T_CORNER_TIME_INFO 생성 + T_CORNER.GUARD_STATUS 갱신 후,
     * 업무구분 1(경비)/2(장애)/3(현송)/5(은행) & 출입구분 2(카드OFF=입실)이면
     * 장애관리 경비해제일시 갱신. (1:카드SET=퇴실, 4:환경은 미처리)
     */
    @Transactional
    public ExternalIfResult cornerTime(CornerTimeRequest req) {
        recordInbound("IQ_04001130", req.getVendorCd(), req.getAtmsSn(), req.getVendSn(), req.toString());

        String responseCd = RESPONSE_OK;
        Map<String, Object> p = new HashMap<>();
        p.put("providerCd", req.getVendorCd());
        p.put("inoutDate", req.getInoutDate());
        p.put("inoutTime", req.getInoutTime());
        p.put("inoutType", req.getInoutType());
        p.put("termGroupId", req.getTermGroupId());
        p.put("termId", req.getTermId());
        p.put("workType", req.getWorkType());
        p.put("cardNo", req.getCardNo());

        if (msgMapper.insertCornerTimeInfo(p) != 1) {
            responseCd = RESPONSE_ERROR;
        }
        if (msgMapper.updateCornerGuardStatus(p) != 1) {
            responseCd = RESPONSE_ERROR;
        }

        String workType = nvl(req.getWorkType());
        boolean targetWork = "1".equals(workType) || "2".equals(workType)
                || "3".equals(workType) || "5".equals(workType);   // 4(환경)은 미처리
        if (targetWork && "2".equals(nvl(req.getInoutType()))) {   // 2:카드OFF=입실
            msgMapper.updateFaultSecurityOff(p);
        } else {
            log.debug("[inbound] corner-time 경비해제 미처리: workType={} inoutType={}",
                    workType, req.getInoutType());
        }

        respond("IQ_04101130", req.getVendorCd(), responseCd, req.getAtmsSn(), req.getVendSn(),
                echo("termGroupId", req.getTermGroupId(), "termId", req.getTermId()));

        log.info("[inbound] corner-time term={}/{} inout={}/{} work={} rsp={}",
                req.getTermGroupId(), req.getTermId(), req.getInoutDate(), req.getInoutType(),
                workType, responseCd);
        return ExternalIfResult.builder().resultCd(responseCd)
                .resultMsg(RESPONSE_OK.equals(responseCd) ? "OK" : "일부 갱신 실패").build();
    }

    /** 시설물관리업무보고 수신 (SM_02001180) — AS-IS 저장 테이블(T_EQUIPMANAGE) 미사용 폐지, 이력+응답만 */
    @Transactional
    public ExternalIfResult facilityReport(FacilityReportRequest req) {
        recordInbound("SM_02001180", req.getVendorCd(), req.getAtmsSn(), req.getVendSn(), req.toString());
        respond("SM_02101180", req.getVendorCd(), RESPONSE_OK, req.getAtmsSn(), req.getVendSn(),
                echo("termGroupId", nvl(req.getTermGroupId()), "cornerCd", nvl(req.getCornerCd())));
        log.info("[inbound] facility-report vendor={} corner={}/{}",
                req.getVendorCd(), req.getTermGroupId(), req.getCornerCd());
        return ExternalIfResult.builder().resultCd(RESPONSE_OK).resultMsg("수신등록(저장 테이블 폐지)").build();
    }

    /** 환경관리업무 수신 (SM_02001190) — AS-IS 저장 테이블(T_CLEANMANAGE) 미사용 폐지, 이력+응답만 */
    @Transactional
    public ExternalIfResult cleanManage(CleanManageRequest req) {
        recordInbound("SM_02001190", req.getVendorCd(), req.getAtmsSn(), req.getVendSn(), req.toString());
        respond("SM_02101190", req.getVendorCd(), RESPONSE_OK, req.getAtmsSn(), req.getVendSn(),
                echo("termGroupId", nvl(req.getTermGroupId()), "termId", nvl(req.getTermId())));
        log.info("[inbound] clean-manage vendor={} term={}/{}",
                req.getVendorCd(), req.getTermGroupId(), req.getTermId());
        return ExternalIfResult.builder().resultCd(RESPONSE_OK).resultMsg("수신등록(저장 테이블 폐지)").build();
    }

    // ---- 내부 공통 ----

    /** 수신 이력 적재 — vendorCd 미제공(기존 REST 호출 호환) 시 생략. */
    private void recordInbound(String msgCode, String vendorCd, String atmsSn, String vendSn, String recvMsg) {
        if (vendorCd == null || vendorCd.isBlank()) {
            return;
        }
        try {
            dispatchService.recordInbound(msgCode, vendorCd, atmsSn, vendSn, recvMsg);
        } catch (Exception e) {
            log.warn("[inbound] 수신 이력 적재 실패 msg={}: {}", msgCode, e.getMessage());
        }
    }

    /** 응답전문 역송신 — vendorCd 미제공 시 생략(null 반환). */
    private ExternalIfResult respond(String msgCode, String vendorCd, String responseCd,
                                     String atmsSn, String vendSn, Map<String, String> echoBody) {
        if (vendorCd == null || vendorCd.isBlank()) {
            return null;
        }
        try {
            return dispatchService.dispatchResponse(msgCode, vendorCd, responseCd, atmsSn, vendSn, echoBody);
        } catch (Exception e) {
            log.warn("[inbound] 응답 역송신 실패 msg={}: {}", msgCode, e.getMessage());
            return null;
        }
    }

    /** 기기상태이력 생성 — 실패해도 본 처리에 영향 없음. */
    private void insertAtmMonitor(String termGroupId, String termId, String atmStatus) {
        try {
            Map<String, Object> p = new HashMap<>();
            p.put("termGroupId", termGroupId);
            p.put("termId", termId);
            p.put("atmStatus", atmStatus);
            msgMapper.insertAtmMonitor(p);
        } catch (Exception e) {
            log.warn("[inbound] 기기상태이력 생성 실패 term={}/{}: {}", termGroupId, termId, e.getMessage());
        }
    }

    private static Map<String, String> echo(String... kv) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            m.put(kv[i], kv[i + 1] == null ? "" : kv[i + 1]);
        }
        return m;
    }

    private static String zeroSn() {
        return String.format("%07d", 0);
    }

    private FaultManagement requireFault(Long faultSeq) {
        FaultManagement fault = faultMapper.selectBySeq(faultSeq);
        if (fault == null) {
            throw new BusinessException("FAULT_NOT_FOUND", "존재하지 않는 장애관리 건입니다: " + faultSeq);
        }
        return fault;
    }

    private ExternalIfResult result(FaultManagement fault) {
        return ExternalIfResult.builder()
                .globalId(fault.getGlobalId())
                .resultCd("0000")
                .resultMsg("OK")
                .faultSeq(fault.getFaultSeq())
                .build();
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString();
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /**
     * 두 일시(yyyyMMdd + HHmmss)의 단순 경과시간(분)을 산출한다.
     * 입력이 불완전하면 null 을 반환한다.
     */
    private static Long elapsedMinutes(String fromDate, String fromTime, String toDate, String toTime) {
        LocalDateTime from = parse(fromDate, fromTime);
        LocalDateTime to = parse(toDate, toTime);
        if (from == null || to == null) {
            return null;
        }
        long minutes = Duration.between(from, to).toMinutes();
        return minutes < 0 ? 0L : minutes;
    }

    private static LocalDateTime parse(String date, String time) {
        if (date == null || date.length() != 8 || time == null) {
            return null;
        }
        String t = (time + "000000").substring(0, 6);
        try {
            return LocalDateTime.parse(date + t, DTF);
        } catch (Exception e) {
            return null;
        }
    }
}
