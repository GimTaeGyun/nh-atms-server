package kr.co.nh.atms.ifin.mapper;

import kr.co.nh.atms.ifin.entity.GwInterface;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface GwInterfaceMapper {

    /** 연계전문 이력 적재 (GW_SEQ 채번, GW_FLAG '0') */
    int insert(GwInterface gw);

    /** 응답 수신 반영 (GW_FLAG '1', MSG_SEQNO 매칭 - AS-IS updateGwInterface 복원) */
    int updateRecvByMsgSeqNo(@Param("msgSeqNo") String msgSeqNo,
                             @Param("recvData") String recvData,
                             @Param("resultCd") String resultCd,
                             @Param("resultMsg") String resultMsg);

    /** EAI 전문 일련번호(7자리) 채번 (구 SP_GET_SEQNO 대응 - Oracle 시퀀스) */
    String selectNextMsgSeqNo();
}
