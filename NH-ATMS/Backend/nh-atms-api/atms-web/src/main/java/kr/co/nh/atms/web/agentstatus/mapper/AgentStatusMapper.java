package kr.co.nh.atms.web.agentstatus.mapper;

import kr.co.nh.atms.web.agentstatus.dto.AgentStatusSearchDto;
import kr.co.nh.atms.web.agentstatus.entity.AgentStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AgentStatusMapper {
    List<AgentStatus> selectList(AgentStatusSearchDto search);
    long countList(AgentStatusSearchDto search);
    AgentStatus selectOne(@Param("atmIp") String atmIp);

    /** 원격제어 푸시 대상 기기의 현재 접속 IP 조회(접속중 우선, 최근 기동순). */
    String selectConnectedIp(@Param("termGroupId") String termGroupId,
                             @Param("termId") String termId);
}
