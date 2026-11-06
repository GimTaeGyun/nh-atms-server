package kr.co.nh.atms.journal.mapper;

import kr.co.nh.atms.journal.entity.AgentStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AgentStatusMapper {

    /** 에이전트 기동(open): 있으면 갱신, 없으면 삽입. */
    int upsert(AgentStatus a);

    /** 에이전트 종료(exit): connect_flag=0 + exit 시각. */
    int updateExit(@Param("atmIp") String atmIp,
                   @Param("exitDate") String exitDate,
                   @Param("exitTime") String exitTime);

    /** 폴링(ping): 최종 폴링 시각 갱신. */
    int updatePoll(@Param("atmIp") String atmIp,
                   @Param("pollDate") String pollDate,
                   @Param("pollTime") String pollTime);

    /** 저널모드 통보: 기기(termGroupId/termId) 기준 JNL_MODE 갱신. */
    int updateJnlMode(@Param("termGroupId") String termGroupId,
                      @Param("termId") String termId,
                      @Param("jnlMode") String jnlMode);

    /** 저널 수신: 기기(termGroupId/termId) 기준 최종 저널 수신 일시 갱신(atmr update_T_AGENTSTATUS 대응). */
    int updateRecvOnJournal(@Param("termGroupId") String termGroupId,
                            @Param("termId") String termId,
                            @Param("jnlDate") String jnlDate,
                            @Param("jnlTime") String jnlTime);

    /** 단건 조회. */
    AgentStatus selectByIp(@Param("atmIp") String atmIp);
}
