package kr.co.nh.atms.ifin.gateway;

import kr.co.nh.atms.ifin.dto.DrawerCheckResponse;
import kr.co.nh.atms.ifin.dto.FaultMessengerRequest;
import kr.co.nh.atms.ifin.dto.GatewayResult;
import kr.co.nh.atms.ifin.entity.SmsSendData;

import java.util.List;

/**
 * 대내(계정계/EAI/SMS) 연계 게이트웨이.
 * 실제 통신(TCP Socket / DB2DB)과 T_GW_INTERFACE 2-phase 기록(송신 INSERT '0' → 응답 UPDATE '1')은
 * 구현체가 담당한다. 기본은 {@link MockInternalGateway}, internal.gateway.mode=eai 시
 * {@link kr.co.nh.atms.ifin.gateway.eai.EaiInternalGateway} 가 활성화된다.
 */
public interface InternalGateway {

    /**
     * 시재 조회 (TCP Socket, ATMONST10100).
     *
     * @param globalId  AS-IS 규약 32자 GlobalID
     * @param tellerNos 텔러번호(3자) 목록 - AS-IS teller_no[] occur 반복
     */
    DrawerCheckResponse drawerCheck(String globalId, String termGroupId, String termId,
                                    String baseDate, List<String> tellerNos);

    /** 장애 SMS 발송 (TCP Socket, SMSONSS10200) */
    GatewayResult sendFaultSms(String globalId, SmsSendData sms);

    /** 장애 메신저 통보 (DB2DB 배치, ATMBYDD10100) */
    GatewayResult sendFaultMessenger(String globalId, FaultMessengerRequest req);
}
