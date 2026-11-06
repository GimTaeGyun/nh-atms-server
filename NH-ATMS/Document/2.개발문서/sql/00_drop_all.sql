-- =====================================================================
-- 00_drop_all.sql : ATMS 스키마 전체 초기화 (테스트 서버 재구축용)
--   목적: 명명 표준(언더스코어) 일괄 전환 후, 구 명명 잔재·중복 테이블을
--         완전히 제거하고 표준 DDL로 깨끗이 재구축하기 위한 초기화 스크립트.
--   대상: 현재 접속한 스키마(ATMS)의 모든 테이블/시퀀스
--
--   ⚠️⚠️ 경고 ⚠️⚠️
--   - 이 스크립트는 현재 접속 스키마의 **모든 테이블과 데이터를 영구 삭제**한다.
--   - 반드시 ATMS(테스트) 스키마에 접속한 상태에서만 실행할 것.
--   - 운영/공유 DB에서는 절대 실행 금지.
--   - PURGE 옵션이므로 휴지통 복구 불가.
--
--   실행 후 순서:
--     00_drop_all.sql  (본 파일)
--     → V1.0.0 → V1.0.2 → V1.0.3 ... → V1.3.2 → V1.3.3
--     (Flyway 사용 시 baseline 재설정 또는 flyway_schema_history도 정리)
-- =====================================================================

-- ---------------------------------------------------------------------
-- 0) 안전장치: 접속 스키마 확인 (ATMS가 아니면 즉시 중단)
--    필요 시 아래 'ATMS'를 실제 테스트 스키마명으로 수정.
-- ---------------------------------------------------------------------
DECLARE
    v_schema VARCHAR2(128);
BEGIN
    SELECT SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') INTO v_schema FROM dual;
    IF v_schema <> 'ATMS' THEN
        RAISE_APPLICATION_ERROR(-20001,
            '현재 스키마(' || v_schema || ')가 ATMS가 아닙니다. 안전을 위해 중단합니다.');
    END IF;
END;
/

-- ---------------------------------------------------------------------
-- 1) 모든 테이블 DROP (제약·인덱스 동반 삭제, 휴지통 미경유)
-- ---------------------------------------------------------------------
BEGIN
    FOR t IN (SELECT table_name FROM user_tables) LOOP
        BEGIN
            EXECUTE IMMEDIATE
                'DROP TABLE "' || t.table_name || '" CASCADE CONSTRAINTS PURGE';
        EXCEPTION
            WHEN OTHERS THEN
                DBMS_OUTPUT.PUT_LINE('테이블 DROP 실패: ' || t.table_name || ' / ' || SQLERRM);
        END;
    END LOOP;
END;
/

-- ---------------------------------------------------------------------
-- 2) 모든 시퀀스 DROP (SEQ_T_JOURNAL_ERROR 등)
-- ---------------------------------------------------------------------
BEGIN
    FOR s IN (SELECT sequence_name FROM user_sequences) LOOP
        BEGIN
            EXECUTE IMMEDIATE 'DROP SEQUENCE "' || s.sequence_name || '"';
        EXCEPTION
            WHEN OTHERS THEN
                DBMS_OUTPUT.PUT_LINE('시퀀스 DROP 실패: ' || s.sequence_name || ' / ' || SQLERRM);
        END;
    END LOOP;
END;
/

-- ---------------------------------------------------------------------
-- 3) 검증: 남은 객체가 없어야 정상 (0행이면 OK)
-- ---------------------------------------------------------------------
SELECT 'TABLE'    AS obj_type, table_name    AS obj_name FROM user_tables
UNION ALL
SELECT 'SEQUENCE' AS obj_type, sequence_name AS obj_name FROM user_sequences
ORDER BY obj_type, obj_name;
