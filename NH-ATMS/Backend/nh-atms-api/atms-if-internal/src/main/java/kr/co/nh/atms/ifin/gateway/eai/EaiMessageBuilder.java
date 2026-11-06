package kr.co.nh.atms.ifin.gateway.eai;

import com.google.gson.JsonObject;
import kr.co.nh.atms.ifin.constants.InternalIfConstants;
import kr.co.nh.atms.ifin.entity.SmsSendData;
import kr.co.nh.atms.ifin.gateway.eai.util.Json;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 대내 인터페이스(IIF) 송신전문 조립기.
 * 구 atmi ATMONST10100_OUT / SMSONSS10200_OUT 의 전문 생성부 이식.
 * 필드 키는 interface.internal.json 스펙 키와 1:1 이며, 패딩은 Converter.toPacket 이 수행한다.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "internal.gateway.mode", havingValue = "eai")
public class EaiMessageBuilder {

    private static final DateTimeFormatter D14 = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter D17 = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final EaiGatewayProperties props;
    private final EaiGlobalIdGenerator idGen;

    /**
     * 시재조회(ATMONST10100-S) 요청 전문.
     * AS-IS: gmgo_cd = term_group_id.substring(1), occur = 텔러수, telr_no = 텔러번호(3자) 연결.
     */
    public JsonObject drawerCheckMessage(String globalId, String termGroupId, List<String> tellerNos) {
        JsonObject head = head(globalId,
                InternalIfConstants.DRAWER_CHECK_TR_CD,
                InternalIfConstants.DRAWER_CHECK_SVC_CD,
                "0");

        JsonObject body = new JsonObject();
        body.addProperty("gmgo_cd", termGroupId != null && termGroupId.length() > 1
                ? termGroupId.substring(1) : termGroupId);                       // 금고코드(4)
        body.addProperty("occur", String.format("%02d", tellerNos.size()));      // 반복횟수(2)
        body.addProperty("telr_no", String.join("", tellerNos));                 // 텔러번호(3*30)

        JsonObject message = new JsonObject();
        message.add("head", head);
        message.add("body", body);
        return message;
    }

    /**
     * UMS SMS 장애통보(SMSONSS10200) 요청 전문 - 30필드 바디 (AS-IS 기준).
     */
    public JsonObject smsMessage(String globalId, SmsSendData sms) {
        JsonObject head = head(globalId,
                InternalIfConstants.SMS_SEND_TR_CD,
                InternalIfConstants.SMS_SEND_SVC_CD,
                "0");

        String now14 = LocalDateTime.now().format(D14);
        String termGroupCd = sms.getTermGroupId() != null && sms.getTermGroupId().length() > 1
                ? sms.getTermGroupId().substring(1) : sms.getTermGroupId();

        JsonObject body = new JsonObject();
        body.addProperty("snd_hist_id", padRight(globalId, 40, '0'));    // 발송이력식별자 : 글로벌ID(32)+00000000
        body.addProperty("trans_id", "UMSSTD0001");                      // 거래식별자
        body.addProperty("msg_type", "SM");                              // 메시지구분
        body.addProperty("ums_biz_id", "BKATM2601");                     // UMS업무식별자
        body.addProperty("sub_biz_type", "");                            // 업무내구분
        body.addProperty("trans_dt", now14);                             // 거래일시 (14)
        body.addProperty("req_dt", now14);                               // 요청일시 (14)
        body.addProperty("resrv_flag", "N");                             // 예약플래그
        body.addProperty("resrv_dt", "");                                // 예약일시
        body.addProperty("ums_id", "");                                  // UMS아이디
        body.addProperty("ums_join_chnl", "");                           // UMS가입채널
        body.addProperty("cust_type", "");                               // 개인기업구분
        body.addProperty("cust_class", "");                              // 고객구분
        body.addProperty("cust_id", "");                                 // 고객번호
        body.addProperty("real_name_no", "");                            // 실명번호
        body.addProperty("cust_name", "");                               // 고객명
        body.addProperty("acct_mgmt_br", "");                            // 계좌관리부점
        body.addProperty("acct_no", "");                                 // 계좌번호
        body.addProperty("real_acct_type", "");                          // 실계좌구분
        body.addProperty("real_acct_no", "");                            // 실계좌번호
        body.addProperty("card_no", "");                                 // 카드번호
        body.addProperty("rcpt_addr_no", nvl(sms.getRecvPhone()));       // 수신번호주소 : 휴대폰번호
        body.addProperty("snd_addr_no", nvl(sms.getSendPhone()));        // 발신번호주소 : 금고 전화번호
        body.addProperty("snd_emp_no", "");                              // 발송직원번호
        body.addProperty("snd_br_cd", nvl(termGroupCd));                 // 발송부점코드 : 금고코드
        body.addProperty("callback_url", "");                            // 콜백URL
        body.addProperty("temp_fil_1", "");                              // 예비
        body.addProperty("temp_fil_2", "");                              // 예비
        body.addProperty("sms_subject", nvl(sms.getMsgContent()));       // 단문내용제목 : SMS본문
        body.addProperty("lms_subject", "");                             // 장문내용제목

        JsonObject message = new JsonObject();
        message.add("head", head);
        message.add("body", body);
        return message;
    }

    /**
     * 표준전문 헤더(50필드). 구 atmi *_OUT 헤더 조립 공통부 이식.
     * stnd_msg_len / data_length 는 송신 직전 Converter/게이트웨이가 재계산한다.
     */
    private JsonObject head(String globalId, String trCd, String eaiSvcCd, String yeonsumdYn) {
        String now17 = LocalDateTime.now().format(D17);
        int bodyTlen = Json.int32(Converter.IIF.body(eaiSvcCd), Converter.TLEN_KEY);

        JsonObject head = new JsonObject();
        head.addProperty("stnd_msg_len", "0");            // 표준전문길이 : toPacket 이 재계산
        head.addProperty("global_id", globalId);          // 글로벌ID
        head.addProperty("tr_cd", trCd);                  // 거래코드
        head.addProperty("resp_tr_cd", "");               // 응답거래코드
        head.addProperty("long_msg_type", "1");           // 출력전문유형 1 기본
        head.addProperty("send_rspn_type", "S");          // 요청/응답구분 요청(S) 응답(R)
        head.addProperty("msg_cret_dtim", now17);         // 전문생성일시 yyyyMMddHHmmssSSS
        head.addProperty("yeonsumd_yn", yeonsumdYn);      // 연수모드여부 0 운영 1 연수(개발)
        head.addProperty("sync_type", "S");               // SYNC구분코드 S(sync)/A(async)
        head.addProperty("async_resp_yn", "Y");           // ASYNC응답여부
        head.addProperty("xa_begin_flag", "0");           // XA거래구분코드 0:NonXA
        head.addProperty("eai_svc_cd", eaiSvcCd);         // EAI서비스코드
        head.addProperty("trmn_lock_yn", "0");            // 단말잠금여부
        head.addProperty("send_intf_gb", "2");            // 송신인터페이스구분 2:단위 4:코어
        head.addProperty("send_node_cntn", idGen.sendNodeCntn()); // 송신노드내용 DATM1/PATM1..
        head.addProperty("chl_knd", "GH");                // 채널유형 : ATMS 서버 GH
        head.addProperty("msg_knd", "0");                 // 전문종류 0:요청전문
        head.addProperty("msg_send_gbcd", "1");           // 전문전송구분코드
        head.addProperty("al_appl_code", "");             // 애니링크업무코드
        head.addProperty("al_kind_code", "");             // 애니링크종별코드
        head.addProperty("al_inst_code", "");             // 애니링크기관코드
        head.addProperty("msg_flag", "");                 // 전문결과코드
        head.addProperty("err_src", "");                  // 장애시스템명
        head.addProperty("err_type", "");                 // 장애종류
        head.addProperty("msg_cd", "");                   // 메세지코드
        head.addProperty("clnt_ip_no", props.getClientIp());   // 클라이언트IP번호
        head.addProperty("client_mac", props.getClientMac());  // 클라이언트MAC
        head.addProperty("scm_id", "");                   // 화면ID
        head.addProperty("clos_gb", "0");                 // 마감구분 0:마감전
        head.addProperty("msg_medu_gbcd", "");            // 전문매체구분코드
        head.addProperty("tr_gmgo_conv_yn", "1");         // 거래금고전환여부 1:전환
        head.addProperty("tr_gmgocd", "0001");            // 거래금고코드 0001:연합회
        head.addProperty("tr_gmgo_hqbrcd", "999");        // 거래금고본지점코드 999:본부
        head.addProperty("tr_gmgo_tmno", "D01");          // 거래금고단말번호
        head.addProperty("telrno", "D01");                // 텔레번호
        head.addProperty("empno", "D010001");             // 사원번호
        head.addProperty("aprv_empno_1", "");             // 승인사원번호1
        head.addProperty("aprv_empno_2", "");             // 승인사원번호2
        head.addProperty("busi_yn", "");                  // 영업유무
        head.addProperty("sys_date", "");                 // 시스템일자
        head.addProperty("orgnl_global_id", globalId);    // 원GLOBAL_ID
        head.addProperty("apm_msg_send_time", "");        // APM전문전송시간
        head.addProperty("apm_msg_rcv_tm", "");           // APM전문수신시간
        head.addProperty("apm_log_flag", "");             // APM로깅FLAG
        head.addProperty("filler", "");                   // 여유공간

        // #data-head# (35byte 고정)
        head.addProperty("data_type", "90");                              // 데이터종류
        head.addProperty("data_length", String.valueOf(27 + bodyTlen));   // 데이터전문길이 : 27 + body tlen
        head.addProperty("txn_code", trCd);                               // 거래코드
        head.addProperty("src_id", "");                                   // 화면ID
        head.addProperty("form_cnt", "00");                               // Form반복회수
        return head;
    }

    private static String nvl(String v) {
        return v == null ? "" : v;
    }

    private static String padRight(String input, int totalLength, char pad) {
        if (input == null) input = "";
        if (input.length() >= totalLength) return input.substring(0, totalLength);
        StringBuilder sb = new StringBuilder(input);
        while (sb.length() < totalLength) sb.append(pad);
        return sb.toString();
    }
}
