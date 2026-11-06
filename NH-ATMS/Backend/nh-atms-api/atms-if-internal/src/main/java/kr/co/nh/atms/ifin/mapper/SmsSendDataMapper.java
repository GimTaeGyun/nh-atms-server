package kr.co.nh.atms.ifin.mapper;

import kr.co.nh.atms.ifin.entity.SmsSendData;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SmsSendDataMapper {

    /** SMS 발송 이력 적재 (SMS_SEQ 채번) */
    int insert(SmsSendData sms);

    /** 발송 결과 갱신 */
    int updateResult(SmsSendData sms);
}
