package kr.co.nh.atms.ifin.gateway.eai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.annotation.PostConstruct;
import kr.co.nh.atms.ifin.constants.InternalIfConstants;
import kr.co.nh.atms.ifin.dto.DrawerCheckResponse;
import kr.co.nh.atms.ifin.dto.FaultMessengerRequest;
import kr.co.nh.atms.ifin.dto.GatewayResult;
import kr.co.nh.atms.ifin.dto.TellerAmount;
import kr.co.nh.atms.ifin.entity.GwInterface;
import kr.co.nh.atms.ifin.entity.SmsSendData;
import kr.co.nh.atms.ifin.gateway.GwInterfaceRecorder;
import kr.co.nh.atms.ifin.gateway.InternalGateway;
import kr.co.nh.atms.ifin.gateway.eai.util.Hex;
import kr.co.nh.atms.ifin.gateway.eai.util.Json;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 은행 EAI 실연동 게이트웨이 (TCP).
 * 구 atmi.lib Gateway 의 send worker(상시연결 keep-connect, 큐 기반 송신, 동기 응답 수신) 이식.
 * - NIO Selector 기반 논블로킹 IO 는 블로킹 Socket + 타임아웃으로 단순화 (프로토콜 동일:
 *   표준전문길이(8) 프리픽스 프레이밍, EUC-KR 고정길이 전문, 바디 AES 암복호화)
 * - T_GW_INTERFACE 2-phase: 송신 시 INSERT(GW_FLAG '0') → 응답 수신 시 UPDATE(GW_FLAG '1', MSG_SEQNO 매칭)
 * - internal.gateway.mode=eai 일 때만 활성화 (기본 Mock)
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "internal.gateway.mode", havingValue = "eai")
public class EaiInternalGateway implements InternalGateway, SmartLifecycle {

    private static final int STND_MSG_LEN_FIELD = 8; // 표준전문길이 필드 (구 FI_STND_MSG_LEN[1])

    private final EaiGatewayProperties props;
    private final EaiCipher cipher;
    private final EaiMessageBuilder messageBuilder;
    private final EaiGlobalIdGenerator idGen;
    private final GwInterfaceRecorder recorder;

    private final BlockingQueue<EaiCall> queueSnd = new LinkedBlockingQueue<>();
    private final List<Thread> workers = new ArrayList<>();
    private volatile boolean running;

    /** 송수신 교환 단위 (구 Gateway.QueueSndItem) */
    static class EaiCall {
        final JsonObject sndJpacket;
        final byte[] sndPacket;
        final CompletableFuture<EaiCall> future = new CompletableFuture<>();
        byte[] rcvPacket;
        JsonObject rcvJpacket;
        boolean succ;
        String emsg;

        EaiCall(JsonObject sndJpacket, byte[] sndPacket) {
            this.sndJpacket = sndJpacket;
            this.sndPacket = sndPacket;
        }
    }

    /* ------------------------------------------------------------------ */
    /* 전문 스펙 로딩 (atms-file MifConfig 클래스패스 로딩 패턴)             */
    /* ------------------------------------------------------------------ */

    @PostConstruct
    void loadSpec() {
        try {
            ClassPathResource resource = new ClassPathResource(props.getSpecLocation());
            try (InputStream in = resource.getInputStream()) {
                String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                Converter.IIF.load(props.getInterfaceName(), json);
            }
            log.info("[EAI] IIF spec loaded: name={}, charset={}, location={}",
                    Converter.IIF.name(), Converter.IIF.charset(), props.getSpecLocation());
        } catch (Exception e) {
            log.error("[EAI] IIF spec load 실패: location={}", props.getSpecLocation(), e);
        }
    }

    private Charset charset() {
        Charset cs = Converter.IIF.charset();
        return cs != null ? cs : Charset.forName("euc-kr");
    }

    /* ------------------------------------------------------------------ */
    /* SmartLifecycle : 송신 워커 스레드 관리 (구 Gateway.bgn/end)          */
    /* ------------------------------------------------------------------ */

    @Override
    public void start() {
        if (running) return;
        running = true;
        for (int i = 0; i < Math.max(1, props.getSendWorker()); i++) {
            final int wid = i;
            Thread t = new Thread(() -> sendProcess(wid), String.format("[eai-send-worker][%d]", wid));
            t.setDaemon(true);
            t.start();
            workers.add(t);
        }
        log.info("[EAI][bgn] send worker {}개 기동 - {}:{} keep-connect={}",
                workers.size(), props.getIp(), props.getPort(), props.isKeepConnect());
    }

    @Override
    public void stop() {
        running = false;
        for (Thread t : workers) {
            t.interrupt();
        }
        workers.clear();
        log.info("[EAI][end]");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    /* ------------------------------------------------------------------ */
    /* 송신 워커 (구 Gateway.send_process 이식)                             */
    /* ------------------------------------------------------------------ */

    private void sendProcess(int wid) {
        Socket socket = null;
        long lastConTry = 0;
        try {
            while (running) {
                EaiCall item = queueSnd.poll(1000, TimeUnit.MILLISECONDS);
                long lnow = System.currentTimeMillis();
                if (item == null) {
                    // 아이템이 없어도 keep-connect 면 재접속 시도 (구 AS-IS 30초 간격)
                    if (props.isKeepConnect() && socket == null
                            && lnow - lastConTry >= props.getReconnectIntervalMs()) {
                        lastConTry = lnow;
                        socket = tryConnect(wid, null);
                    }
                    continue;
                }
                if (socket == null) {
                    lastConTry = lnow;
                    socket = tryConnect(wid, item);
                }
                try {
                    if (socket == null) {
                        if (item.emsg == null) item.emsg = "채널없음";
                        log.info("[wid:{}][snd_worker][eai][{}:{}][end][{}]", wid, props.getIp(), props.getPort(), item.emsg);
                        continue;
                    }
                    // 송신 (바디 암호화 + 표준전문길이 프리픽스는 전문 자체에 포함)
                    byte[] encSndPacket = cipher.packetEncrypt(item.sndPacket);
                    if (log.isTraceEnabled()) {
                        log.trace("[wid:{}][snd][dump]\r\n{}", wid, Hex.dump(encSndPacket));
                    }
                    try {
                        OutputStream out = socket.getOutputStream();
                        out.write(encSndPacket);
                        out.flush();
                    } catch (IOException e) {
                        socket = closeQuietly(socket);
                        item.emsg = String.format("송신실패 : %s", nvl(e.getMessage()));
                        log.error("[wid:{}][snd_worker][eai][end][{}]", wid, item.emsg);
                        continue;
                    }
                    // 수신 : 표준전문길이(8) → 잔여 전문 (구 recvn 프레이밍)
                    try {
                        InputStream in = socket.getInputStream();
                        byte[] blen = readn(in, STND_MSG_LEN_FIELD);
                        if (blen == null) {
                            socket = closeQuietly(socket);
                            item.emsg = "수신실패 : 원격지 종료 : LEN";
                            log.info("[wid:{}][snd_worker][eai][end][{}]", wid, item.emsg);
                            continue;
                        }
                        int len = Integer.parseInt(new String(blen, charset()).trim());
                        byte[] rest = readn(in, len);
                        if (rest == null) {
                            socket = closeQuietly(socket);
                            item.emsg = "수신실패 : 원격지 종료";
                            log.info("[wid:{}][snd_worker][eai][end][{}]", wid, item.emsg);
                            continue;
                        }
                        byte[] encRcvPacket = new byte[blen.length + rest.length];
                        System.arraycopy(blen, 0, encRcvPacket, 0, blen.length);
                        System.arraycopy(rest, 0, encRcvPacket, blen.length, rest.length);
                        if (log.isTraceEnabled()) {
                            log.trace("[wid:{}][rcv][dump]\r\n{}", wid, Hex.dump(encRcvPacket));
                        }
                        item.rcvPacket = cipher.packetDecrypt(encRcvPacket);
                        item.rcvJpacket = Converter.IIF.toJson(item.rcvPacket);
                        item.succ = true;
                    } catch (IOException | NumberFormatException e) {
                        socket = closeQuietly(socket);
                        item.emsg = String.format("수신실패 : %s", nvl(e.getMessage()));
                        log.error("[wid:{}][snd_worker][eai][end][{}]", wid, item.emsg);
                    }
                } finally {
                    if (!props.isKeepConnect()) {
                        socket = closeQuietly(socket);
                    }
                    item.future.complete(item);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            closeQuietly(socket);
        }
    }

    private Socket tryConnect(int wid, EaiCall item) {
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(props.getIp(), props.getPort()), props.getConnectTimeoutMs());
            socket.setKeepAlive(true);
            socket.setSoTimeout(props.getReadTimeoutMs());
            log.info("[wid:{}][snd_worker][eai][{}:{}][bgn]", wid, props.getIp(), props.getPort());
            return socket;
        } catch (IOException e) {
            String emsg = String.format("연결실패 : %s", nvl(e.getMessage()));
            if (item != null) item.emsg = emsg;
            log.warn("[wid:{}][snd_worker][eai][{}:{}][try][{}]", wid, props.getIp(), props.getPort(), emsg);
            return null;
        }
    }

    /** n 바이트 수신 (구 Gateway.recvn). 원격지 종료 시 null */
    private static byte[] readn(InputStream in, int n) throws IOException {
        byte[] buf = new byte[n];
        int off = 0;
        while (off < n) {
            int io = in.read(buf, off, n - off);
            if (io < 0) {
                return null;
            }
            off += io;
        }
        return buf;
    }

    private static Socket closeQuietly(Socket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignore) {
            }
        }
        return null;
    }

    private static String nvl(String v) {
        return v == null ? "null" : v;
    }

    /* ------------------------------------------------------------------ */
    /* 전문 송신 공통 (전문화 + 2-phase 기록 + 응답 매칭)                    */
    /* ------------------------------------------------------------------ */

    private EaiCall exchange(JsonObject message, String trCode, String ifType,
                             String termGroupId, String termId, GwInterface[] gwOut) {
        // 구 Gateway.send_process : data_length = 27 + body spec tlen 재계산
        JsonObject jhead = message.getAsJsonObject("head");
        String iid = Json.strt(jhead, "eai_svc_cd");
        JsonObject spec = Converter.IIF.body(iid);
        jhead.addProperty("data_length", 27 + Json.int32(spec, Converter.TLEN_KEY));

        byte[] sndPacket = Converter.IIF.toPacket(message);
        EaiCall call = new EaiCall(message, sndPacket);
        if (sndPacket == null) {
            call.emsg = "전문 생성 실패 (IIF spec 미로딩)";
            return call;
        }

        // Phase 1 : 송신 시 INSERT (GW_FLAG '0')
        String globalId = Json.strt(jhead, "global_id");
        GwInterface gw = recorder.begin(globalId, trCode, ifType, termGroupId, termId,
                new String(sndPacket, charset()));
        gwOut[0] = gw;

        if (!running) {
            call.emsg = "게이트웨이 미기동";
            return call;
        }
        queueSnd.offer(call);
        try {
            // 구 AS-IS waitor.wait(1000*30) 대응
            call.future.get(props.getReplyTimeoutMs(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            call.emsg = "응답 타임아웃";
        } catch (Exception e) {
            call.emsg = nvl(e.getMessage());
        }

        // Phase 2 : 응답 수신 시 UPDATE (GW_FLAG '1', MSG_SEQNO 매칭 - AS-IS substring(23,30))
        if (call.succ && call.rcvJpacket != null) {
            JsonObject rcvHead = Json.obj(call.rcvJpacket, "head");
            String rcvGlobalId = Json.strt(rcvHead, "global_id");
            String msgSeqNo = EaiGlobalIdGenerator.msgSeqNo(
                    rcvGlobalId.length() >= 30 ? rcvGlobalId : globalId);
            String resultCd = mapResultCd(rcvHead);
            String resultMsg = InternalIfConstants.RESPONSE_OK.equals(resultCd)
                    ? "정상" : Json.strt(rcvHead, "msg_cd", "오류");
            recorder.complete(msgSeqNo, new String(call.rcvPacket, charset()), resultCd, resultMsg);
        }
        return call;
    }

    /**
     * 응답코드 매핑 (AS-IS ATMIConstants 복원).
     * msg_flag(전문결과코드) '0'/공백 → 0000, 그 외 → 0099.
     */
    private String mapResultCd(JsonObject rcvHead) {
        String msgFlag = Json.strt(rcvHead, "msg_flag");
        if (msgFlag.isEmpty() || "0".equals(msgFlag)) {
            return InternalIfConstants.RESPONSE_OK;
        }
        return InternalIfConstants.RESPONSE_ERROR;
    }

    /** 통신 결과 → 응답코드 (무응답 0098 / 오류 0099) */
    private String failResultCd(EaiCall call) {
        if (call.emsg != null && call.emsg.startsWith("응답 타임아웃")) {
            return InternalIfConstants.RESPONSE_MISSING;
        }
        return InternalIfConstants.RESPONSE_ERROR;
    }

    /* ------------------------------------------------------------------ */
    /* InternalGateway 구현                                                */
    /* ------------------------------------------------------------------ */

    @Override
    public DrawerCheckResponse drawerCheck(String globalId, String termGroupId, String termId,
                                           String baseDate, List<String> tellerNos) {
        JsonObject message = messageBuilder.drawerCheckMessage(globalId, termGroupId, tellerNos);
        GwInterface[] gwOut = new GwInterface[1];
        EaiCall call = exchange(message, InternalIfConstants.DRAWER_CHECK_RESP_SVC_CD,
                "DRAWER_CHECK", termGroupId, termId, gwOut);

        DrawerCheckResponse.DrawerCheckResponseBuilder res = DrawerCheckResponse.builder()
                .globalId(globalId)
                .termGroupId(termGroupId)
                .termId(termId)
                .gwSeq(gwOut[0] != null ? gwOut[0].getGwSeq() : null);

        if (!call.succ || call.rcvJpacket == null) {
            log.warn("[EAI] 시재조회 실패 globalId={} emsg={}", globalId, call.emsg);
            return res.resultCd(failResultCd(call)).resultMsg(nvl(call.emsg)).build();
        }

        // 응답 파싱 : 텔러별/자점/타점 금액 (구 Gateway ATMONST10100 s_amt 1710byte = 57byte*30 반복)
        JsonObject rcvHead = Json.obj(call.rcvJpacket, "head");
        JsonObject rcvBody = Json.obj(call.rcvJpacket, "body");
        int occur = parseIntSafe(Json.strt(rcvBody, "occur"));
        String sAmt = Json.str(rcvBody, "s_amt");

        List<TellerAmount> tellerAmounts = new ArrayList<>();
        long totalAmount = 0;
        for (int i = 0, off; sAmt.length() == 1710 && i < occur; i++) {
            off = i * 57;
            String telrNo   = sAmt.substring(off, off + 3);  off += 3;   // 텔러번호
            String telrAmt  = sAmt.substring(off, off + 18); off += 18;  // 텔러시재금액
            String slfbrAmt = sAmt.substring(off, off + 18); off += 18;  // 자점금액
            String othbrAmt = sAmt.substring(off, off + 18);             // 타점금액

            long amt = parseLongSafe(telrAmt);
            totalAmount += amt;
            tellerAmounts.add(TellerAmount.builder()
                    .tellerNo(telrNo.trim())
                    .tellerAmt(amt)
                    .selfBranchAmt(parseLongSafe(slfbrAmt))
                    .otherBranchAmt(parseLongSafe(othbrAmt))
                    .build());
        }

        String resultCd = mapResultCd(rcvHead);
        if (InternalIfConstants.RESPONSE_OK.equals(resultCd) && occur == 0) {
            resultCd = InternalIfConstants.RESPONSE_NO_DATA; // 조회자료없음
        }
        return res.resultCd(resultCd)
                .resultMsg(InternalIfConstants.RESPONSE_OK.equals(resultCd) ? "정상"
                        : InternalIfConstants.RESPONSE_NO_DATA.equals(resultCd) ? "조회자료없음"
                        : Json.strt(rcvHead, "msg_cd", "오류"))
                .totalCashCount((long) tellerAmounts.size())
                .totalCashAmount(totalAmount)
                .tellerAmounts(tellerAmounts)
                .build();
    }

    @Override
    public GatewayResult sendFaultSms(String globalId, SmsSendData sms) {
        JsonObject message = messageBuilder.smsMessage(globalId, sms);
        GwInterface[] gwOut = new GwInterface[1];
        EaiCall call = exchange(message, InternalIfConstants.SMS_SEND_SVC_CD,
                "FAULT_SMS", sms.getTermGroupId(), sms.getTermId(), gwOut);

        Long gwSeq = gwOut[0] != null ? gwOut[0].getGwSeq() : null;
        if (!call.succ || call.rcvJpacket == null) {
            log.warn("[EAI] 장애SMS 송신 실패 globalId={} emsg={}", globalId, call.emsg);
            return GatewayResult.builder()
                    .resultCd(failResultCd(call)).resultMsg(nvl(call.emsg)).gwSeq(gwSeq).build();
        }
        JsonObject rcvHead = Json.obj(call.rcvJpacket, "head");
        String resultCd = mapResultCd(rcvHead);
        return GatewayResult.builder()
                .resultCd(resultCd)
                .resultMsg(InternalIfConstants.RESPONSE_OK.equals(resultCd)
                        ? "정상" : Json.strt(rcvHead, "msg_cd", "오류"))
                .gwSeq(gwSeq)
                .build();
    }

    /**
     * 장애 메신저 통보 (ATMBYDD10100).
     * AS-IS 는 batch.sh(DB2DB 배치 에이전트) 실행 방식으로 온라인 전문이 아니며 인프라 의존이라 스킵.
     * 송신 이력만 기록하고 0098(결번) 을 반환한다. (MIGRATION-NOTES.md 참고)
     */
    @Override
    public GatewayResult sendFaultMessenger(String globalId, FaultMessengerRequest req) {
        GwInterface gw = recorder.begin(globalId, "ATMBYDD10100", "FAULT_MESSENGER",
                req.getTermGroupId(), req.getTermId(),
                "errCd=" + req.getErrCd() + ",to=" + req.getRecvUserId() + ",msg=" + req.getMsgContent());
        log.warn("[EAI] 장애메신저(ATMBYDD10100)는 배치모드(batch.sh) 미구현으로 스킵 - globalId={}", globalId);
        return GatewayResult.builder()
                .resultCd(InternalIfConstants.RESPONSE_MISSING)
                .resultMsg("ATMBYDD10100 배치모드 미구현(스킵)")
                .gwSeq(gw.getGwSeq())
                .build();
    }

    private static int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private static long parseLongSafe(String s) {
        try {
            return Long.parseLong(s.trim());
        } catch (Exception e) {
            return 0L;
        }
    }
}
