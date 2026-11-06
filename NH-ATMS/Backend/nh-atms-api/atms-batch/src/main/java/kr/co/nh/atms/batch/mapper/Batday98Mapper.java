package kr.co.nh.atms.batch.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * batday98 (테이블 데이터 정리) 매퍼 — AS-IS atmb sqlmap/batday98.xml 이식.
 * T_TABLE_MANAGEMENT(KEEP_CYCLE='1', DELETE_YN='1') 의 보관기간(KEEP_PERIOD)을
 * 기준으로 이력성 테이블의 만료 데이터를 삭제한다.
 * (resources/sql/table-management-oracle.sql — 시드는 DELETE_YN='0' 비활성 상태)
 */
@Mapper
public interface Batday98Mapper {

    /** 삭제 대상 테이블/기준일 목록 (로그용, AS-IS sel_logs) */
    List<Map<String, Object>> selectCleanupTargets();

    int deleteBatchHistory();        // T_BATCH_HISTORY        (AS-IS t_atms_batch_his)
    int deleteAtmMonitor();          // T_ATM_MONITOR
    int deleteGwInterface();         // T_GW_INTERFACE         (AS-IS t_gwinterface)
    int deleteNetRecvData();         // T_NET_RECV_DATA
    int deleteNetSendData();         // T_NET_SEND_DATA
    int deleteJournal();             // T_JOURNAL
    int deleteJournalData();         // T_JOURNAL_DATA
    int deleteJournalError();        // T_JOURNAL_ERROR
    int deleteJournalImage();        // T_JOURNAL_IMAGE
    int deleteTransactionJournal();  // T_TRANSACTION_JOURNAL
    int deleteOperationJournal();    // T_OPERATION_JOURNAL
}
