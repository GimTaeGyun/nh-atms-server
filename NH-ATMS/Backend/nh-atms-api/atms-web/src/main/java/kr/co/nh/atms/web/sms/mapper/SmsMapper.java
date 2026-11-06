package kr.co.nh.atms.web.sms.mapper;

import kr.co.nh.atms.web.sms.dto.SmsSearchDto;
import kr.co.nh.atms.web.sms.entity.SmsSendData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SmsMapper {
    List<SmsSendData> selectList(SmsSearchDto search);
    long countList(SmsSearchDto search);
    SmsSendData selectOne(@Param("smsSeq") Long smsSeq);
    int insert(SmsSendData sms);
}
