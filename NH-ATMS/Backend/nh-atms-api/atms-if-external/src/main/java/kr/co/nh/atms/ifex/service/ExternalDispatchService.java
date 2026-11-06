package kr.co.nh.atms.ifex.service;

import kr.co.nh.atms.common.util.GlobalIdGenerator;
import kr.co.nh.atms.ifex.dto.ExternalIfResult;
import kr.co.nh.atms.ifex.entity.NetRecvData;
import kr.co.nh.atms.ifex.entity.NetSendData;
import kr.co.nh.atms.ifex.gateway.ExternalGateway;
import kr.co.nh.atms.ifex.gateway.GatewayResult;
import kr.co.nh.atms.ifex.mapper.NetRecvDataMapper;
import kr.co.nh.atms.ifex.mapper.NetSendDataMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 대외 전문 공통 송수신 디스패처.
 *
 * <p>전문 송신(atms_sn "004"/vend_sn "005" 영구 채번 → 게이트웨이 송신 →
 * T_NET_SEND_DATA / T_NET_RECV_DATA 이력 적재)과 수신전문 등록(수신등록 채번 "004"/netrecv_reg,
 * 업체 일련번호 동기화 SP_UPDATE_SEQNO 등가)을 담당한다.
 * 레거시 atme Gateway.send_post + MessageMakeUtil.makeHead + insertIgnoreNetSendData/RecvData 정합.</p>
 *
 * <p>송신 전문(JSON)은 순서 보존 맵으로 직렬화되며 선두 3개 키(vendorCd/atmsSn/vendSn,
 * 응답전문은 +responseCd)는 전문 헤더 라우팅 정보, 이후 키는 전문 바디 필드(스펙 순서와 1:1)다.
 * EifExternalGateway 는 이 규약으로 고정길이 전문을 조립한다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalDispatchService {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TF = DateTimeFormatter.ofPattern("HHmmss");
    private static final String ORG_CODE = "045";

    /** 송신 msg 맵 헤더 예약 키 (바디 필드 아님) */
    public static final String KEY_VENDOR = "vendorCd";
    public static final String KEY_ATMS_SN = "atmsSn";
    public static final String KEY_VEND_SN = "vendSn";
    public static final String KEY_RESPONSE_CD = "responseCd";

    private final ExternalGateway gateway;
    private final NetSendDataMapper sendMapper;
    private final NetRecvDataMapper recvMapper;
    private final SequenceNoService seqNoService;

    /** 요청(OUT) 전문 송신 — 신규 globalId 발급. */
    @Transactional
    public ExternalIfResult dispatchOutbound(String msgCode, String ifType, String termGroupId, String termId,
                                             String vendorCd, Map<String, String> body, Long faultSeq) {
        return dispatchOutbound(GlobalIdGenerator.generate(ORG_CODE), msgCode, ifType,
                termGroupId, termId, vendorCd, body, faultSeq);
    }

    /**
     * 요청(OUT) 전문 송신.
     * atms_sn = SP_GET_SEQNO("004", vendorCd, "netsend_snd") / vend_sn = SP_GET_SEQNO("005", vendorCd, "").
     */
    @Transactional
    public ExternalIfResult dispatchOutbound(String globalId, String msgCode, String ifType,
                                             String termGroupId, String termId,
                                             String vendorCd, Map<String, String> body, Long faultSeq) {
        String vnd = nvl(vendorCd);
        String atmsSn = vnd.isBlank() ? "0000000" : seqNoService.nextFormatted("004", vnd, "netsend_snd");
        String vendSn = vnd.isBlank() ? "0000000" : seqNoService.nextFormatted("005", vnd, "");

        Map<String, String> msgMap = new LinkedHashMap<>();
        msgMap.put(KEY_VENDOR, vnd);
        msgMap.put(KEY_ATMS_SN, atmsSn);
        msgMap.put(KEY_VEND_SN, vendSn);
        msgMap.putAll(body);

        return send(globalId, msgCode, ifType, termGroupId, termId, vnd, atmsSn, vendSn, toJson(msgMap), faultSeq);
    }

    /**
     * 수신전문에 대한 응답(역송신) 전문 송신 — atms_sn/vend_sn 은 수신전문 헤더 값을 그대로 반향.
     * 레거시 procResponseForInbound(응답코드 0000/0098/0099 + 수신바디 echo) 정합.
     */
    @Transactional
    public ExternalIfResult dispatchResponse(String msgCode, String vendorCd, String responseCd,
                                             String atmsSn, String vendSn, Map<String, String> echoBody) {
        String vnd = nvl(vendorCd);
        Map<String, String> msgMap = new LinkedHashMap<>();
        msgMap.put(KEY_VENDOR, vnd);
        msgMap.put(KEY_ATMS_SN, nvl(atmsSn));
        msgMap.put(KEY_VEND_SN, nvl(vendSn));
        msgMap.put(KEY_RESPONSE_CD, responseCd == null ? "0000" : responseCd);
        if (echoBody != null) {
            msgMap.putAll(echoBody);
        }
        return send(GlobalIdGenerator.generate(ORG_CODE), msgCode, "INBOUND_RESPONSE",
                null, null, vnd, nvl(atmsSn), nvl(vendSn), toJson(msgMap), null);
    }

    /** 결번요청(IQ_04001110) — 저장된 원 송신전문(JSON) 그대로 재전송. */
    @Transactional
    public ExternalIfResult resend(NetSendData origin) {
        return send(GlobalIdGenerator.generate(ORG_CODE), origin.getTrCode(), "MISSING_RESEND",
                origin.getTermGroupId(), origin.getTermId(),
                nvl(origin.getProviderCd()), nvl(origin.getAtmsSn()), nvl(origin.getVendSn()),
                origin.getSendMsg(), null);
    }

    /**
     * 수신(IN) 전문 이력 등록 — 수신등록 일련번호 채번("004"/netrecv_reg) 후
     * 업체 통보 일련번호(vend_sn) 동기화 (구 SP_UPDATE_SEQNO).
     */
    @Transactional
    public void recordInbound(String msgCode, String vendorCd, String atmsSn, String vendSn, String recvMsg) {
        String vnd = nvl(vendorCd);
        String registSn = vnd.isBlank() ? "0000000" : seqNoService.nextFormatted("004", vnd, "netrecv_reg");
        if (!vnd.isBlank() && vendSn != null && !vendSn.isBlank()) {
            try {
                seqNoService.syncVendorSeqNo(vnd, Long.parseLong(vendSn.trim()));
            } catch (NumberFormatException e) {
                log.warn("[inbound] vend_sn 숫자 아님 — 동기화 생략: {}", vendSn);
            }
        }
        LocalDateTime now = LocalDateTime.now();
        recvMapper.insert(NetRecvData.builder()
                .globalId(GlobalIdGenerator.generate(ORG_CODE))
                .trCode(msgCode)
                .recvMsg(recvMsg)
                .resultCd("0000")
                .resultMsg("수신등록")
                .recvDate(now.format(DF))
                .recvTime(now.format(TF))
                .providerCd(vnd)
                .atmsSn(registSn)
                .vendSn(nvl(vendSn))
                .build());
    }

    // ---- 내부 공통 ----

    private ExternalIfResult send(String globalId, String msgCode, String ifType,
                                  String termGroupId, String termId,
                                  String vendorCd, String atmsSn, String vendSn,
                                  String sendMsg, Long faultSeq) {
        LocalDateTime sentAt = LocalDateTime.now();
        GatewayResult gr = gateway.send(globalId, msgCode, ifType, sendMsg);
        LocalDateTime recvAt = LocalDateTime.now();

        NetSendData send = NetSendData.builder()
                .globalId(globalId)
                .trCode(msgCode)
                .ifType(ifType)
                .termGroupId(termGroupId)
                .termId(termId)
                .sendMsg(sendMsg)
                .resultCd(gr.getResultCd())
                .resultMsg(gr.getResultMsg())
                .sendDate(sentAt.format(DF))
                .sendTime(sentAt.format(TF))
                .providerCd(vendorCd)
                .atmsSn(atmsSn)
                .vendSn(vendSn)
                .rawMsg(gr.getRawSendMsg())
                .build();
        sendMapper.insert(send);

        recvMapper.insert(NetRecvData.builder()
                .globalId(globalId)
                .trCode(msgCode)
                .sendSeq(send.getSendSeq())
                .recvMsg(gr.getRecvMsg())
                .resultCd(gr.getResultCd())
                .resultMsg(gr.getResultMsg())
                .recvDate(recvAt.format(DF))
                .recvTime(recvAt.format(TF))
                .providerCd(vendorCd)
                .atmsSn(atmsSn)
                .vendSn(vendSn)
                .build());

        return ExternalIfResult.builder()
                .globalId(globalId)
                .resultCd(gr.getResultCd())
                .resultMsg(gr.getResultMsg())
                .sendSeq(send.getSendSeq())
                .faultSeq(faultSeq)
                .build();
    }

    public static String nvl(String s) {
        return s == null ? "" : s;
    }

    /** 순서 보존 Map 을 JSON 문자열로 직렬화 (값은 전부 코드/문자열). */
    public static String toJson(Map<String, String> m) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> e : m.entrySet()) {
            if (!first) sb.append(',');
            sb.append('"').append(jsonEscape(e.getKey())).append("\":\"")
              .append(jsonEscape(e.getValue())).append('"');
            first = false;
        }
        return sb.append('}').toString();
    }

    private static String jsonEscape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:   sb.append(c);
            }
        }
        return sb.toString();
    }
}
