package kr.co.nh.atms.ifin.gateway;

import kr.co.nh.atms.ifin.constants.InternalIfConstants;
import kr.co.nh.atms.ifin.dto.DrawerCheckResponse;
import kr.co.nh.atms.ifin.dto.FaultMessengerRequest;
import kr.co.nh.atms.ifin.dto.GatewayResult;
import kr.co.nh.atms.ifin.dto.TellerAmount;
import kr.co.nh.atms.ifin.entity.GwInterface;
import kr.co.nh.atms.ifin.entity.SmsSendData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 대내 연계 Mock 구현 — 실제 소켓/DB2DB 통신 없이 더미 응답을 생성한다.
 * T_GW_INTERFACE 2-phase 기록(송신 '0' → 응답 '1')은 실 구현과 동일하게 수행한다.
 * 빈 등록은 {@link GatewayConfig} 가 담당하며, internal.gateway.mode=eai 시 실 게이트웨이로 대체된다.
 */
@Slf4j
@RequiredArgsConstructor
public class MockInternalGateway implements InternalGateway {

    private static final String OK = InternalIfConstants.RESPONSE_OK;

    private final GwInterfaceRecorder recorder;

    @Override
    public DrawerCheckResponse drawerCheck(String globalId, String termGroupId, String termId,
                                           String baseDate, List<String> tellerNos) {
        GwInterface gw = recorder.begin(globalId, InternalIfConstants.DRAWER_CHECK_RESP_SVC_CD,
                "DRAWER_CHECK", termGroupId, termId,
                "[MOCK] baseDate=" + baseDate + ",tellers=" + tellerNos);

        List<TellerAmount> tellerAmounts = new ArrayList<>();
        long totalAmount = 0;
        for (String tellerNo : tellerNos) {
            long amt = ThreadLocalRandom.current().nextLong(100, 2000) * 50_000L;
            totalAmount += amt;
            tellerAmounts.add(TellerAmount.builder()
                    .tellerNo(tellerNo)
                    .tellerAmt(amt)
                    .selfBranchAmt(0L)
                    .otherBranchAmt(0L)
                    .build());
        }
        log.info("[MOCK-IF] 시재조회 globalId={} term={}/{} base={} tellers={} → amt={}",
                globalId, termGroupId, termId, baseDate, tellerNos, totalAmount);

        recorder.complete(gw.getMsgSeqNo(), "[MOCK] totalAmount=" + totalAmount, OK, "정상");

        return DrawerCheckResponse.builder()
                .globalId(globalId)
                .resultCd(OK)
                .resultMsg("정상")
                .termGroupId(termGroupId)
                .termId(termId)
                .totalCashCount((long) tellerAmounts.size())
                .totalCashAmount(totalAmount)
                .tellerAmounts(tellerAmounts)
                .gwSeq(gw.getGwSeq())
                .build();
    }

    @Override
    public GatewayResult sendFaultSms(String globalId, SmsSendData sms) {
        GwInterface gw = recorder.begin(globalId, InternalIfConstants.SMS_SEND_SVC_CD,
                "FAULT_SMS", sms.getTermGroupId(), sms.getTermId(),
                "[MOCK] to=" + sms.getRecvPhone() + ",msg=" + sms.getMsgContent());
        log.info("[MOCK-IF] 장애SMS globalId={} to={} msg={}", globalId, sms.getRecvPhone(), sms.getMsgContent());
        recorder.complete(gw.getMsgSeqNo(), "[MOCK] OK", OK, "정상");
        return GatewayResult.builder().resultCd(OK).resultMsg("정상").gwSeq(gw.getGwSeq()).build();
    }

    @Override
    public GatewayResult sendFaultMessenger(String globalId, FaultMessengerRequest req) {
        GwInterface gw = recorder.begin(globalId, "ATMBYDD10100",
                "FAULT_MESSENGER", req.getTermGroupId(), req.getTermId(),
                "[MOCK] errCd=" + req.getErrCd() + ",to=" + req.getRecvUserId());
        log.info("[MOCK-IF] 장애메신저 globalId={} to={} msg={}", globalId, req.getRecvUserId(), req.getMsgContent());
        recorder.complete(gw.getMsgSeqNo(), req.getMsgContent(), OK, "정상");
        return GatewayResult.builder().resultCd(OK).resultMsg("정상").gwSeq(gw.getGwSeq()).build();
    }
}
