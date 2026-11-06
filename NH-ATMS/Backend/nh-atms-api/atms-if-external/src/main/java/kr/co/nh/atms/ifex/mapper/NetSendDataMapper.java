package kr.co.nh.atms.ifex.mapper;

import kr.co.nh.atms.ifex.entity.NetSendData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NetSendDataMapper {

    /** 송신전문 이력 적재 (SEND_SEQ 채번) */
    int insert(NetSendData send);

    /** 결번요청(IQ_04001110) 재전송용 원 송신전문 조회 (업체/송신일자/ATMS전문일련번호) */
    NetSendData selectForResend(@Param("providerCd") String providerCd,
                                @Param("sendDate") String sendDate,
                                @Param("atmsSn") String atmsSn);
}
