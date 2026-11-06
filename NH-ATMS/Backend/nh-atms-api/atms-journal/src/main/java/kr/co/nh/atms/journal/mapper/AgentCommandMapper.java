package kr.co.nh.atms.journal.mapper;

import kr.co.nh.atms.journal.entity.AgentCommand;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AgentCommandMapper {

    /** 명령 발행 (selectKey 로 cmdId 채번). */
    int insert(AgentCommand c);

    /** 단건 조회. */
    AgentCommand selectById(@Param("cmdId") String cmdId);

    /** 기기별 대기(W) 명령을 발행순으로 limit 만큼 조회 (에이전트 폴링). */
    List<AgentCommand> selectPending(@Param("termGroupId") String termGroupId,
                                     @Param("termId") String termId,
                                     @Param("limit") int limit);

    /** 수령확인: W → A (이미 종료 상태면 미변경). */
    int markAck(@Param("cmdId") String cmdId);

    /** 결과 반영: status(S/F) + resultCd/resultData/errMsg/completedAt. */
    int updateResult(AgentCommand c);

    /** 타임아웃 대상(대기/수령 상태에서 ISSUED_AT + TIMEOUT_SEC 경과) 조회. */
    List<AgentCommand> selectTimedOut(@Param("limit") int limit);

    /** 타임아웃 처리: 현재 W/A 인 건만 T 로. */
    int markTimeout(@Param("cmdId") String cmdId);
}
