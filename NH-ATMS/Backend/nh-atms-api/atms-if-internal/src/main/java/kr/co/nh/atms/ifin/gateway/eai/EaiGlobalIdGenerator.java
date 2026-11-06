package kr.co.nh.atms.ifin.gateway.eai;

import kr.co.nh.atms.ifin.mapper.GwInterfaceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * AS-IS 규약 GlobalID(32자) 생성기.
 *
 * 레이아웃: 전문생성일(6;yyMMdd) + 시분초(6;HHmmss) + 요청시스템(8;hostname) + 일련번호(10;lpad0) + 진행번호(2;"00")
 * 일련번호는 7자리 채번(구 SP_GET_SEQNO 대응 → Oracle 시퀀스 SEQ_EAI_MSG_SEQNO)이며
 * MSG_SEQNO = globalId.substring(23, 30) 추출 매칭이 성립한다.
 * (10자리 자리 중 앞 3자리는 '0' 패딩, 23~29 인덱스가 7자리 일련번호)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EaiGlobalIdGenerator {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyMMdd");
    private static final DateTimeFormatter TF = DateTimeFormatter.ofPattern("HHmmss");

    private final GwInterfaceMapper gwInterfaceMapper;
    private final EaiGatewayProperties props;

    /** DB 시퀀스 채번 실패 시 폴백용 (개발/로컬 환경) */
    private final AtomicLong fallbackSeq = new AtomicLong(0);

    /** 신규 일련번호(7자리)로 GlobalID 생성 */
    public String generate() {
        return generate(nextSeqNo());
    }

    /** 지정 일련번호(7자리)로 GlobalID 생성 (SMS: SMS_SEQ 사용) */
    public String generate(String seqNo7) {
        LocalDateTime now = LocalDateTime.now();
        return now.format(DF) + now.format(TF) + hostname8() + padLeft(seqNo7, 10, '0') + "00";
    }

    /** 일련번호(7자리) 채번. 구 SP_GET_SEQNO('010','0','0') → Oracle 시퀀스 */
    public String nextSeqNo() {
        try {
            String seq = gwInterfaceMapper.selectNextMsgSeqNo();
            if (seq != null && !seq.isBlank()) {
                return seq;
            }
        } catch (Exception e) {
            log.warn("[EAI] MSG_SEQNO 시퀀스 채번 실패 - 메모리 폴백 사용: {}", e.getMessage());
        }
        return String.format("%07d", fallbackSeq.incrementAndGet() % 10_000_000);
    }

    /** GlobalID 에서 MSG_SEQNO(7자리) 추출 (AS-IS substring(23, 30) 규약) */
    public static String msgSeqNo(String globalId) {
        if (globalId == null || globalId.length() < 30) {
            return "";
        }
        return globalId.substring(23, 30);
    }

    /**
     * 요청시스템명(8자).
     * AS-IS: env HOSTNAME 이 atmap* 이 아니면 atmap01t 폴백 후 대문자화.
     * 설정(internal.gateway.eai.hostname)으로 재정의 가능.
     */
    public String hostname8() {
        String hostname = props.getHostname();
        if (hostname == null || hostname.isBlank()) {
            hostname = System.getenv("HOSTNAME");
            if (hostname == null || !hostname.startsWith("atmap")) {
                hostname = "atmap01t";
            }
        }
        hostname = hostname.toUpperCase();
        if (hostname.length() > 8) {
            hostname = hostname.substring(0, 8);
        }
        while (hostname.length() < 8) {
            hostname += "0";
        }
        return hostname;
    }

    /** 송신노드내용(5자). AS-IS: 개발(hostname 끝 T) DATM+7번째자리, 운영 PATM+7번째자리 */
    public String sendNodeCntn() {
        String hostname = hostname8();
        char node = hostname.length() > 6 ? hostname.charAt(6) : '1';
        return (hostname.endsWith("T") ? "DATM" : "PATM") + node;
    }

    private static String padLeft(String s, int len, char c) {
        if (s == null) s = "";
        if (s.length() >= len) return s.substring(s.length() - len);
        StringBuilder sb = new StringBuilder();
        for (int i = s.length(); i < len; i++) sb.append(c);
        return sb.append(s).toString();
    }
}
