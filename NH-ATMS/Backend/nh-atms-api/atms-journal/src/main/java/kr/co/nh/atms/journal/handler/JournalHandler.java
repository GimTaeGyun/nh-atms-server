package kr.co.nh.atms.journal.handler;

import kr.co.nh.atms.journal.dto.JifHeadKey;
import kr.co.nh.atms.journal.dto.JifMessage;

import java.util.Set;

/**
 * jnl_kind 별 분기 처리 핸들러.
 * 신규 jnl_kind 지원 = 이 인터페이스 구현체 1개 추가 + @Component 만 붙이면 자동 등록.
 */
public interface JournalHandler {
    /** 이 핸들러가 지원하는 jnl_kind 집합. (예: 21, 22) */
    Set<String> supportedKinds();

    /** 본문 저장 (T_JOURNAL 헤더는 Dispatcher 가 이미 저장한 뒤 호출). */
    void handle(JifHeadKey key, JifMessage msg);
}
