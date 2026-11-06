package kr.co.nh.atms.ifin.gateway;

import kr.co.nh.atms.ifin.entity.GwInterface;
import kr.co.nh.atms.ifin.gateway.eai.EaiGlobalIdGenerator;
import kr.co.nh.atms.ifin.mapper.GwInterfaceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * T_GW_INTERFACE 2-phase 기록기 (AS-IS 복원).
 *  - 송신 시 INSERT (GW_FLAG '0', SEND_DATA, MSG_SEQNO)
 *  - 응답 수신 시 UPDATE (GW_FLAG '1', RECV_DATA, RESULT_CD) : MSG_SEQNO 매칭
 * AS-IS 는 별도 SqlSession 즉시 commit 이었으므로 REQUIRES_NEW 로 송신 이력을 즉시 확정한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GwInterfaceRecorder {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TF = DateTimeFormatter.ofPattern("HHmmss");

    private final GwInterfaceMapper gwInterfaceMapper;

    /** Phase 1 : 송신 시 INSERT (GW_FLAG '0') */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public GwInterface begin(String globalId, String trCode, String ifType,
                             String termGroupId, String termId, String sendData) {
        LocalDateTime now = LocalDateTime.now();
        GwInterface gw = GwInterface.builder()
                .globalId(globalId)
                .msgSeqNo(EaiGlobalIdGenerator.msgSeqNo(globalId))
                .gwFlag("0")
                .trCode(trCode)
                .ifType(ifType)
                .termGroupId(termGroupId)
                .termId(termId)
                .sendData(sendData)
                .sendDate(now.format(DF))
                .sendTime(now.format(TF))
                .build();
        gwInterfaceMapper.insert(gw);
        return gw;
    }

    /** Phase 2 : 응답 수신 시 UPDATE (GW_FLAG '1', MSG_SEQNO 매칭) */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(String msgSeqNo, String recvData, String resultCd, String resultMsg) {
        int updated = gwInterfaceMapper.updateRecvByMsgSeqNo(msgSeqNo, recvData, resultCd, resultMsg);
        if (updated == 0) {
            log.warn("[GW] 응답 매칭 실패 - MSG_SEQNO={} 에 해당하는 송신 이력이 없습니다.", msgSeqNo);
        }
    }
}
