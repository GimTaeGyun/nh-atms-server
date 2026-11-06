package kr.co.nh.atms.ifin.service;

import kr.co.nh.atms.ifin.constants.InternalIfConstants;
import kr.co.nh.atms.ifin.dto.DrawerCheckRequest;
import kr.co.nh.atms.ifin.dto.DrawerCheckResponse;
import kr.co.nh.atms.ifin.dto.FaultMessengerRequest;
import kr.co.nh.atms.ifin.dto.FaultSmsRequest;
import kr.co.nh.atms.ifin.dto.GatewayResult;
import kr.co.nh.atms.ifin.dto.IfSendResult;
import kr.co.nh.atms.ifin.entity.SmsSendData;
import kr.co.nh.atms.ifin.gateway.InternalGateway;
import kr.co.nh.atms.ifin.gateway.eai.EaiGlobalIdGenerator;
import kr.co.nh.atms.ifin.mapper.SmsSendDataMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 대내(atmi) 인터페이스 서비스.
 * 시재조회/장애SMS/장애메신저 요청을 받아 게이트웨이로 위임한다.
 * GlobalID 는 AS-IS 규약(yyMMdd+HHmmss+hostname8+일련10+"00")으로 채번하며,
 * T_GW_INTERFACE 2-phase 기록(송신 '0' → 응답 '1')은 게이트웨이 계층(GwInterfaceRecorder)이 담당한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InternalIfService {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TF = DateTimeFormatter.ofPattern("HHmmss");
    private static final String OK = InternalIfConstants.RESPONSE_OK;

    private final InternalGateway gateway;
    private final SmsSendDataMapper smsMapper;
    private final EaiGlobalIdGenerator globalIdGenerator;

    /** 시재 조회 (ATMONST10100) */
    @Transactional
    public DrawerCheckResponse drawerCheck(DrawerCheckRequest req) {
        String globalId = globalIdGenerator.generate();
        String baseDate = (req.getBaseDate() == null || req.getBaseDate().isBlank())
                ? LocalDateTime.now().format(DF) : req.getBaseDate();
        // AS-IS teller_no[] 다건 조회. 미지정 시 termId(단말/텔러번호) 1건
        List<String> tellerNos = (req.getTellerNos() == null || req.getTellerNos().isEmpty())
                ? List.of(req.getTermId()) : req.getTellerNos();

        return gateway.drawerCheck(globalId, req.getTermGroupId(), req.getTermId(), baseDate, tellerNos);
    }

    /** 장애 SMS 발송 (SMSONSS10200) */
    @Transactional
    public IfSendResult faultSms(FaultSmsRequest req) {
        SmsSendData sms = SmsSendData.builder()
                .termGroupId(req.getTermGroupId())
                .termId(req.getTermId())
                .recvPhone(req.getRecvPhone())
                .sendPhone(req.getSendPhone())
                .msgContent(req.getMsgContent())
                .errCd(req.getErrCd())
                .sendStatus("W")
                .build();
        smsMapper.insert(sms);

        // AS-IS 규약: SMS 는 SMS 일련번호(구 SMS_SNO → SMS_SEQ)를 GlobalID 일련번호로 사용
        String globalId = globalIdGenerator.generate(
                String.format("%07d", sms.getSmsSeq() % 10_000_000));
        sms.setGlobalId(globalId);

        GatewayResult gr = gateway.sendFaultSms(globalId, sms);
        boolean ok = OK.equals(gr.getResultCd());

        LocalDateTime now = LocalDateTime.now();
        sms.setSendStatus(ok ? "S" : "F");
        sms.setSendDate(now.format(DF));
        sms.setSendTime(now.format(TF));
        sms.setResultCd(gr.getResultCd());
        sms.setResultMsg(ok ? "정상" : "발송실패");
        smsMapper.updateResult(sms);

        return IfSendResult.builder()
                .globalId(globalId)
                .resultCd(gr.getResultCd())
                .resultMsg(sms.getResultMsg())
                .refSeq(sms.getSmsSeq())
                .build();
    }

    /** 장애 메신저 통보 (ATMBYDD10100, DB2DB 배치) */
    @Transactional
    public IfSendResult faultMessenger(FaultMessengerRequest req) {
        String globalId = globalIdGenerator.generate();
        GatewayResult gr = gateway.sendFaultMessenger(globalId, req);

        return IfSendResult.builder()
                .globalId(globalId)
                .resultCd(gr.getResultCd())
                .resultMsg(gr.getResultMsg())
                .refSeq(gr.getGwSeq())
                .build();
    }
}
