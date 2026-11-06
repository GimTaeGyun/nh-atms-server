package kr.co.nh.atms.web.sms.service;

import kr.co.nh.atms.common.page.PageResult;
import kr.co.nh.atms.web.sms.dto.SmsSearchDto;
import kr.co.nh.atms.web.sms.dto.SmsSendRequest;
import kr.co.nh.atms.web.sms.entity.SmsSendData;
import kr.co.nh.atms.web.sms.mapper.SmsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * SMS 전송 서비스.
 * 등록 시 T_SMS_SEND_DATA(SEND_STATUS='W')에 적재되어 발송 모듈이 처리한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SmsService {

    private final SmsMapper mapper;

    /** SMS 발송이력 조회. */
    public PageResult<SmsSendData> search(SmsSearchDto s) {
        return PageResult.of(mapper.selectList(s), mapper.countList(s), s);
    }

    /** SMS 전송 등록(발송 대기). */
    @Transactional
    public SmsSendData send(SmsSendRequest req) {
        SmsSendData sms = SmsSendData.builder()
                .termGroupId(req.getTermGroupId())
                .termId(req.getTermId())
                .recvPhone(req.getRecvPhone().trim())
                .sendPhone(req.getSendPhone().trim())
                .msgContent(req.getMsgContent().trim())
                .sendStatus("W")
                .build();
        mapper.insert(sms);
        log.info("[sms] queued smsSeq={} recv={} term={}",
                sms.getSmsSeq(), sms.getRecvPhone(), sms.getTermGroupId());
        return sms;
    }
}
