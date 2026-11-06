
-- SET SERVEROUTPUT ON SIZE UNLIMITED;

DECLARE
    -- 안전장치: 접속 스키마 확인 (필요 시 'ATMS'를 실제 스키마명으로 수정)
    v_schema VARCHAR2(128);

    -- (구명, 표준명) 매핑 처리 프로시저
    -- 중첩 함수 없이 인라인으로 단순화 (IDE 파서 빨간줄 방지)
    PROCEDURE migrate(p_old IN VARCHAR2, p_new IN VARCHAR2) IS
        v_old_exists  NUMBER := 0;
        v_new_exists  NUMBER := 0;
        v_old_has     NUMBER := 0;
        v_new_has     NUMBER := 0;
    BEGIN
        -- 존재 여부 확인
        SELECT COUNT(*) INTO v_old_exists FROM user_tables WHERE table_name = p_old;
        SELECT COUNT(*) INTO v_new_exists FROM user_tables WHERE table_name = p_new;

        IF v_old_exists = 0 AND v_new_exists = 0 THEN
            RETURN;  -- 둘 다 없음 → 건너뜀
        END IF;

        -- 데이터 존재 여부 확인 (ROWNUM=1 로 14M 테이블도 즉시 반환)
        IF v_old_exists = 1 THEN
            EXECUTE IMMEDIATE
                'SELECT COUNT(*) FROM (SELECT 1 FROM ' || p_old || ' WHERE ROWNUM = 1)'
                INTO v_old_has;
        END IF;
        IF v_new_exists = 1 THEN
            EXECUTE IMMEDIATE
                'SELECT COUNT(*) FROM (SELECT 1 FROM ' || p_new || ' WHERE ROWNUM = 1)'
                INTO v_new_has;
        END IF;

        -- 케이스별 처리
        IF v_old_exists = 1 AND v_new_exists = 1 THEN
            IF v_old_has > 0 AND v_new_has = 0 THEN
                -- 구명에 데이터, 표준명은 비어있음 → 표준명 DROP 후 RENAME
                EXECUTE IMMEDIATE 'DROP TABLE ' || p_new || ' CASCADE CONSTRAINTS PURGE';
                DBMS_OUTPUT.PUT_LINE('  DROP   ' || p_new || ' (빈 중복 제거)');
            ELSIF v_new_has > 0 AND v_old_has = 0 THEN
                -- 표준명에 데이터, 구명은 비어있음 → 구명만 DROP
                EXECUTE IMMEDIATE 'DROP TABLE ' || p_old || ' CASCADE CONSTRAINTS PURGE';
                DBMS_OUTPUT.PUT_LINE('  DROP   ' || p_old || ' (빈 구명 제거)');
                RETURN;
            ELSIF v_old_has = 0 AND v_new_has = 0 THEN
                -- 둘 다 비어있음 → 구명만 DROP
                EXECUTE IMMEDIATE 'DROP TABLE ' || p_old || ' CASCADE CONSTRAINTS PURGE';
                DBMS_OUTPUT.PUT_LINE('  DROP   ' || p_old || ' (빈 구명 제거)');
                RETURN;
            ELSE
                -- 양쪽 모두 데이터 존재 → 수동 확인 필요
                DBMS_OUTPUT.PUT_LINE('  ⚠️ 양쪽 모두 데이터 존재(수동 확인): '
                                     || p_old || ' / ' || p_new);
                RETURN;
            END IF;
        ELSIF v_new_exists = 1 AND v_old_exists = 0 THEN
            -- 이미 표준명만 존재 → 멱등, 건너뜀
            RETURN;
        END IF;

        -- 여기까지 왔으면: 구명 존재, 표준명 없음(또는 방금 DROP) → RENAME
        -- 1) 테이블명 변경 (Oracle 전용 문법)
        EXECUTE IMMEDIATE 'RENAME ' || p_old || ' TO ' || p_new;
        DBMS_OUTPUT.PUT_LINE('  RENAME ' || p_old || ' -> ' || p_new);

        -- 2) PK 제약명 변경 (옵티마이저 힌트 정합)
        BEGIN
            EXECUTE IMMEDIATE 'ALTER TABLE ' || p_new
                              || ' RENAME CONSTRAINT PK_' || p_old
                              || ' TO PK_' || p_new;
            DBMS_OUTPUT.PUT_LINE('         PK_' || p_old || ' -> PK_' || p_new);
        EXCEPTION WHEN OTHERS THEN NULL;  -- PK명이 규칙과 다르면 건너뜀
        END;

        -- 3) 인덱스명 변경 (구명 포함 인덱스 전체)
        FOR ix IN (SELECT index_name FROM user_indexes
                    WHERE table_name = p_new
                      AND INSTR(index_name, p_old) > 0) LOOP
            BEGIN
                EXECUTE IMMEDIATE 'ALTER INDEX ' || ix.index_name
                                  || ' RENAME TO '
                                  || REPLACE(ix.index_name, p_old, p_new);
            EXCEPTION WHEN OTHERS THEN NULL;
            END;
        END LOOP;

    EXCEPTION WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('  오류 (' || p_old || '): ' || SQLERRM);
    END migrate;

BEGIN
    SELECT SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') INTO v_schema FROM dual;
    IF v_schema <> 'ATMS' THEN
        RAISE_APPLICATION_ERROR(-20001,
            '현재 스키마(' || v_schema || ')가 ATMS가 아닙니다. 안전을 위해 중단합니다.');
    END IF;

    DBMS_OUTPUT.PUT_LINE('=== 명명 표준 정리 시작 ===');

    -- ----- 저널/거래 핵심 (데이터 보존 대상 포함) -----
    migrate('T_ATMMONITOR',              'T_ATM_MONITOR');
    migrate('T_TRANSACTIONJOURNAL',      'T_TRANSACTION_JOURNAL');
    migrate('T_OPERATIONJOURNAL',        'T_OPERATION_JOURNAL');
    migrate('T_JOURNALDATA',             'T_JOURNAL_DATA');
    migrate('T_JOURNALIMAGE',            'T_JOURNAL_IMAGE');
    migrate('T_DAYTOTAL',                'T_DAY_TOTAL');
    migrate('T_ADDCASH',                 'T_ADD_CASH');
    migrate('T_ADDCHECK',                'T_ADD_CHECK');

    -- ----- 에이전트/배치/집계 -----
    migrate('T_AGENTSTATUS',             'T_AGENT_STATUS');
    migrate('T_ATMUNITSTATUS',           'T_ATM_UNIT_STATUS');
    migrate('T_BATCHJOB',                'T_BATCH_JOB');
    migrate('T_BATCHHISTORY',            'T_BATCH_HISTORY');
    migrate('T_TRANSACTIONDAILYTOTAL',   'T_TRANSACTION_DAILY_TOTAL');
    migrate('T_FAULTRATEMANAGEMENT',     'T_FAULT_RATE_MANAGEMENT');
    migrate('T_OPERATIONRATEMANAGEMENT', 'T_OPERATION_RATE_MANAGEMENT');
    migrate('T_TRANSACTIONHOURLYTOTAL',  'T_TRANSACTION_HOURLY_TOTAL');
    migrate('T_TRANSACTIONMONTHLYTOTAL', 'T_TRANSACTION_MONTHLY_TOTAL');
    migrate('T_JOURNALPROCESSTOTAL',     'T_JOURNAL_PROCESS_TOTAL');
    migrate('T_OPENCLOSESTATUS',         'T_OPEN_CLOSE_STATUS');
    migrate('T_GIROMONTHLYTOTAL',        'T_GIRO_MONTHLY_TOTAL');

    -- ----- 연계(IF) -----
    migrate('T_GWINTERFACE',             'T_GW_INTERFACE');
    migrate('T_SMSSENDDATA',             'T_SMS_SEND_DATA');
    migrate('T_NETSENDDATA',             'T_NET_SEND_DATA');
    migrate('T_NETRECVDATA',             'T_NET_RECV_DATA');
    migrate('T_FAULTMANAGEMENT',         'T_FAULT_MANAGEMENT');
    migrate('T_SEQNOMASTER',             'T_SEQ_NO_MASTER');

    -- ----- 마스터/코드 -----
    migrate('T_CODEGROUP',               'T_CODE_GROUP');
    migrate('T_ATMGROUP',                'T_ATM_GROUP');
    migrate('T_ATMSTATUS',               'T_ATM_STATUS');
    migrate('T_ATMMODEL',                'T_ATM_MODEL');
    migrate('T_VENDORATM',               'T_VENDOR_ATM');
    migrate('T_ERRORMESSAGE',            'T_ERROR_MESSAGE');
    migrate('T_DISPATCHHISTORY',         'T_DISPATCH_HISTORY');
    migrate('T_ATTACHEDFILE',            'T_ATTACHED_FILE');
    migrate('T_SERVICEPROVIDER',         'T_SERVICE_PROVIDER');

    -- ----- 거래 상세/상태 -----
    migrate('T_CASHTRANSACTION',         'T_CASH_TRANSACTION');
    migrate('T_CHECKTRANSACTION',        'T_CHECK_TRANSACTION');
    migrate('T_CASSETTESTATUS',          'T_CASSETTE_STATUS');
    migrate('T_FAULTANALYSIS',           'T_FAULT_ANALYSIS');
    migrate('T_ATMSTOPHISTORY',          'T_ATM_STOP_HISTORY');
    migrate('T_GIROTRANSACTION',         'T_GIRO_TRANSACTION');
    migrate('T_FOREXTRANSACTION',        'T_FOREX_TRANSACTION');
    migrate('T_PASSBOOKTRANSACTION',     'T_PASSBOOK_TRANSACTION');
    migrate('T_FOREXTOTAL',              'T_FOREX_TOTAL');
    migrate('T_FILEATTACH',              'T_FILE_ATTACH');
    migrate('T_SLIPFORMAT',              'T_SLIP_FORMAT');
    migrate('T_REMOTECOMMAND',           'T_REMOTE_COMMAND');
    migrate('T_ECASHTRANSACTION',        'T_ECASH_TRANSACTION');
    migrate('T_NONBANKDEPOSIT',          'T_NONBANK_DEPOSIT');
    migrate('T_TRANSTATUSMSG',           'T_TRAN_STATUS_MSG');
    migrate('T_NETWORKSTATUS',           'T_NETWORK_STATUS');
    migrate('T_DAEMONSTATUS',            'T_DAEMON_STATUS');
    migrate('T_UNFINISHEDCOUNT',         'T_UNFINISHED_COUNT');
    migrate('T_CARDBIN',                 'T_CARD_BIN');
    migrate('T_DEVICEHEATTIME',          'T_DEVICE_HEAT_TIME');

    -- ----- 자금/AS-IS 미매핑 -----
    migrate('T_FUNDRETURNMEMO',          'T_FUND_RETURN_MEMO');  -- 긴 이름 먼저
    migrate('T_FUNDRETURN',              'T_FUND_RETURN');
    migrate('T_VIPCUSTOMER',             'T_VIP_CUSTOMER');
    migrate('T_OVERSEAREMITTANCE',       'T_OVERSEA_REMITTANCE');
    migrate('T_DESTROYVIEWHIST',         'T_DESTROY_VIEW_HIST');
    migrate('T_ACCIDENTREPORT',          'T_ACCIDENT_REPORT');
    migrate('T_CASHSHORTAGE',            'T_CASH_SHORTAGE');
    migrate('T_EAPPROVALHIST',           'T_EAPPROVAL_HIST');
    migrate('T_SLIPEDIT',                'T_SLIP_EDIT');

    -- ----- HR/감사 -----
    migrate('T_JOURNALAUTHHIST',         'T_JOURNAL_AUTH_HIST');  -- 긴 이름 먼저
    migrate('T_JOURNALAUTH',             'T_JOURNAL_AUTH');
    migrate('T_SYSTEMCONFIG',            'T_SYSTEM_CONFIG');
    migrate('T_JOURNALVIEWLOG',          'T_JOURNAL_VIEW_LOG');
    migrate('T_PERSONALINFOPRINTHIST',   'T_PERSONAL_INFO_PRINT_HIST');
    migrate('T_DATADELETEHIST',          'T_DATA_DELETE_HIST');

    -- ----- SLA/용역 -----
    migrate('T_SLAGRADEHIST',            'T_SLA_GRADE_HIST');     -- 긴 이름 먼저
    migrate('T_SLAGRADE',                'T_SLA_GRADE');
    migrate('T_CORNERSLA',               'T_CORNER_SLA');
    migrate('T_DEVICEOPRATEHIST',        'T_DEVICE_OP_RATE_HIST');
    migrate('T_DEVICEFAULTHIST',         'T_DEVICE_FAULT_HIST');
    migrate('T_VENDORCSEVAL',            'T_VENDOR_CS_EVAL');
    migrate('T_NONOPREASON',             'T_NON_OP_REASON');
    migrate('T_SERVICEFEECALC',          'T_SERVICE_FEE_CALC');

    DBMS_OUTPUT.PUT_LINE('=== 정리 완료 ===');
    COMMIT;
END;


-- ---------------------------------------------------------------------
-- 검증: 아래 쿼리가 0행이면 구 명명 잔재 없음 (정상)
-- ---------------------------------------------------------------------
SELECT table_name AS 구명명_잔재
FROM   user_tables
WHERE  table_name IN (
  'T_ATMMONITOR','T_TRANSACTIONJOURNAL','T_OPERATIONJOURNAL','T_JOURNALDATA','T_JOURNALIMAGE',
  'T_DAYTOTAL','T_ADDCASH','T_ADDCHECK','T_AGENTSTATUS','T_ATMUNITSTATUS','T_BATCHJOB',
  'T_BATCHHISTORY','T_TRANSACTIONDAILYTOTAL','T_FAULTRATEMANAGEMENT','T_OPERATIONRATEMANAGEMENT',
  'T_TRANSACTIONHOURLYTOTAL','T_TRANSACTIONMONTHLYTOTAL','T_JOURNALPROCESSTOTAL','T_OPENCLOSESTATUS',
  'T_GIROMONTHLYTOTAL','T_GWINTERFACE','T_SMSSENDDATA','T_NETSENDDATA','T_NETRECVDATA','T_FAULTMANAGEMENT',
  'T_CODEGROUP','T_ATMGROUP','T_ATMSTATUS','T_ATMMODEL','T_VENDORATM','T_ERRORMESSAGE','T_DISPATCHHISTORY',
  'T_ATTACHEDFILE','T_SERVICEPROVIDER','T_CASHTRANSACTION','T_CHECKTRANSACTION','T_CASSETTESTATUS',
  'T_FAULTANALYSIS','T_ATMSTOPHISTORY','T_GIROTRANSACTION','T_FOREXTRANSACTION','T_PASSBOOKTRANSACTION',
  'T_FOREXTOTAL','T_FILEATTACH','T_SLIPFORMAT','T_REMOTECOMMAND','T_ECASHTRANSACTION','T_NONBANKDEPOSIT',
  'T_TRANSTATUSMSG','T_NETWORKSTATUS','T_DAEMONSTATUS','T_UNFINISHEDCOUNT','T_CARDBIN','T_DEVICEHEATTIME',
  'T_FUNDRETURN','T_FUNDRETURNMEMO','T_VIPCUSTOMER','T_OVERSEAREMITTANCE','T_DESTROYVIEWHIST',
  'T_ACCIDENTREPORT','T_CASHSHORTAGE','T_EAPPROVALHIST','T_SLIPEDIT','T_JOURNALAUTH','T_JOURNALAUTHHIST',
  'T_SYSTEMCONFIG','T_JOURNALVIEWLOG','T_PERSONALINFOPRINTHIST','T_DATADELETEHIST','T_SLAGRADE',
  'T_SLAGRADEHIST','T_CORNERSLA','T_DEVICEOPRATEHIST','T_DEVICEFAULTHIST','T_VENDORCSEVAL',
  'T_NONOPREASON','T_SERVICEFEECALC'
)
ORDER BY table_name;
