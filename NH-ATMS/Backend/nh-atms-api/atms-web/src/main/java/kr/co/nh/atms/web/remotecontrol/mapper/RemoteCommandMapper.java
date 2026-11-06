package kr.co.nh.atms.web.remotecontrol.mapper;

import kr.co.nh.atms.web.remotecontrol.dto.RemoteCommandSearchDto;
import kr.co.nh.atms.web.remotecontrol.entity.RemoteCommand;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RemoteCommandMapper {
    List<RemoteCommand> selectList(RemoteCommandSearchDto search);
    long countList(RemoteCommandSearchDto search);
    RemoteCommand selectOne(@Param("cmdId") String cmdId);
    int insert(RemoteCommand command);

    /** 푸시 직후 상태/결과 갱신(동기=S/F, 비동기=A + asyncUid). */
    int updateAfterPush(@Param("cmdId") String cmdId,
                        @Param("status") String status,
                        @Param("asyncUid") Long asyncUid,
                        @Param("resultCd") String resultCd,
                        @Param("resultData") String resultData,
                        @Param("errMsg") String errMsg);

    /** asyncUid 로 명령 조회(비동기 콜백 상관). */
    RemoteCommand selectByAsyncUid(@Param("asyncUid") Long asyncUid);

    /** 비동기 콜백 결과 갱신(S/F + 결과). */
    int updateAsyncResult(@Param("asyncUid") Long asyncUid,
                          @Param("status") String status,
                          @Param("resultCd") String resultCd,
                          @Param("resultData") String resultData,
                          @Param("errMsg") String errMsg);
}
