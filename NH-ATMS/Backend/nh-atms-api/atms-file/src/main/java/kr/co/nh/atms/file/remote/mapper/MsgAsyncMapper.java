package kr.co.nh.atms.file.remote.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.Map;

/**
 * 비동기 원격명령 송수신 이력(T_MSG_ASYNC) 매퍼.
 * 구 새마을금고 sqlmap/atms/t_msg_async.xml (MySQL) → Oracle 이식.
 * sta: 1=송신, 2=에러, 3=수신완료
 */
@Mapper
public interface MsgAsyncMapper {

    /** 송신 이력 insert. selectKey 로 param["uid"](Long) 채움. */
    void insert(Map<String, Object> param);

    /** 수신/에러 결과 update (uid 기준). */
    void update(Map<String, Object> param);

    /** 비동기 결과 조회 (sta/rcv_dt/rcv). */
    Map<String, Object> selectRcv(Map<String, Object> param);
}
