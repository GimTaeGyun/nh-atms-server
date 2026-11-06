-- =====================================================================
-- ATMS 스키마 사용자 / 테이블스페이스 생성
-- 실행 위치: PDB(ATMSPDB)에 SYS / SYSDBA 권한으로 접속
--
-- 도커 컨테이너 내부 실행 예:
--   docker exec -it oracle19c-dev bash -lc "sqlplus / as sysdba"
--   SQL> ALTER SESSION SET CONTAINER = ATMSPDB;
--   SQL> @/opt/oracle/scripts/00_create_user.sql
--
-- 주의: 이미 생성된 객체가 있으면 해당 스택은 무시하고 다음 단계 진행
-- =====================================================================

-- 1) 테이블스페이스
--    tbs_atms01.dbf 충돌 발생 시 tbs_atms02.dbf 사용 (ORA-01537 회피)
CREATE TABLESPACE TBS_ATMS
    DATAFILE '/opt/oracle/oradata/ATMS/ATMSPDB/tbs_atms02.dbf'
    SIZE 500M AUTOEXTEND ON NEXT 100M MAXSIZE 10G
    LOGGING
    EXTENT MANAGEMENT LOCAL
    SEGMENT SPACE MANAGEMENT AUTO;

-- 2) 애플리케이션 사용자
CREATE USER ATMS IDENTIFIED BY "Atms2026!"
    DEFAULT TABLESPACE TBS_ATMS
    TEMPORARY TABLESPACE TEMP
    QUOTA UNLIMITED ON TBS_ATMS;

-- 3) 권한
GRANT CONNECT, RESOURCE TO ATMS;
GRANT CREATE SESSION       TO ATMS;
GRANT CREATE TABLE         TO ATMS;
GRANT CREATE SEQUENCE      TO ATMS;
GRANT CREATE VIEW          TO ATMS;
GRANT CREATE PROCEDURE     TO ATMS;
GRANT CREATE TRIGGER       TO ATMS;
GRANT CREATE SYNONYM       TO ATMS;

-- 4) 비밀번호 영구 (개발용)
ALTER USER ATMS PROFILE DEFAULT;
ALTER PROFILE DEFAULT LIMIT PASSWORD_LIFE_TIME UNLIMITED;


-- =====================================================================
-- [보수용] 이미 ATMS 유저가 생성되어 있어 위 2)단계가 실패했다면
--           아래 쿠타/권한 재부여 단독으로 실행
-- =====================================================================
-- ALTER USER ATMS QUOTA UNLIMITED ON TBS_ATMS;
-- ALTER USER ATMS DEFAULT TABLESPACE TBS_ATMS;
-- GRANT CONNECT, RESOURCE TO ATMS;
-- GRANT CREATE SESSION    TO ATMS;
-- GRANT CREATE TABLE      TO ATMS;
-- GRANT CREATE SEQUENCE   TO ATMS;
-- GRANT CREATE VIEW       TO ATMS;
-- GRANT CREATE PROCEDURE  TO ATMS;
-- GRANT CREATE TRIGGER    TO ATMS;
-- GRANT CREATE SYNONYM    TO ATMS;
-- ALTER PROFILE DEFAULT LIMIT PASSWORD_LIFE_TIME UNLIMITED;
