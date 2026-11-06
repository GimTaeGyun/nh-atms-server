package kr.co.nh.atms.ifex.mapper;

import kr.co.nh.atms.ifex.entity.NetRecvData;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NetRecvDataMapper {

    /** 수신전문 이력 적재 (RECV_SEQ 채번) */
    int insert(NetRecvData recv);
}
