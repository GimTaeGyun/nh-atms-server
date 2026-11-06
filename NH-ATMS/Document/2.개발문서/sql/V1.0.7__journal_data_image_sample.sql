-- =====================================================================
-- V1.0.7 : 저널 본문/이미지 실데이터 import
--   - T_JOURNAL_DATA  253 rows (새마을금고 datm.t_journaldata 원본)
--   - T_JOURNAL_IMAGE 205 rows (새마을금고 datm.t_journalimage 원본)
--   - datm. 스키마 prefix 제거, CLOB 텍스트는 원문 보존
--   - 재실행 시 ORA-00001(unique) 무시 또는 TRUNCATE 후 재적재
-- =====================================================================
SET DEFINE OFF;

-- ---- T_JOURNAL_DATA ----
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250924', '1316065540', '
==========================
   ◈ POWER OFF/ON ◈
0706 415 0120 
일      자 : 20250924
시      간 : 13:16:06
UPDATE    : 25.08.23
AP VERSION: V250825_O
SD R/M:13,051MB
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250924', '1310215520', '▤▤▤▤ 전원  차단 ▤▤▤▤
일 자 : 20250924
시 간 : 13:10:20
사 유 : 비정상 종료
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250924', '0018045510', '
==========================
   ◈ POWER OFF/ON ◈
0706 415 0120 
일      자 : 20250924
시      간 : 00:18:04
UPDATE    : 25.08.08
AP VERSION: V999999_O
SD R/M:13,051MB
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250924', '0012325490', '▤▤▤▤ 전원  차단 ▤▤▤▤
일 자 : 20250924
시 간 : 00:12:01
사 유 : 24시 기계 관리용 리부팅
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250923', '1433595470', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 정상 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250923', '1433585460', '        ◈ OFF - LINE ◈
        ◈ 개국명세표 ◈
HOST        :   172.25.221.64
PORT        :            8200
LOCAL       :  172.27.128.242
GATE        :    172.27.128.1
NETMASK     :   255.255.255.0
금고번호(점번) :    00706
기        번 :      415
일 련   번 호 :     0119
개 국   일 자 : 20250923
개 국   시 간 : 14:33:56
HOST MS차단  : 미차단
장애인　  모드 : 실  행(정상)
근접센서  모드 : 사용
R F 버전     : HS-N-08 
미수취함 설정: 수표회수함
TOTA:GCEK-200916- G3
BVTO:KRW -190201-  0
MAIN:GAAJ-200909-G2
TSK :GACA-180905-01
CAM :GADA-200710-02
MBV :GAUL-201008-0E
RJB :GAFD-160513-01
RJP :GAGD-161220-01
CSTA:GAHD-190729-01
CSTB:GAHD-190729-01
CSTC:GAHD-190729-01
CSTD:GAJF-190514-N0
SCST:GAID-190729-01
ML  :GALA-190705-03
MU  :GAMA-190705-03
MS  :GANA-190705-01
SBV :GAUM-200930-01');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250923', '1319365430', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 정상 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250923', '0953285400', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 정상 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250923', '0953275390', '        ◈ OFF - LINE ◈
        ◈ 개국명세표 ◈
HOST        :   172.25.221.64
PORT        :            8200
LOCAL       :  172.27.128.242
GATE        :    172.27.128.1
NETMASK     :   255.255.255.0
금고번호(점번) :    00706
기        번 :      415
일 련   번 호 :     0116
개 국   일 자 : 20250923
개 국   시 간 : 09:53:24
HOST MS차단  : 미차단
장애인　  모드 : 실  행(정상)
근접센서  모드 : 사용
R F 버전     : HS-N-08 
미수취함 설정: 수표회수함
TOTA:GCEK-200916- G3
BVTO:KRW -190201-  0
MAIN:GAAJ-200909-G2
TSK :GACA-180905-01
CAM :GADA-200710-02
MBV :GAUL-201008-0E
RJB :GAFD-160513-01
RJP :GAGD-161220-01
CSTA:GAHD-190729-01
CSTB:GAHD-190729-01
CSTC:GAHD-190729-01
CSTD:GAJF-190514-N0
SCST:GAID-190729-01
ML  :GALA-190705-03
MU  :GAMA-190705-03
MS  :GANA-190705-01
SBV :GAUM-200930-01');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250923', '0910025380', '
==========================
   ◈ POWER OFF/ON ◈
0706 415 0115 
일      자 : 20250923
시      간 : 09:10:01
UPDATE    : 25.08.08
AP VERSION: V999999_O
SD R/M:13,051MB
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250922', '0859545310', '        ◈ OFF - LINE ◈
        ◈ 개국명세표 ◈
HOST        :   172.25.221.64
PORT        :            8200
LOCAL       :  172.27.128.242
GATE        :    172.27.128.1
NETMASK     :   255.255.255.0
금고번호(점번) :    00706
기        번 :      415
일 련   번 호 :     0114
개 국   일 자 : 20250922
개 국   시 간 : 08:59:52
HOST MS차단  : 미차단
장애인　  모드 : 실  행(정상)
근접센서  모드 : 사용
R F 버전     : HS-N-08 
미수취함 설정: 수표회수함
TOTA:GCEK-200916- G3
BVTO:KRW -190201-  0
MAIN:GAAJ-200909-G2
TSK :GACA-180905-01
CAM :GADA-200710-02
MBV :GAUL-201008-0E
RJB :GAFD-160513-01
RJP :GAGD-161220-01
CSTA:GAHD-190729-01
CSTB:GAHD-190729-01
CSTC:GAHD-190729-01
CSTD:GAJF-190514-N0
SCST:GAID-190729-01
ML  :GALA-190705-03
MU  :GAMA-190705-03
MS  :GANA-190705-01
SBV :GAUM-200930-01');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250922', '0806485300', '
==========================
   ◈ POWER OFF/ON ◈
0706 415 0113 
일      자 : 20250922
시      간 : 08:06:48
UPDATE    : 25.08.08
AP VERSION: V999999_O
SD R/M:13,052MB
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250922', '0859555320', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 정상 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250922', '0901065340', '계좌:[9002207423734   ]
----------------------------
[카드입금]
  ◈ OFF-LINE 명세표 ◈

20250922007064150115카입금

 09004  9004-03-000061-7

    *30,000*     *150*
                    **


022     09:00:47  00034

      출금수표: 00매
      입금수표: 00매
ATEC AP 

');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250922', '1743105360', '▤▤▤▤ 전원  차단 ▤▤▤▤
일 자 : 20250922
시 간 : 17:43:10
사 유 : 계원화면 전원버튼 종료
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1632284650', '     ◈ TIME OUT 발생 ◈
0706 415 0105 
일    자:            20250920
시    간:            16:32:27
계좌번호:       9002207423734
현금수취 TIME OUT 
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '0925594280', '
==========================
   ◈ POWER OFF/ON ◈
0706 415 0098 
일      자 : 20250920
시      간 : 09:25:59
UPDATE    : 25.08.08
AP VERSION: V999999_O
SD R/M:13,053MB
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1632264630', '
    ◈ 지폐 회수표 ◈
0706 415 0105    *10,000
일자:20250920    시간:16:32:20
회수함위치: 미수취함
오  만  원  권:           0 매
만    원    권:           1 매
오  천  원  권:           0 매
천    원    권:           0 매
식별 불능 현금:           0 매
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1632044610', '계좌:[9002207423734   ]
----------------------------
[카드출금]
  ◈ OFF-LINE 명세표 ◈

20250920007064150105카출금

 09004  9004-03-000061-7

    *10,000*     *150*
                    **


022     16:30:54  00034

      출금수표: 00매
      입금수표: 00매
ATEC AP 

');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1622484590', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 정상 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1622474580', '
==========================
        ◈ OFF - LINE ◈
        ◈ 개국명세표 ◈
HOST        :   172.25.221.64
PORT        :            8200
LOCAL       :  172.27.128.242
GATE        :    172.27.128.1
NETMASK     :   255.255.255.0
금고번호(점번) :    00706
기        번 :      415
일 련   번 호 :     0104
개 국   일 자 : 20250920
개 국   시 간 : 16:22:44
HOST MS차단  : 미차단
장애인　  모드 : 실  행(정상)
근접센서  모드 : 사용
R F 버전     : HS-N-08 
미수취함 설정: 수표회수함
TOTA:GCEK-200916- G3
BVTO:KRW -190201-  0
MAIN:GAAJ-200909-G2
TSK :GACA-180905-01
CAM :GADA-200710-02
MBV :GAUL-201008-0E
RJB :GAFD-160513-01
RJP :GAGD-161220-01
CSTA:GAHD-190729-01
CSTB:GAHD-190729-01
CSTC:GAHD-190729-01
CSTD:GAJF-190514-N0
SCST:GAID-190729-01
ML  :GALA-190705-03
MU  :GAMA-190705-03
MS  :GANA-190705-01
SBV :GAUM-200930-01');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1620064560', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 정상 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1620054550', '
   ◈ 장애 복구 완료 ◈
 일     자:   20250920
 시     간:   16:20:04
    장애가 정상 복구되었습니다
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1619594540', '
     ◈ 장애명세표 ◈
 일     자:  20250920
 시     간:  16:19:58
 일련  번호:      0103
 장애 STEP:    [INSERVICE]
 모듈[09] 코드[32] 상세코드[0421]
 내용:고객조작 타임아웃(미수취)

==========================
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1619454520', '     ◈ TIME OUT 발생 ◈
0706 415 0103 
일    자:            20250920
시    간:            16:19:44
계좌번호:       9002207423734
명세표수취 TIME OUT 
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1619444500', '  ◈ OFF-LINE 명세표 ◈

20250920007064150103카출금

 09004  9004-03-000061-7

    *10,000*     *150*
                    **


022     16:19:36  00034

      출금수표: 00매
      입금수표: 00매
ATEC AP 

');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1619364480', '
    ◈ 지폐 회수표 ◈
0706 415 0103    *10,000
일자:20250920    시간:16:19:30
회수함위치: 미수취함
오  만  원  권:           0 매
만    원    권:           1 매
오  천  원  권:           0 매
천    원    권:           0 매
식별 불능 현금:           0 매
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1619134460', '계좌:[9002207423734   ]
----------------------------
[카드출금]
  ◈ OFF-LINE 명세표 ◈

20250920007064150103카출금

 09004  9004-03-000061-7

    *10,000*     *150*
                    **


022     16:18:16  00034

      출금수표: 00매
      입금수표: 00매
ATEC AP 

');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1617494440', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 정상 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1617484430', '        ◈ OFF - LINE ◈
        ◈ 개국명세표 ◈
HOST        :   172.25.221.64
PORT        :            8200
LOCAL       :  172.27.128.242
GATE        :    172.27.128.1
NETMASK     :   255.255.255.0
금고번호(점번) :    00706
기        번 :      415
일 련   번 호 :     0102
개 국   일 자 : 20250920
개 국   시 간 : 16:17:46
HOST MS차단  : 미차단
장애인　  모드 : 실  행(정상)
근접센서  모드 : 사용
R F 버전     : HS-N-08 
미수취함 설정: 수표회수함
TOTA:GCEK-200916- G3
BVTO:KRW -190201-  0
MAIN:GAAJ-200909-G2
TSK :GACA-180905-01
CAM :GADA-200710-02
MBV :GAUL-201008-0E
RJB :GAFD-160513-01
RJP :GAGD-161220-01
CSTA:GAHD-190729-01
CSTB:GAHD-190729-01
CSTC:GAHD-190729-01
CSTD:GAJF-190514-N0
SCST:GAID-190729-01
ML  :GALA-190705-03
MU  :GAMA-190705-03
MS  :GANA-190705-01
SBV :GAUM-200930-01');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1617214420', '
==========================
   ◈ POWER OFF/ON ◈
0706 415 0101 
일      자 : 20250920
시      간 : 16:17:21
UPDATE    : 25.08.08
AP VERSION: V999999_O
SD R/M:13,053MB
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1615434400', '▤▤▤▤ 전원  차단 ▤▤▤▤
일 자 : 20250920
시 간 : 16:15:42
사 유 : 기기 설정값(BBM) 변경
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1614564380', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 정상 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1614554370', '
   ◈ 장애 복구 완료 ◈
 일     자:   20250920
 시     간:   16:14:54
    장애가 정상 복구되었습니다
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1614504360', '      ◈ 비정상 P-OFF ◈
706 415 0099 
일     자:            20250920
시     간:            16:14:46
계좌 번호:         90022074237
금     액:                *0원
[카드출금]중 전원 OFF

     ◈ 장애명세표 ◈
 일     자:  20250920
 시     간:  16:14:48
 일련  번호:      0099
 장애 STEP:    [POWERUP]
 모듈[09] 코드[31] 상세코드[0117]
 내용:비정상 POWER OFF

==========================
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1614254340', '
==========================
   ◈ POWER OFF/ON ◈
0706 415 0099 
일      자 : 20250920
시      간 : 16:14:24
UPDATE    : 25.08.08
AP VERSION: V999999_O
SD R/M:13,053MB
PON중 장애 발생함 
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1609514320', '▤▤▤▤ 전원  차단 ▤▤▤▤
일 자 : 20250920
시 간 : 16:09:50
사 유 : 비정상 종료
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1131534300', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 정상 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1131524290', '        ◈ OFF - LINE ◈
        ◈ 개국명세표 ◈
HOST        :   172.25.221.64
PORT        :            8200
LOCAL       :  172.27.128.242
GATE        :    172.27.128.1
NETMASK     :   255.255.255.0
금고번호(점번) :    00706
기        번 :      415
일 련   번 호 :     0099
개 국   일 자 : 20250920
개 국   시 간 : 11:31:49
HOST MS차단  : 미차단
장애인　  모드 : 실  행(정상)
근접센서  모드 : 사용
R F 버전     : HS-N-08 
미수취함 설정: 수표회수함
TOTA:GCEK-200916- G3
BVTO:KRW -190201-  0
MAIN:GAAJ-200909-G2
TSK :GACA-180905-01
CAM :GADA-200710-02
MBV :GAUL-201008-0E
RJB :GAFD-160513-01
RJP :GAGD-161220-01
CSTA:GAHD-190729-01
CSTB:GAHD-190729-01
CSTC:GAHD-190729-01
CSTD:GAJF-190514-N0
SCST:GAID-190729-01
ML  :GALA-190705-03
MU  :GAMA-190705-03
MS  :GANA-190705-01
SBV :GAUM-200930-01');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1858285030', '계좌:[9002207423734   ]
----------------------------
[카드출금]
  ◈ OFF-LINE 명세표 ◈

20250920007064150111카출금

 09004  9004-03-000061-7

    *10,000*     *150*
                    **


022     18:57:17  00034

      출금수표: 00매
      입금수표: 00매
ATEC AP 

');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1802504840', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 정상 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1856555010', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 정상 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1856545000', '
==========================
        ◈ OFF - LINE ◈
        ◈ 개국명세표 ◈
HOST        :   172.25.221.64
PORT        :            8200
LOCAL       :  172.27.128.242
GATE        :    172.27.128.1
NETMASK     :   255.255.255.0
금고번호(점번) :    00706
기        번 :      415
일 련   번 호 :     0110
개 국   일 자 : 20250920
개 국   시 간 : 18:56:51
HOST MS차단  : 미차단
장애인　  모드 : 실  행(정상)
근접센서  모드 : 사용
R F 버전     : HS-N-08 
미수취함 설정: 수표회수함
TOTA:GCEK-200916- G3
BVTO:KRW -190201-  0
MAIN:GAAJ-200909-G2
TSK :GACA-180905-01
CAM :GADA-200710-02
MBV :GAUL-201008-0E
RJB :GAFD-160513-01
RJP :GAGD-161220-01
CSTA:GAHD-190729-01
CSTB:GAHD-190729-01
CSTC:GAHD-190729-01
CSTD:GAJF-190514-N0
SCST:GAID-190729-01
ML  :GALA-190705-03
MU  :GAMA-190705-03
MS  :GANA-190705-01
SBV :GAUM-200930-01');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1837494980', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 정상 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1837484970', '
   ◈ 장애 복구 완료 ◈
 일     자:   20250920
 시     간:   18:37:47
    장애가 정상 복구되었습니다
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1837424960', '
     ◈ 장애명세표 ◈
 일     자:  20250920
 시     간:  18:37:41
 일련  번호:      0109
 장애 STEP:    [INSERVICE]
 모듈[09] 코드[32] 상세코드[0421]
 내용:고객조작 타임아웃(미수취)

==========================
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1837354950', '  ◈ OFF-LINE 명세표 ◈

20250920007064150109카출금




          현금출금 취소
    2025-09-20   18:37


취소거래T.O잔액조회요망
장애CODE : 09-32-0421
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1837214930', '     ◈ TIME OUT 발생 ◈
0706 415 0109 
일    자:            20250920
시    간:            18:37:20
계좌번호:       9002207423734
현금수취 TIME OUT 
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1837204910', '
    ◈ 지폐 회수표 ◈
0706 415 0109    *10,000
일자:20250920    시간:18:37:13
회수함위치: 미수취함
오  만  원  권:           0 매
만    원    권:           1 매
오  천  원  권:           0 매
천    원    권:           0 매
식별 불능 현금:           0 매
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1836574890', '계좌:[9002207423734   ]
----------------------------
[카드출금]
  ◈ OFF-LINE 명세표 ◈

20250920007064150109카출금

 09004  9004-03-000061-7

    *10,000*     *150*
                    **


022     18:35:50  00034

      출금수표: 00매
      입금수표: 00매
ATEC AP 

');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1835004870', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 정상 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1834594860', '
==========================
        ◈ OFF - LINE ◈
        ◈ 개국명세표 ◈
HOST        :   172.25.221.64
PORT        :            8200
LOCAL       :  172.27.128.242
GATE        :    172.27.128.1
NETMASK     :   255.255.255.0
금고번호(점번) :    00706
기        번 :      415
일 련   번 호 :     0108
개 국   일 자 : 20250920
개 국   시 간 : 18:34:56
HOST MS차단  : 미차단
장애인　  모드 : 실  행(정상)
근접센서  모드 : 사용
R F 버전     : HS-N-08 
미수취함 설정: 수표회수함
TOTA:GCEK-200916- G3
BVTO:KRW -190201-  0
MAIN:GAAJ-200909-G2
TSK :GACA-180905-01
CAM :GADA-200710-02
MBV :GAUL-201008-0E
RJB :GAFD-160513-01
RJP :GAGD-161220-01
CSTA:GAHD-190729-01
CSTB:GAHD-190729-01
CSTC:GAHD-190729-01
CSTD:GAJF-190514-N0
SCST:GAID-190729-01
ML  :GALA-190705-03
MU  :GAMA-190705-03
MS  :GANA-190705-01
SBV :GAUM-200930-01');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1632414670', '  ◈ OFF-LINE 명세표 ◈

20250920007064150105카출금




          현금출금 취소
    2025-09-20   16:32


취소거래T.O잔액조회요망
장애CODE : 09-32-0421
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1802504830', '
   ◈ 장애 복구 완료 ◈
 일     자:   20250920
 시     간:   18:02:49
    장애가 정상 복구되었습니다
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1802444820', '
     ◈ 장애명세표 ◈
 일     자:  20250920
 시     간:  18:02:42
 일련  번호:      0107
 장애 STEP:    [INSERVICE]
 모듈[09] 코드[32] 상세코드[0421]
 내용:고객조작 타임아웃(미수취)

==========================
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1802364810', '  ◈ OFF-LINE 명세표 ◈

20250920007064150107카출금




          현금출금 취소
    2025-09-20   18:02


취소거래T.O잔액조회요망
장애CODE : 09-32-0421
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1802224790', '     ◈ TIME OUT 발생 ◈
0706 415 0107 
일    자:            20250920
시    간:            18:02:21
계좌번호:       9002207423734
현금수취 TIME OUT 
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1802214770', '
    ◈ 지폐 회수표 ◈
0706 415 0107    *10,000
일자:20250920    시간:18:02:14
회수함위치: 미수취함
오  만  원  권:           0 매
만    원    권:           1 매
오  천  원  권:           0 매
천    원    권:           0 매
식별 불능 현금:           0 매
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1801584750', '계좌:[9002207423734   ]
----------------------------
[카드출금]
  ◈ OFF-LINE 명세표 ◈

20250920007064150107카출금

 09004  9004-03-000061-7

    *10,000*     *150*
                    **


022     18:00:28  00034

      출금수표: 00매
      입금수표: 00매
ATEC AP 

');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1642184730', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 정상 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1642174720', '
==========================
        ◈ OFF - LINE ◈
        ◈ 개국명세표 ◈
HOST        :   172.25.221.64
PORT        :            8200
LOCAL       :  172.27.128.242
GATE        :    172.27.128.1
NETMASK     :   255.255.255.0
금고번호(점번) :    00706
기        번 :      415
일 련   번 호 :     0106
개 국   일 자 : 20250920
개 국   시 간 : 16:42:14
HOST MS차단  : 미차단
장애인　  모드 : 실  행(정상)
근접센서  모드 : 사용
R F 버전     : HS-N-08 
미수취함 설정: 수표회수함
TOTA:GCEK-200916- G3
BVTO:KRW -190201-  0
MAIN:GAAJ-200909-G2
TSK :GACA-180905-01
CAM :GADA-200710-02
MBV :GAUL-201008-0E
RJB :GAFD-160513-01
RJP :GAGD-161220-01
CSTA:GAHD-190729-01
CSTB:GAHD-190729-01
CSTC:GAHD-190729-01
CSTD:GAJF-190514-N0
SCST:GAID-190729-01
ML  :GALA-190705-03
MU  :GAMA-190705-03
MS  :GANA-190705-01
SBV :GAUM-200930-01');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1632564700', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 정상 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1632554690', '
   ◈ 장애 복구 완료 ◈
 일     자:   20250920
 시     간:   16:32:54
    장애가 정상 복구되었습니다
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1632494680', '
     ◈ 장애명세표 ◈
 일     자:  20250920
 시     간:  16:32:48
 일련  번호:      0105
 장애 STEP:    [INSERVICE]
 모듈[09] 코드[32] 상세코드[0421]
 내용:고객조작 타임아웃(미수취)

==========================
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1930025280', '▤▤▤▤ 전원  차단 ▤▤▤▤
일 자 : 20250920
시 간 : 19:30:02
사 유 : 계원화면 전원버튼 종료
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1858515050', '
    ◈ 지폐 회수표 ◈
0706 415 0111    *10,000
일자:20250920    시간:18:58:44
회수함위치: 미수취함
오  만  원  권:           0 매
만    원    권:           1 매
오  천  원  권:           0 매
천    원    권:           0 매
식별 불능 현금:           0 매
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1858525070', '     ◈ TIME OUT 발생 ◈
0706 415 0111 
일    자:            20250920
시    간:            18:58:51
계좌번호:       9002207423734
현금수취 TIME OUT 
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1859065090', '  ◈ OFF-LINE 명세표 ◈

20250920007064150111카출금




          현금출금 취소
    2025-09-20   18:58


취소거래T.O잔액조회요망
장애CODE : 09-32-0421
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1859145100', '
     ◈ 장애명세표 ◈
 일     자:  20250920
 시     간:  18:59:12
 일련  번호:      0111
 장애 STEP:    [INSERVICE]
 모듈[09] 코드[32] 상세코드[0421]
 내용:고객조작 타임아웃(미수취)

==========================
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1859195110', '
   ◈ 장애 복구 완료 ◈
 일     자:   20250920
 시     간:   18:59:19
    장애가 정상 복구되었습니다
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1859205120', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 정상 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1912345140', '
==========================
        ◈ OFF - LINE ◈
        ◈ 개국명세표 ◈
HOST        :   172.25.221.64
PORT        :            8200
LOCAL       :  172.27.128.242
GATE        :    172.27.128.1
NETMASK     :   255.255.255.0
금고번호(점번) :    00706
기        번 :      415
일 련   번 호 :     0112
개 국   일 자 : 20250920
개 국   시 간 : 19:12:32
HOST MS차단  : 미차단
장애인　  모드 : 실  행(정상)
근접센서  모드 : 사용
R F 버전     : HS-N-08 
미수취함 설정: 수표회수함
TOTA:GCEK-200916- G3
BVTO:KRW -190201-  0
MAIN:GAAJ-200909-G2
TSK :GACA-180905-01
CAM :GADA-200710-02
MBV :GAUL-201008-0E
RJB :GAFD-160513-01
RJP :GAGD-161220-01
CSTA:GAHD-190729-01
CSTB:GAHD-190729-01
CSTC:GAHD-190729-01
CSTD:GAJF-190514-N0
SCST:GAID-190729-01
ML  :GALA-190705-03
MU  :GAMA-190705-03
MS  :GANA-190705-01
SBV :GAUM-200930-01');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1912365150', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 정상 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1917105190', '
    ◈ 지폐 회수표 ◈
0706 415 0113    *10,000
일자:20250920    시간:19:17:04
회수함위치: 미수취함
오  만  원  권:           0 매
만    원    권:           1 매
오  천  원  권:           0 매
천    원    권:           0 매
식별 불능 현금:           0 매
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1917115210', '     ◈ TIME OUT 발생 ◈
0706 415 0113 
일    자:            20250920
시    간:            19:17:11
계좌번호:       9002207423734
현금수취 TIME OUT 
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1917265230', '  ◈ OFF-LINE 명세표 ◈

20250920007064150113카출금




          현금출금 취소
    2025-09-20   19:17


취소거래T.O잔액조회요망
장애CODE : 09-32-0421
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1917335240', '
     ◈ 장애명세표 ◈
 일     자:  20250920
 시     간:  19:17:32
 일련  번호:      0113
 장애 STEP:    [INSERVICE]
 모듈[09] 코드[32] 상세코드[0421]
 내용:고객조작 타임아웃(미수취)

==========================
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1917395250', '
   ◈ 장애 복구 완료 ◈
 일     자:   20250920
 시     간:   19:17:38
    장애가 정상 복구되었습니다
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1917405260', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 정상 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250920', '1916475170', '계좌:[9002207423734   ]
----------------------------
[카드출금]
  ◈ OFF-LINE 명세표 ◈

20250920007064150113카출금

 09004  9004-03-000061-7

    *10,000*     *150*
                    **


022     19:15:35  00034

      출금수표: 00매
      입금수표: 00매
ATEC AP 

');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250919', '1824584260', '▤▤▤▤ 전원  차단 ▤▤▤▤
일 자 : 20250919
시 간 : 18:24:58
사 유 : 계원화면 전원버튼 종료
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250919', '0851434240', '계좌:[9002207423734   ]
----------------------------
[카드입금]
  ◈ OFF-LINE 명세표 ◈

20250919007064150098카입금

 09004  9004-03-000061-7

    *30,000*     *150*
                    **


022     08:51:21  00034

      출금수표: 00매
      입금수표: 00매
ATEC AP 

');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250919', '0850244220', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 정상 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250919', '0850074190', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 정상 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250919', '0850064180', '        ◈ OFF - LINE ◈
        ◈ 개국명세표 ◈
HOST        :   172.25.221.64
PORT        :            8200
LOCAL       :  172.27.128.242
GATE        :    172.27.128.1
NETMASK     :   255.255.255.0
금고번호(점번) :    00706
기        번 :      415
일 련   번 호 :     0097
개 국   일 자 : 20250919
개 국   시 간 : 08:50:04
HOST MS차단  : 미차단
장애인　  모드 : 실  행(정상)
근접센서  모드 : 사용
R F 버전     : HS-N-08 
미수취함 설정: 수표회수함
TOTA:GCEK-200916- G3
BVTO:KRW -190201-  0
MAIN:GAAJ-200909-G2
TSK :GACA-180905-01
CAM :GADA-200710-02
MBV :GAUL-201008-0E
RJB :GAFD-160513-01
RJP :GAGD-161220-01
CSTA:GAHD-190729-01
CSTB:GAHD-190729-01
CSTC:GAHD-190729-01
CSTD:GAJF-190514-N0
SCST:GAID-190729-01
ML  :GALA-190705-03
MU  :GAMA-190705-03
MS  :GANA-190705-01
SBV :GAUM-200930-01');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250919', '0848444170', '
==========================
   ◈ POWER OFF/ON ◈
0706 415 0096 
일      자 : 20250919
시      간 : 08:48:43
UPDATE    : 25.08.08
AP VERSION: V999999_O
SD R/M:13,053MB
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250919', '0844204150', '▤▤▤▤ 전원  차단 ▤▤▤▤
일 자 : 20250919
시 간 : 08:44:20
사 유 : 기기 설정값(BBM) 변경
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250919', '0842184130', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 정상 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250919', '0802073990', '
==========================
   ◈ POWER OFF/ON ◈
0706 415 0088 
일      자 : 20250919
시      간 : 08:02:06
UPDATE    : 25.08.08
AP VERSION: V999999_O
SD R/M:13,053MB
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250919', '0827154000', '        ◈ OFF - LINE ◈
        ◈ 개국명세표 ◈
HOST        :   172.25.221.64
PORT        :            8200
LOCAL       :  172.27.128.242
GATE        :    172.27.128.1
NETMASK     :   255.255.255.0
금고번호(점번) :    00706
기        번 :      415
일 련   번 호 :     0089
개 국   일 자 : 20250919
개 국   시 간 : 08:27:12
HOST MS차단  : 미차단
장애인　  모드 : 실  행(정상)
근접센서  모드 : 사용
R F 버전     : HS-N-08 
미수취함 설정: 수표회수함
TOTA:GCEK-200916- G3
BVTO:KRW -190201-  0
MAIN:GAAJ-200909-G2
TSK :GACA-180905-01
CAM :GADA-200710-02
MBV :GAUL-201008-0E
RJB :GAFD-160513-01
RJP :GAGD-161220-01
CSTA:GAHD-190729-01
CSTB:GAHD-190729-01
CSTC:GAHD-190729-01
CSTD:GAJF-190514-N0
SCST:GAID-190729-01
ML  :GALA-190705-03
MU  :GAMA-190705-03
MS  :GANA-190705-01
SBV :GAUM-200930-01');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250919', '0827164010', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 정상 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250919', '0830484040', '        ◈ OFF - LINE ◈
        ◈ 개국명세표 ◈
HOST        :   172.25.221.64
PORT        :            8200
LOCAL       :  172.27.128.242
GATE        :    172.27.128.1
NETMASK     :   255.255.255.0
금고번호(점번) :    00706
기        번 :      415
일 련   번 호 :     0090
개 국   일 자 : 20250919
개 국   시 간 : 08:30:45
HOST MS차단  : 미차단
장애인　  모드 : 실  행(정상)
근접센서  모드 : 사용
R F 버전     : HS-N-08 
미수취함 설정: 수표회수함
TOTA:GCEK-200916- G3
BVTO:KRW -190201-  0
MAIN:GAAJ-200909-G2
TSK :GACA-180905-01
CAM :GADA-200710-02
MBV :GAUL-201008-0E
RJB :GAFD-160513-01
RJP :GAGD-161220-01
CSTA:GAHD-190729-01
CSTB:GAHD-190729-01
CSTC:GAHD-190729-01
CSTD:GAJF-190514-N0
SCST:GAID-190729-01
ML  :GALA-190705-03
MU  :GAMA-190705-03
MS  :GANA-190705-01
SBV :GAUM-200930-01');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250919', '0830494050', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 정상 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250919', '0836434070', '▤▤▤▤ 전원  차단 ▤▤▤▤
일 자 : 20250919
시 간 : 08:36:43
사 유 : 비정상 종료
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250919', '0841464090', '
==========================
   ◈ POWER OFF/ON ◈
0706 415 0090 
일      자 : 20250919
시      간 : 08:41:46
UPDATE    : 25.08.08
AP VERSION: V999999_O
SD R/M:13,053MB
PON중 장애 발생함 
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250919', '0842124110', '      ◈ 비정상 P-OFF ◈
706 415 0090 
일     자:            20250919
시     간:            08:42:08
계좌 번호:         90022074237
금     액:                *0원
[카드출금]중 전원 OFF

     ◈ 장애명세표 ◈
 일     자:  20250919
 시     간:  08:42:11
 일련  번호:      0090
 장애 STEP:    [POWERUP]
 모듈[09] 코드[31] 상세코드[0117]
 내용:비정상 POWER OFF

==========================
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250919', '0842174120', '
   ◈ 장애 복구 완료 ◈
 일     자:   20250919
 시     간:   08:42:16
    장애가 정상 복구되었습니다
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1506322680', '     ◈ TIME OUT 발생 ◈
0706 415 0067 
일    자:            20250918
시    간:            15:06:31
계좌번호:       9002207423734
명세표수취 TIME OUT 
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1512582810', '     ◈ TIME OUT 발생 ◈
0706 415 0069 
일    자:            20250918
시    간:            15:12:57
계좌번호:       9002207423734
현금수취 TIME OUT 
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1512562790', '
    ◈ 지폐 회수표 ◈
0706 415 0069    *10,000
일자:20250918    시간:15:12:50
회수함위치: 미수취함
오  만  원  권:           0 매
만    원    권:           1 매
오  천  원  권:           0 매
천    원    권:           0 매
식별 불능 현금:           0 매
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1512342770', '계좌:[9002207423734   ]
----------------------------
[카드출금]
  ◈ OFF-LINE 명세표 ◈

20250918007064150069카출금

 09004  9004-03-000061-7

    *10,000*     *150*
                    **


022     15:11:27  00034

      출금수표: 00매
      입금수표: 00매
ATEC AP 

');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1509532750', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 정상 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1509522740', '
==========================
        ◈ OFF - LINE ◈
        ◈ 개국명세표 ◈
HOST        :   172.25.221.64
PORT        :            8200
LOCAL       :  172.27.128.242
GATE        :    172.27.128.1
NETMASK     :   255.255.255.0
금고번호(점번) :    00706
기        번 :      415
일 련   번 호 :     0068
개 국   일 자 : 20250918
개 국   시 간 : 15:09:49
HOST MS차단  : 미차단
장애인　  모드 : 실  행(정상)
근접센서  모드 : 사용
R F 버전     : HS-N-08 
미수취함 설정: 수표회수함
TOTA:GCEK-200916- G3
BVTO:KRW -190201-  0
MAIN:GAAJ-200909-G2
TSK :GACA-180905-01
CAM :GADA-200710-02
MBV :GAUL-201008-0E
RJB :GAFD-160513-01
RJP :GAGD-161220-01
CSTA:GAHD-190729-01
CSTB:GAHD-190729-01
CSTC:GAHD-190729-01
CSTD:GAJF-190514-N0
SCST:GAID-190729-01
ML  :GALA-190705-03
MU  :GAMA-190705-03
MS  :GANA-190705-01
SBV :GAUM-200930-01');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1506532720', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 정상 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1506522710', '
   ◈ 장애 복구 완료 ◈
 일     자:   20250918
 시     간:   15:06:52
    장애가 정상 복구되었습니다
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1506462700', '
     ◈ 장애명세표 ◈
 일     자:  20250918
 시     간:  15:06:45
 일련  번호:      0067
 장애 STEP:    [INSERVICE]
 모듈[09] 코드[32] 상세코드[0421]
 내용:고객조작 타임아웃(미수취)

==========================
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1513122830', '  ◈ OFF-LINE 명세표 ◈

20250918007064150069카출금




          현금출금 취소
    2025-09-18   15:12


취소거래T.O잔액조회요망
장애CODE : 09-32-0421
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1506312660', '  ◈ OFF-LINE 명세표 ◈

20250918007064150067카출금

 09004  9004-03-000061-7

    *10,000*     *150*
                    **


022     15:06:23  00034

      출금수표: 00매
      입금수표: 00매
ATEC AP 

');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1506232640', '
    ◈ 지폐 회수표 ◈
0706 415 0067    *10,000
일자:20250918    시간:15:06:17
회수함위치: 미수취함
오  만  원  권:           0 매
만    원    권:           1 매
오  천  원  권:           0 매
천    원    권:           0 매
식별 불능 현금:           0 매
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1506012620', '계좌:[9002207423734   ]
----------------------------
[카드출금]
  ◈ OFF-LINE 명세표 ◈

20250918007064150067카출금

 09004  9004-03-000061-7

    *10,000*     *150*
                    **


022     15:05:02  00034

      출금수표: 00매
      입금수표: 00매
ATEC AP 

');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1259072600', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 정상 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1259062590', '        ◈ OFF - LINE ◈
        ◈ 개국명세표 ◈
HOST        :   172.25.221.64
PORT        :            8200
LOCAL       :  172.27.128.242
GATE        :    172.27.128.1
NETMASK     :   255.255.255.0
금고번호(점번) :    00706
기        번 :      415
일 련   번 호 :     0066
개 국   일 자 : 20250918
개 국   시 간 : 12:59:03
HOST MS차단  : 미차단
장애인　  모드 : 실  행(정상)
근접센서  모드 : 사용
R F 버전     : HS-N-08 
미수취함 설정: 수표회수함
TOTA:GCEK-200916- G3
BVTO:KRW -190201-  0
MAIN:GAAJ-200909-G2
TSK :GACA-180905-01
CAM :GADA-200710-02
MBV :GAUL-201008-0E
RJB :GAFD-160513-01
RJP :GAGD-161220-01
CSTA:GAHD-190729-01
CSTB:GAHD-190729-01
CSTC:GAHD-190729-01
CSTD:GAJF-190514-N0
SCST:GAID-190729-01
ML  :GALA-190705-03
MU  :GAMA-190705-03
MS  :GANA-190705-01
SBV :GAUM-200930-01');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '0907072580', '
==========================
   ◈ POWER OFF/ON ◈
0706 415 0065 
일      자 : 20250918
시      간 : 09:07:06
UPDATE    : 25.08.08
AP VERSION: V999999_O
SD R/M:13,056MB
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1614313090', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 부족예보 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1513192840', '
     ◈ 장애명세표 ◈
 일     자:  20250918
 시     간:  15:13:18
 일련  번호:      0069
 장애 STEP:    [INSERVICE]
 모듈[09] 코드[32] 상세코드[0421]
 내용:고객조작 타임아웃(미수취)

==========================
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1613543070', '
 ◈ 수동 장애복구 실시 ◈
 일     자:   20250918
 시     간:   16:13:52

==========================
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1543383060', '계좌:[9002207423734   ]
----------------------------
[카드입금]
  ◈ OFF-LINE 명세표 ◈

20250918007064150073카입금

 09004  9004-03-000061-7

    *10,000*     *150*
                    **


022     15:42:36  00034

      출금수표: 00매
      입금수표: 00매
ATEC AP 


     ◈ 장애명세표 ◈
 일     자:  20250918
 시     간:  15:43:36
 일련  번호:      0073
 장애 STEP:    [INSERVICE]
 모듈[33] 코드[99] 상세코드[00A1]
 내용:현금부 동작 이상

==========================
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1541323030', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 부족예보 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1541313020', '
==========================
        ◈ OFF - LINE ◈
        ◈ 개국명세표 ◈
HOST        :   172.25.221.64
PORT        :            8200
LOCAL       :  172.27.128.242
GATE        :    172.27.128.1
NETMASK     :   255.255.255.0
금고번호(점번) :    00706
기        번 :      415
일 련   번 호 :     0072
개 국   일 자 : 20250918
개 국   시 간 : 15:41:28
HOST MS차단  : 미차단
장애인　  모드 : 실  행(정상)
근접센서  모드 : 사용
R F 버전     : HS-N-08 
미수취함 설정: 수표회수함
TOTA:GCEK-200916- G3
BVTO:KRW -190201-  0
MAIN:GAAJ-200909-G2
TSK :GACA-180905-01
CAM :GADA-200710-02
MBV :GAUL-201008-0E
RJB :GAFD-160513-01
RJP :GAGD-161220-01
CSTA:GAHD-190729-01
CSTB:GAHD-190729-01
CSTC:GAHD-190729-01
CSTD:GAJF-190514-N0
SCST:GAID-190729-01
ML  :GALA-190705-03
MU  :GAMA-190705-03
MS  :GANA-190705-01
SBV :GAUM-200930-01');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1527143000', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 정상 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1527132990', '
   ◈ 장애 복구 완료 ◈
 일     자:   20250918
 시     간:   15:27:12
    장애가 정상 복구되었습니다
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1527072980', '
     ◈ 장애명세표 ◈
 일     자:  20250918
 시     간:  15:27:06
 일련  번호:      0071
 장애 STEP:    [INSERVICE]
 모듈[09] 코드[32] 상세코드[0421]
 내용:고객조작 타임아웃(미수취)

==========================
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1527002970', '  ◈ OFF-LINE 명세표 ◈

20250918007064150071카출금




          현금출금 취소
    2025-09-18   15:26


취소거래T.O잔액조회요망
장애CODE : 09-32-0421
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1526462950', '     ◈ TIME OUT 발생 ◈
0706 415 0071 
일    자:            20250918
시    간:            15:26:45
계좌번호:       9002207423734
현금수취 TIME OUT 
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1526452930', '
    ◈ 지폐 회수표 ◈
0706 415 0071   *100,000
일자:20250918    시간:15:26:38
회수함위치: 미수취함
오  만  원  권:           1 매
만    원    권:           5 매
오  천  원  권:           0 매
천    원    권:           0 매
식별 불능 현금:           0 매
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1526212910', '계좌:[9002207423734   ]
----------------------------
[카드출금]
  ◈ OFF-LINE 명세표 ◈

20250918007064150071카출금

 09004  9004-03-000061-7

    *100,000*     *150*
                    **


022     15:25:08  00034

      출금수표: 00매
      입금수표: 00매
ATEC AP 

');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1514002890', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 정상 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1513592880', '
==========================
        ◈ OFF - LINE ◈
        ◈ 개국명세표 ◈
HOST        :   172.25.221.64
PORT        :            8200
LOCAL       :  172.27.128.242
GATE        :    172.27.128.1
NETMASK     :   255.255.255.0
금고번호(점번) :    00706
기        번 :      415
일 련   번 호 :     0070
개 국   일 자 : 20250918
개 국   시 간 : 15:13:56
HOST MS차단  : 미차단
장애인　  모드 : 실  행(정상)
근접센서  모드 : 사용
R F 버전     : HS-N-08 
미수취함 설정: 수표회수함
TOTA:GCEK-200916- G3
BVTO:KRW -190201-  0
MAIN:GAAJ-200909-G2
TSK :GACA-180905-01
CAM :GADA-200710-02
MBV :GAUL-201008-0E
RJB :GAFD-160513-01
RJP :GAGD-161220-01
CSTA:GAHD-190729-01
CSTB:GAHD-190729-01
CSTC:GAHD-190729-01
CSTD:GAJF-190514-N0
SCST:GAID-190729-01
ML  :GALA-190705-03
MU  :GAMA-190705-03
MS  :GANA-190705-01
SBV :GAUM-200930-01');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1513262860', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 정상 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1513252850', '
   ◈ 장애 복구 완료 ◈
 일     자:   20250918
 시     간:   15:13:24
    장애가 정상 복구되었습니다
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1736473280', '상단 Tray - 장착');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1743213490', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 정상 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1743053460', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 정상 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1743043450', '
==========================
        ◈ OFF - LINE ◈
        ◈ 개국명세표 ◈
HOST        :   172.25.221.64
PORT        :            8200
LOCAL       :  172.27.128.242
GATE        :    172.27.128.1
NETMASK     :   255.255.255.0
금고번호(점번) :    00706
기        번 :      415
일 련   번 호 :     0080
개 국   일 자 : 20250918
개 국   시 간 : 17:43:02
HOST MS차단  : 미차단
장애인　  모드 : 실  행(정상)
근접센서  모드 : 사용
R F 버전     : HS-N-08 
미수취함 설정: 수표회수함
TOTA:GCEK-200916- G3
BVTO:KRW -190201-  0
MAIN:GAAJ-200909-G2
TSK :GACA-180905-01
CAM :GADA-200710-02
MBV :GAUL-201008-0E
RJB :GAFD-160513-01
RJP :GAGD-161220-01
CSTA:GAHD-190729-01
CSTB:GAHD-190729-01
CSTC:GAHD-190729-01
CSTD:GAJF-190514-N0
SCST:GAID-190729-01
ML  :GALA-190705-03
MU  :GAMA-190705-03
MS  :GANA-190705-01
SBV :GAUM-200930-01');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1742203430', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 정상 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1742203420', '
   ◈ 장애 복구 완료 ◈
 일     자:   20250918
 시     간:   17:42:19
    장애가 정상 복구되었습니다
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1741443410', '
 ◈ 수동 장애복구 실시 ◈
 일     자:   20250918
 시     간:   17:41:42

==========================
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1740413400', '계좌:[9002207423734   ]
----------------------------
[카드입금]
  ◈ OFF-LINE 명세표 ◈

20250918007064150079카입금

 09004  9004-03-000061-7

    *10,000*     *150*
                    **


022     17:39:34  00034

      출금수표: 00매
      입금수표: 00매
ATEC AP 


     ◈ 장애명세표 ◈
 일     자:  20250918
 시     간:  17:40:40
 일련  번호:      0079
 장애 STEP:    [INSERVICE]
 모듈[33] 코드[99] 상세코드[00A1]
 내용:현금부 동작 이상

==========================
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1738203370', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 정상 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1738153340', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 정상 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1738143330', '
==========================
        ◈ OFF - LINE ◈
        ◈ 개국명세표 ◈
HOST        :   172.25.221.64
PORT        :            8200
LOCAL       :  172.27.128.242
GATE        :    172.27.128.1
NETMASK     :   255.255.255.0
금고번호(점번) :    00706
기        번 :      415
일 련   번 호 :     0078
개 국   일 자 : 20250918
개 국   시 간 : 17:38:12
HOST MS차단  : 미차단
장애인　  모드 : 실  행(정상)
근접센서  모드 : 사용
R F 버전     : HS-N-08 
미수취함 설정: 수표회수함
TOTA:GCEK-200916- G3
BVTO:KRW -190201-  0
MAIN:GAAJ-200909-G2
TSK :GACA-180905-01
CAM :GADA-200710-02
MBV :GAUL-201008-0E
RJB :GAFD-160513-01
RJP :GAGD-161220-01
CSTA:GAHD-190729-01
CSTB:GAHD-190729-01
CSTC:GAHD-190729-01
CSTD:GAJF-190514-N0
SCST:GAID-190729-01
ML  :GALA-190705-03
MU  :GAMA-190705-03
MS  :GANA-190705-01
SBV :GAUM-200930-01');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1737273310', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 부족예보 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1737263300', '
   ◈ 장애 복구 완료 ◈
 일     자:   20250918
 시     간:   17:37:26
    장애가 정상 복구되었습니다
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1736503290', '
 ◈ 수동 장애복구 실시 ◈
 일     자:   20250918
 시     간:   17:36:49

==========================
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1911293940', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 정상 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1736133270', '상단 Tray - 탈착');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1626233260', '계좌:[9002207423734   ]
----------------------------
[카드입금]
  ◈ OFF-LINE 명세표 ◈

20250918007064150077카입금

 09004  9004-03-000061-7

    *10,000*     *150*
                    **


022     16:25:21  00034

      출금수표: 00매
      입금수표: 00매
ATEC AP 


     ◈ 장애명세표 ◈
 일     자:  20250918
 시     간:  16:26:21
 일련  번호:      0077
 장애 STEP:    [INSERVICE]
 모듈[33] 코드[99] 상세코드[00A1]
 내용:현금부 동작 이상

==========================
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1624203230', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 부족예보 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1624193220', '
==========================
        ◈ OFF - LINE ◈
        ◈ 개국명세표 ◈
HOST        :   172.25.221.64
PORT        :            8200
LOCAL       :  172.27.128.242
GATE        :    172.27.128.1
NETMASK     :   255.255.255.0
금고번호(점번) :    00706
기        번 :      415
일 련   번 호 :     0076
개 국   일 자 : 20250918
개 국   시 간 : 16:24:17
HOST MS차단  : 미차단
장애인　  모드 : 실  행(정상)
근접센서  모드 : 사용
R F 버전     : HS-N-08 
미수취함 설정: 수표회수함
TOTA:GCEK-200916- G3
BVTO:KRW -190201-  0
MAIN:GAAJ-200909-G2
TSK :GACA-180905-01
CAM :GADA-200710-02
MBV :GAUL-201008-0E
RJB :GAFD-160513-01
RJP :GAGD-161220-01
CSTA:GAHD-190729-01
CSTB:GAHD-190729-01
CSTC:GAHD-190729-01
CSTD:GAJF-190514-N0
SCST:GAID-190729-01
ML  :GALA-190705-03
MU  :GAMA-190705-03
MS  :GANA-190705-01
SBV :GAUM-200930-01');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1624103200', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 부족예보 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1624083190', '
   ◈ 장애 복구 완료 ◈
 일     자:   20250918
 시     간:   16:24:08
    장애가 정상 복구되었습니다
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1623323180', '
 ◈ 수동 장애복구 실시 ◈
 일     자:   20250918
 시     간:   16:23:31

==========================
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1623283170', '상단 Tray - 장착');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1623263160', '상단 Tray - 탈착');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1616583150', '계좌:[9002207423734   ]
----------------------------
[카드입금]
  ◈ OFF-LINE 명세표 ◈

20250918007064150075카입금

 09004  9004-03-000061-7

    *10,000*     *150*
                    **


022     16:15:55  00034

      출금수표: 00매
      입금수표: 00매
ATEC AP 


     ◈ 장애명세표 ◈
 일     자:  20250918
 시     간:  16:16:57
 일련  번호:      0075
 장애 STEP:    [INSERVICE]
 모듈[33] 코드[99] 상세코드[00A1]
 내용:현금부 동작 이상

==========================
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1614433120', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 부족예보 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1614423110', '
==========================
        ◈ OFF - LINE ◈
        ◈ 개국명세표 ◈
HOST        :   172.25.221.64
PORT        :            8200
LOCAL       :  172.27.128.242
GATE        :    172.27.128.1
NETMASK     :   255.255.255.0
금고번호(점번) :    00706
기        번 :      415
일 련   번 호 :     0074
개 국   일 자 : 20250918
개 국   시 간 : 16:14:40
HOST MS차단  : 미차단
장애인　  모드 : 실  행(정상)
근접센서  모드 : 사용
R F 버전     : HS-N-08 
미수취함 설정: 수표회수함
TOTA:GCEK-200916- G3
BVTO:KRW -190201-  0
MAIN:GAAJ-200909-G2
TSK :GACA-180905-01
CAM :GADA-200710-02
MBV :GAUL-201008-0E
RJB :GAFD-160513-01
RJP :GAGD-161220-01
CSTA:GAHD-190729-01
CSTB:GAHD-190729-01
CSTC:GAHD-190729-01
CSTD:GAJF-190514-N0
SCST:GAID-190729-01
ML  :GALA-190705-03
MU  :GAMA-190705-03
MS  :GANA-190705-01
SBV :GAUM-200930-01');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1614293080', '
   ◈ 장애 복구 완료 ◈
 일     자:   20250918
 시     간:   16:14:29
    장애가 정상 복구되었습니다
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1747593530', '상단 Tray - 탈착');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1916353970', '▤▤▤▤ 전원  차단 ▤▤▤▤
일 자 : 20250918
시 간 : 19:16:34
사 유 : 계원화면 전원버튼 종료
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1911283930', '        ◈ OFF - LINE ◈
        ◈ 개국명세표 ◈
HOST        :   172.25.221.64
PORT        :            8200
LOCAL       :  172.27.128.242
GATE        :    172.27.128.1
NETMASK     :   255.255.255.0
금고번호(점번) :    00706
기        번 :      415
일 련   번 호 :     0088
개 국   일 자 : 20250918
개 국   시 간 : 19:11:26
HOST MS차단  : 미차단
장애인　  모드 : 실  행(정상)
근접센서  모드 : 사용
R F 버전     : HS-N-08 
미수취함 설정: 수표회수함
TOTA:GCEK-200916- G3
BVTO:KRW -190201-  0
MAIN:GAAJ-200909-G2
TSK :GACA-180905-01
CAM :GADA-200710-02
MBV :GAUL-201008-0E
RJB :GAFD-160513-01
RJP :GAGD-161220-01
CSTA:GAHD-190729-01
CSTB:GAHD-190729-01
CSTC:GAHD-190729-01
CSTD:GAJF-190514-N0
SCST:GAID-190729-01
ML  :GALA-190705-03
MU  :GAMA-190705-03
MS  :GANA-190705-01
SBV :GAUM-200930-01');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1905203900', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 정상 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1905193890', '        ◈ OFF - LINE ◈
        ◈ 개국명세표 ◈
HOST        :   172.25.221.64
PORT        :            8200
LOCAL       :  172.27.128.242
GATE        :    172.27.128.1
NETMASK     :   255.255.255.0
금고번호(점번) :    00706
기        번 :      415
일 련   번 호 :     0087
개 국   일 자 : 20250918
개 국   시 간 : 19:05:17
HOST MS차단  : 미차단
장애인　  모드 : 실  행(정상)
근접센서  모드 : 사용
R F 버전     : HS-N-08 
미수취함 설정: 수표회수함
TOTA:GCEK-200916- G3
BVTO:KRW -190201-  0
MAIN:GAAJ-200909-G2
TSK :GACA-180905-01
CAM :GADA-200710-02
MBV :GAUL-201008-0E
RJB :GAFD-160513-01
RJP :GAGD-161220-01
CSTA:GAHD-190729-01
CSTB:GAHD-190729-01
CSTC:GAHD-190729-01
CSTD:GAJF-190514-N0
SCST:GAID-190729-01
ML  :GALA-190705-03
MU  :GAMA-190705-03
MS  :GANA-190705-01
SBV :GAUM-200930-01');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1845143860', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 정상 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1845123850', '
==========================
        ◈ OFF - LINE ◈
        ◈ 개국명세표 ◈
HOST        :   172.25.221.64
PORT        :            8200
LOCAL       :  172.27.128.242
GATE        :    172.27.128.1
NETMASK     :   255.255.255.0
금고번호(점번) :    00706
기        번 :      415
일 련   번 호 :     0086
개 국   일 자 : 20250918
개 국   시 간 : 18:45:10
HOST MS차단  : 미차단
장애인　  모드 : 실  행(정상)
근접센서  모드 : 사용
R F 버전     : HS-N-08 
미수취함 설정: 수표회수함
TOTA:GCEK-200916- G3
BVTO:KRW -190201-  0
MAIN:GAAJ-200909-G2
TSK :GACA-180905-01
CAM :GADA-200710-02
MBV :GAUL-201008-0E
RJB :GAFD-160513-01
RJP :GAGD-161220-01
CSTA:GAHD-190729-01
CSTB:GAHD-190729-01
CSTC:GAHD-190729-01
CSTD:GAJF-190514-N0
SCST:GAID-190729-01
ML  :GALA-190705-03
MU  :GAMA-190705-03
MS  :GANA-190705-01
SBV :GAUM-200930-01');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1844243830', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 정상 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1844223820', '
   ◈ 장애 복구 완료 ◈
 일     자:   20250918
 시     간:   18:44:21
    장애가 정상 복구되었습니다
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1843473810', '
 ◈ 수동 장애복구 실시 ◈
 일     자:   20250918
 시     간:   18:43:45

==========================
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1802353800', '계좌:[9002207423734   ]
----------------------------
[카드입금]
  ◈ OFF-LINE 명세표 ◈

20250918007064150085카입금

 09004  9004-03-000061-7

    *10,000*     *150*
                    **


022     18:01:09  00034

      출금수표: 00매
      입금수표: 00매
ATEC AP 


     ◈ 장애명세표 ◈
 일     자:  20250918
 시     간:  18:02:33
 일련  번호:      0085
 장애 STEP:    [INSERVICE]
 모듈[33] 코드[99] 상세코드[00A1]
 내용:현금부 동작 이상

==========================
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1759463770', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 정상 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1759363740', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 정상 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1759343730', '
==========================
        ◈ OFF - LINE ◈
        ◈ 개국명세표 ◈
HOST        :   172.25.221.64
PORT        :            8200
LOCAL       :  172.27.128.242
GATE        :    172.27.128.1
NETMASK     :   255.255.255.0
금고번호(점번) :    00706
기        번 :      415
일 련   번 호 :     0084
개 국   일 자 : 20250918
개 국   시 간 : 17:59:32
HOST MS차단  : 미차단
장애인　  모드 : 실  행(정상)
근접센서  모드 : 사용
R F 버전     : HS-N-08 
미수취함 설정: 수표회수함
TOTA:GCEK-200916- G3
BVTO:KRW -190201-  0
MAIN:GAAJ-200909-G2
TSK :GACA-180905-01
CAM :GADA-200710-02
MBV :GAUL-201008-0E
RJB :GAFD-160513-01
RJP :GAGD-161220-01
CSTA:GAHD-190729-01
CSTB:GAHD-190729-01
CSTC:GAHD-190729-01
CSTD:GAJF-190514-N0
SCST:GAID-190729-01
ML  :GALA-190705-03
MU  :GAMA-190705-03
MS  :GANA-190705-01
SBV :GAUM-200930-01');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1759033710', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 정상 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1759023700', '
   ◈ 장애 복구 완료 ◈
 일     자:   20250918
 시     간:   17:59:01
    장애가 정상 복구되었습니다
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1758263690', '
 ◈ 수동 장애복구 실시 ◈
 일     자:   20250918
 시     간:   17:58:24

==========================
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1758213680', '상단 Tray - 장착');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1758203670', '상단 Tray - 탈착');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1752023660', '계좌:[9002207423734   ]
----------------------------
[카드입금]
  ◈ OFF-LINE 명세표 ◈

20250918007064150083카입금

 09004  9004-03-000061-7

    *10,000*     *150*
                    **


022     17:50:35  00034

      출금수표: 00매
      입금수표: 00매
ATEC AP 


     ◈ 장애명세표 ◈
 일     자:  20250918
 시     간:  17:52:01
 일련  번호:      0083
 장애 STEP:    [INSERVICE]
 모듈[33] 코드[99] 상세코드[00A1]
 내용:현금부 동작 이상

==========================
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1749253630', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 정상 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1749163600', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 정상 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1749153590', '
==========================
        ◈ OFF - LINE ◈
        ◈ 개국명세표 ◈
HOST        :   172.25.221.64
PORT        :            8200
LOCAL       :  172.27.128.242
GATE        :    172.27.128.1
NETMASK     :   255.255.255.0
금고번호(점번) :    00706
기        번 :      415
일 련   번 호 :     0082
개 국   일 자 : 20250918
개 국   시 간 : 17:49:12
HOST MS차단  : 미차단
장애인　  모드 : 실  행(정상)
근접센서  모드 : 사용
R F 버전     : HS-N-08 
미수취함 설정: 수표회수함
TOTA:GCEK-200916- G3
BVTO:KRW -190201-  0
MAIN:GAAJ-200909-G2
TSK :GACA-180905-01
CAM :GADA-200710-02
MBV :GAUL-201008-0E
RJB :GAFD-160513-01
RJP :GAGD-161220-01
CSTA:GAHD-190729-01
CSTB:GAHD-190729-01
CSTC:GAHD-190729-01
CSTD:GAJF-190514-N0
SCST:GAID-190729-01
ML  :GALA-190705-03
MU  :GAMA-190705-03
MS  :GANA-190705-01
SBV :GAUM-200930-01');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1748493570', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 정상 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1748483560', '
   ◈ 장애 복구 완료 ◈
 일     자:   20250918
 시     간:   17:48:47
    장애가 정상 복구되었습니다
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1748113550', '
 ◈ 수동 장애복구 실시 ◈
 일     자:   20250918
 시     간:   17:48:09

==========================
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1748073540', '상단 Tray - 장착');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250918', '1746103520', '계좌:[9002207423734   ]
----------------------------
[카드입금]
  ◈ OFF-LINE 명세표 ◈

20250918007064150081카입금

 09004  9004-03-000061-7

    *10,000*     *150*
                    **


022     17:44:44  00034

      출금수표: 00매
      입금수표: 00매
ATEC AP 


     ◈ 장애명세표 ◈
 일     자:  20250918
 시     간:  17:46:09
 일련  번호:      0081
 장애 STEP:    [INSERVICE]
 모듈[33] 코드[99] 상세코드[00A1]
 내용:현금부 동작 이상

==========================
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250917', '1444592040', '        ◈ OFF - LINE ◈
        ◈ 개국명세표 ◈
HOST        :   172.25.221.64
PORT        :            8200
LOCAL       :  172.27.128.242
GATE        :    172.27.128.1
NETMASK     :   255.255.255.0
금고번호(점번) :    00706
기        번 :      415
일 련   번 호 :     0035
개 국   일 자 : 20250917
개 국   시 간 : 14:44:57
HOST MS차단  : 미차단
장애인　  모드 : 실  행(정상)
근접센서  모드 : 사용
R F 버전     : HS-N-08 
미수취함 설정: 수표회수함
TOTA:GCEK-200916- G3
BVTO:KRW -190201-  0
MAIN:GAAJ-200909-G2
TSK :GACA-180905-01
CAM :GADA-200710-02
MBV :GAUL-201008-0E
RJB :GAFD-160513-01
RJP :GAGD-161220-01
CSTA:GAHD-190729-01
CSTB:GAHD-190729-01
CSTC:GAHD-190729-01
CSTD:GAJF-190514-N0
SCST:GAID-190729-01
ML  :GALA-190705-03
MU  :GAMA-190705-03
MS  :GANA-190705-01
SBV :GAUM-200930-01');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250917', '1650382540', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 정상 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250917', '1906162560', '▤▤▤▤ 전원  차단 ▤▤▤▤
일 자 : 20250917
시 간 : 19:06:15
사 유 : 계원화면 전원버튼 종료
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250917', '1444442030', '▤▤▤▤ 전원  차단 ▤▤▤▤
일 자 : 20250917
시 간 : 14:31:07
사 유 : 기기 설정값(BBM) 변경

==========================
   ◈ POWER OFF/ON ◈
0706 415 0033 
일      자 : 20250917
시      간 : 14:33:01
UPDATE    : 25.09.05
AP VERSION: V250825_O
SD R/M:13,066MB
        ◈ OFF - LINE ◈
        ◈ 개국명세표 ◈
HOST        :   172.25.221.64
PORT        :            8200
LOCAL       :  172.27.128.242
GATE        :    172.27.128.1
NETMASK     :   255.255.255.0
금고번호(점번) :    00706
기        번 :      415
일 련   번 호 :     0033
개 국   일 자 : 20250917
개 국   시 간 : 14:33:21
HOST MS차단  : 미차단
장애인　  모드 : 실  행(정상)
근접센서  모드 : 사용
R F 버전     : HS-N-08 
미수취함 설정: 수표회수함
TOTA:GCEK-200916- G3
BVTO:KRW -190201-  0
MAIN:GAAJ-200909-G2
TSK :GACA-180905-01
CAM :GADA-200710-02
MBV :GAUL-201008-0E
RJB :GAFD-160513-01
RJP :GAGD-161220-01
CSTA:GAHD-190729-01
CSTB:GAHD-190729-01
CSTC:GAHD-190729-01
CSTD:GAJF-190514-N0
SCST:GAID-190729-01
ML  :GALA-190705-03
MU  :GAMA-190705-03
MS  :GANA-190705-01
SBV :GAUM-200930-01
    ◈ 현금 보충 명세표 ◈
점번:0706 기번:415 일련번호:0034
일자:20250917 
시간:14:34:15
보충된 오만원 :     1매
보충된　 만권 :     0매
보충된　 천권 :     0매
보충 REJECT :     0매
잔량매수 만권 :   150매
잔량매수5만권 :    25매
잔량 수표번호 :00000000
잔량 수표매수 :   0매
        ◈ OFF - LINE ◈
        ◈ 개국명세표 ◈
HOST        :   172.25.221.64
PORT        :            8200
LOCAL       :  172.27.128.242
GATE        :    172.27.128.1
NETMASK     :   255.255.255.0
금고번호(점번) :    00706
기        번 :      415
일 련   번 호 :     0034
개 국   일 자 : 20250917
개 국   시 간 : 14:40:38
HOST MS차단  : 미차단
장애인　  모드 : 실  행(정상)
근접센서  모드 : 사용
R F 버전     : HS-N-08 
미수취함 설정: 수표회수함
TOTA:GCEK-200916- G3
BVTO:KRW -190201-  0
MAIN:GAAJ-200909-G2
TSK :GACA-180905-01
CAM :GADA-200710-02
MBV :GAUL-201008-0E
RJB :GAFD-160513-01
RJP :GAGD-161220-01
CSTA:GAHD-190729-01
CSTB:GAHD-190729-01
CSTC:GAHD-190729-01
CSTD:GAJF-190514-N0
SCST:GAID-190729-01
ML  :GALA-190705-03
MU  :GAMA-190705-03
MS  :GANA-190705-01
SBV :GAUM-200930-01
==========================
   ◈ POWER OFF/ON ◈
0706 415 0034 
일      자 : 20250917
시      간 : 14:44:43
UPDATE    : 25.08.08
AP VERSION: V999999_O
SD R/M:13,066MB
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250917', '1445002050', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 부족예보 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250917', '1452282070', '계좌:[9002207423734   ]
----------------------------
[카드입금]
  ◈ OFF-LINE 명세표 ◈

20250917007064150036카입금

 09004  9004-03-000061-7

    *10,000*     *150*
                    **


022     14:52:10  00034

      출금수표: 00매
      입금수표: 00매
ATEC AP 

');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250917', '1455162080', '계좌:[9002207423734   ]
----------------------------
[카드입금]
  ◈ OFF-LINE 명세표 ◈

20250917007064150037카입금

 09004  9004-03-000061-7

    *10,000*     *150*
                    **


022     14:54:59  00034

      출금수표: 00매
      입금수표: 00매
ATEC AP 

');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250917', '1459322100', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 부족예보 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250917', '1531202130', '        ◈ OFF - LINE ◈
        ◈ 개국명세표 ◈
HOST        :   172.25.221.64
PORT        :            8200
LOCAL       :  172.27.128.242
GATE        :    172.27.128.1
NETMASK     :   255.255.255.0
금고번호(점번) :    00706
기        번 :      415
일 련   번 호 :     0038
개 국   일 자 : 20250917
개 국   시 간 : 15:31:18
HOST MS차단  : 미차단
장애인　  모드 : 실  행(정상)
근접센서  모드 : 사용
R F 버전     : HS-N-08 
미수취함 설정: 수표회수함
TOTA:GCEK-200916- G3
BVTO:KRW -190201-  0
MAIN:GAAJ-200909-G2
TSK :GACA-180905-01
CAM :GADA-200710-02
MBV :GAUL-201008-0E
RJB :GAFD-160513-01
RJP :GAGD-161220-01
CSTA:GAHD-190729-01
CSTB:GAHD-190729-01
CSTC:GAHD-190729-01
CSTD:GAJF-190514-N0
SCST:GAID-190729-01
ML  :GALA-190705-03
MU  :GAMA-190705-03
MS  :GANA-190705-01
SBV :GAUM-200930-01');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250917', '1531222140', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 부족예보 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250917', '1542482160', '▤▤▤▤ 전원  차단 ▤▤▤▤
일 자 : 20250917
시 간 : 15:42:47
사 유 : 비정상 종료
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250917', '1549182180', '
==========================
   ◈ POWER OFF/ON ◈
0706 415 0038 
일      자 : 20250917
시      간 : 15:49:18
UPDATE    : 25.08.08
AP VERSION: V999999_O
SD R/M:13,066MB
PON중 장애 발생함 
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250917', '1549452200', '      ◈ 비정상 P-OFF ◈
706 415 0038 
일     자:            20250917
시     간:            15:49:41
계좌 번호:         90022074237
금     액:                *0원
[카드출금]중 전원 OFF

     ◈ 장애명세표 ◈
 일     자:  20250917
 시     간:  15:49:44
 일련  번호:      0038
 장애 STEP:    [POWERUP]
 모듈[09] 코드[31] 상세코드[0117]
 내용:비정상 POWER OFF

==========================
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250917', '1549502210', '
   ◈ 장애 복구 완료 ◈
 일     자:   20250917
 시     간:   15:49:50
    장애가 정상 복구되었습니다
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250917', '1549512220', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 부족예보 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250917', '1556052240', '▤▤▤▤ 전원  차단 ▤▤▤▤
일 자 : 20250917
시 간 : 15:56:05
사 유 : 기기 설정값(BBM) 변경
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250917', '1600572260', '
==========================
   ◈ POWER OFF/ON ◈
0706 415 0060 
일      자 : 20250917
시      간 : 16:00:56
UPDATE    : 25.08.08
AP VERSION: V999999_O
SD R/M:13,066MB
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250917', '1601222280', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 부족예보 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250917', '1601202270', '        ◈ OFF - LINE ◈
        ◈ 개국명세표 ◈
HOST        :   172.25.221.64
PORT        :            8200
LOCAL       :  172.27.128.242
GATE        :    172.27.128.1
NETMASK     :   255.255.255.0
금고번호(점번) :    00706
기        번 :      415
일 련   번 호 :     0061
개 국   일 자 : 20250917
개 국   시 간 : 16:01:18
HOST MS차단  : 미차단
장애인　  모드 : 실  행(정상)
근접센서  모드 : 사용
R F 버전     : HS-N-08 
미수취함 설정: 수표회수함
TOTA:GCEK-200916- G3
BVTO:KRW -190201-  0
MAIN:GAAJ-200909-G2
TSK :GACA-180905-01
CAM :GADA-200710-02
MBV :GAUL-201008-0E
RJB :GAFD-160513-01
RJP :GAGD-161220-01
CSTA:GAHD-190729-01
CSTB:GAHD-190729-01
CSTC:GAHD-190729-01
CSTD:GAJF-190514-N0
SCST:GAID-190729-01
ML  :GALA-190705-03
MU  :GAMA-190705-03
MS  :GANA-190705-01
SBV :GAUM-200930-01');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250917', '1650362530', '
   ◈ 장애 복구 완료 ◈
 일     자:   20250917
 시     간:   16:50:35
    장애가 정상 복구되었습니다
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250917', '1650002520', '
 ◈ 수동 장애복구 실시 ◈
 일     자:   20250917
 시     간:   16:49:59

==========================
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250917', '1635102510', '계좌:[9002207423734   ]
----------------------------
[카드입금]
  ◈ OFF-LINE 명세표 ◈

20250917007064150065카입금

 09004  9004-03-000061-7

    *10,000*     *150*
                    **


022     16:34:07  00034

      출금수표: 00매
      입금수표: 00매
ATEC AP 


     ◈ 장애명세표 ◈
 일     자:  20250917
 시     간:  16:35:09
 일련  번호:      0065
 장애 STEP:    [INSERVICE]
 모듈[33] 코드[99] 상세코드[00A1]
 내용:현금부 동작 이상

==========================
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250917', '1632592480', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 정상 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250917', '1632522470', '
    ◈ 현금 보충 명세표 ◈
점번:0706 기번:415 일련번호:0064
일자:20250917 
시간:16:32:45
보충된 오만원 :    26매
보충된　 만권 :   148매
보충된　 천권 :     0매
보충 REJECT :     0매
잔량매수 만권 :   200매
잔량매수5만권 :    31매
잔량 수표번호 :00000000
잔량 수표매수 :   0매
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250917', '1632022450', '보회함 문 - 닫힘');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250917', '1631572440', '보회함 문 - 열림');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250917', '1608442420', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 부족 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250917', '1608072400', '계좌:[9002207423734   ]
----------------------------
[카드출금]
  ◈ OFF-LINE 명세표 ◈

20250917007064150063카출금

 09004  9004-03-000061-7

    *1,000,000*     *150*
                    **


022     16:07:30  00034

      출금수표: 00매
      입금수표: 00매
ATEC AP 

');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250917', '1605282380', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 부족예보 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250917', '1603382360', '계좌:[9002207423734   ]
----------------------------
[카드출금]
  ◈ OFF-LINE 명세표 ◈

20250917007064150062카출금

 09004  9004-03-000061-7

    *1,000,000*     *150*
                    **


022     16:02:49  00034

      출금수표: 00매
      입금수표: 00매
ATEC AP 

');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250917', '1602212340', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 부족예보 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('00706', '415', '20250917', '1601572310', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 부족예보 ◈
◈ 수표상태 : 부족 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('05113', '402', '20250821', '1832326770', '
     ◈ 장애명세표 ◈
 일     자:  20250821
 시     간:  18:32:30
 일련  번호:      0013
 장애 STEP:    [INSERVICE]
 모듈[33] 코드[05] 상세코드[1201]
 내용:현금부 모듈 세트 불량

==========================
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('05113', '402', '20250821', '1832486790', '상단 Tray - 탈착');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('05113', '402', '20250821', '1834416800', '상단 Tray - 장착');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('05113', '402', '20250821', '1834486810', '
 ◈ 수동 장애복구 실시 ◈
 일     자:   20250821
 시     간:   18:34:46

==========================
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('05113', '402', '20250821', '1835516820', '
   ◈ 장애 복구 완료 ◈
 일     자:   20250821
 시     간:   18:35:50
    장애가 정상 복구되었습니다
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('05113', '402', '20250821', '1835546830', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 정상 ◈
◈ 수표상태 : 정상 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('05113', '402', '20250821', '1836076850', '
==========================
        ◈ OFF - LINE ◈
        ◈ 개국명세표 ◈
HOST        :   172.25.221.64
PORT        :            8200
LOCAL       :  172.27.128.242
GATE        :    172.27.128.1
NETMASK     :   255.255.255.0
금고번호(점번) :    05113
기        번 :      402
일 련   번 호 :     0014
개 국   일 자 : 20250821
개 국   시 간 : 18:36:05
HOST MS차단  : 미차단
장애인　  모드 : 실  행(정상)
근접센서  모드 : 사용
R F 버전     : HS-N-08 
미수취함 설정: 수표회수함
TOTA:GCEK-200916- G3
BVTO:KRW -190201-  0
MAIN:GAAJ-200909-G2
TSK :GACA-180905-01
CAM :GADA-200710-02
MBV :GAUL-201008-0E
RJB :GAFD-160513-01
RJP :GAGD-161220-01
CSTA:GAHD-190729-01
CSTB:GAHD-190729-01
CSTC:GAHD-190729-01
CSTD:GAJF-190514-N0
SCST:GAID-190729-01
ML  :GALA-190705-03
MU  :GAMA-190705-03
MS  :GANA-190705-01
SBV :GAUM-200930-01');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('05113', '402', '20250821', '1836096860', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 정상 ◈
◈ 수표상태 : 정상 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('05113', '402', '20250821', '1837046880', '계좌:[9002207423734   ]
----------------------------
[카드출금]
  ◈ OFF-LINE 명세표 ◈

20250821051134020015카출금

 09004  9004-03-000061-7

    *10,000*     *150*
                    **


022     18:36:45  00034

      출금수표: 00매
      입금수표: 00매
ATEC AP 

');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('05113', '402', '20250821', '1837226900', '  ◈ OFF-LINE 명세표 ◈

20250821051134020015카출금




          현금출금 취소
    2025-08-21   18:36


취소거래T.O잔액조회요망
장애CODE : 33-05-1201
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('05113', '402', '20250821', '1837316910', '
     ◈ 장애명세표 ◈
 일     자:  20250821
 시     간:  18:37:30
 일련  번호:      0015
 장애 STEP:    [INSERVICE]
 모듈[33] 코드[05] 상세코드[1201]
 내용:현금부 모듈 세트 불량

==========================
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('05113', '402', '20250821', '1837476930', '상단 Tray - 탈착');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('05113', '402', '20250821', '1838446940', '상단 Tray - 장착');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('05113', '402', '20250821', '1838556950', '
 ◈ 수동 장애복구 실시 ◈
 일     자:   20250821
 시     간:   18:38:54

==========================
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('05113', '402', '20250821', '1839586960', '
   ◈ 장애 복구 완료 ◈
 일     자:   20250821
 시     간:   18:39:57
    장애가 정상 복구되었습니다
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('05113', '402', '20250821', '1840016970', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 정상 ◈
◈ 수표상태 : 정상 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('05113', '402', '20250821', '1907316990', '▤▤▤▤ 전원  차단 ▤▤▤▤
일 자 : 20250821
시 간 : 19:07:31
사 유 : 계원화면 전원버튼 종료
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('05113', '402', '20250821', '1759506610', '상단 Tray - 장착');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('05113', '402', '20250821', '1705326490', '▤▤▤▤ 전원  차단 ▤▤▤▤
일 자 : 20250821
시 간 : 17:05:31
사 유 : 기기 설정값(BBM) 변경
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('05113', '402', '20250821', '1707046510', '
==========================
   ◈ POWER OFF/ON ◈
5113 402 0009 
일      자 : 20250821
시      간 : 17:07:03
UPDATE    : 25.08.08
AP VERSION: V999999_O
SD R/M:13,172MB
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('05113', '402', '20250821', '1707236520', '        ◈ OFF - LINE ◈
        ◈ 개국명세표 ◈
HOST        :   172.25.221.64
PORT        :            8200
LOCAL       :  172.27.128.242
GATE        :    172.27.128.1
NETMASK     :   255.255.255.0
금고번호(점번) :    05113
기        번 :      402
일 련   번 호 :     0010
개 국   일 자 : 20250821
개 국   시 간 : 17:07:20
HOST MS차단  : 미차단
장애인　  모드 : 실  행(정상)
근접센서  모드 : 사용
R F 버전     : HS-N-08 
미수취함 설정: 수표회수함
TOTA:GCEK-200916- G3
BVTO:KRW -190201-  0
MAIN:GAAJ-200909-G2
TSK :GACA-180905-01
CAM :GADA-200710-02
MBV :GAUL-201008-0E
RJB :GAFD-160513-01
RJP :GAGD-161220-01
CSTA:GAHD-190729-01
CSTB:GAHD-190729-01
CSTC:GAHD-190729-01
CSTD:GAJF-190514-N0
SCST:GAID-190729-01
ML  :GALA-190705-03
MU  :GAMA-190705-03
MS  :GANA-190705-01
SBV :GAUM-200930-01');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('05113', '402', '20250821', '1707246530', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 정상 ◈
◈ 수표상태 : 정상 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('05113', '402', '20250821', '1709386550', '계좌:[9002207423734   ]
----------------------------
[카드출금]
  ◈ OFF-LINE 명세표 ◈

20250821051134020011카출금

 09004  9004-03-000061-7

    *10,000*     *150*
                    **


022     17:09:18  00034

      출금수표: 00매
      입금수표: 00매
ATEC AP 

');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('05113', '402', '20250821', '1709576570', '  ◈ OFF-LINE 명세표 ◈

20250821051134020011카출금




          현금출금 취소
    2025-08-21   17:09


취소거래T.O잔액조회요망
장애CODE : 33-05-1201
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('05113', '402', '20250821', '1710066580', '
     ◈ 장애명세표 ◈
 일     자:  20250821
 시     간:  17:10:04
 일련  번호:      0011
 장애 STEP:    [INSERVICE]
 모듈[33] 코드[05] 상세코드[1201]
 내용:현금부 모듈 세트 불량

==========================
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('05113', '402', '20250821', '1710226600', '상단 Tray - 탈착');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('05113', '402', '20250821', '1832226760', '  ◈ OFF-LINE 명세표 ◈

20250821051134020013카출금




          현금출금 취소
    2025-08-21   18:31


취소거래T.O잔액조회요망
장애CODE : 33-05-1201
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('05113', '402', '20250821', '1800036620', '
 ◈ 수동 장애복구 실시 ◈
 일     자:   20250821
 시     간:   18:00:01

==========================
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('05113', '402', '20250821', '1801066630', '
   ◈ 장애 복구 완료 ◈
 일     자:   20250821
 시     간:   18:01:05
    장애가 정상 복구되었습니다
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('05113', '402', '20250821', '1801076640', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 정상 ◈
◈ 수표상태 : 정상 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('05113', '402', '20250821', '1805016670', '
==========================
   ◈ POWER OFF/ON ◈
5113 402 0011 
일      자 : 20250821
시      간 : 18:05:01
UPDATE    : 25.08.08
AP VERSION: V999999_O
SD R/M:13,172MB
');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('05113', '402', '20250821', '1805186680', '        ◈ OFF - LINE ◈
        ◈ 개국명세표 ◈
HOST        :   172.25.221.64
PORT        :            8200
LOCAL       :  172.27.128.242
GATE        :    172.27.128.1
NETMASK     :   255.255.255.0
금고번호(점번) :    05113
기        번 :      402
일 련   번 호 :     0012
개 국   일 자 : 20250821
개 국   시 간 : 18:05:16
HOST MS차단  : 미차단
장애인　  모드 : 실  행(정상)
근접센서  모드 : 사용
R F 버전     : HS-N-08 
미수취함 설정: 수표회수함
TOTA:GCEK-200916- G3
BVTO:KRW -190201-  0
MAIN:GAAJ-200909-G2
TSK :GACA-180905-01
CAM :GADA-200710-02
MBV :GAUL-201008-0E
RJB :GAFD-160513-01
RJP :GAGD-161220-01
CSTA:GAHD-190729-01
CSTB:GAHD-190729-01
CSTC:GAHD-190729-01
CSTD:GAJF-190514-N0
SCST:GAID-190729-01
ML  :GALA-190705-03
MU  :GAMA-190705-03
MS  :GANA-190705-01
SBV :GAUM-200930-01');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('05113', '402', '20250821', '1805206690', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 정상 ◈
◈ 수표상태 : 정상 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('05113', '402', '20250821', '1808256720', '◈ 저널상태 : 정상 ◈
◈ 명세표상태 : 정상 ◈
◈ 현금상태 : 정상 ◈
◈ 수표상태 : 정상 ◈
  ');
INSERT INTO T_JOURNAL_DATA
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, JNL_DATA)
VALUES('05113', '402', '20250821', '1832046740', '계좌:[9002207423734   ]
----------------------------
[카드출금]
  ◈ OFF-LINE 명세표 ◈

20250821051134020013카출금

 09004  9004-03-000061-7

    *10,000*     *150*
                    **


022     18:31:48  00034

      출금수표: 00매
      입금수표: 00매
ATEC AP 

');
-- ---- T_JOURNAL_IMAGE ----
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00107', '404', '20250924', '1020000140', '1', '/atms/share/ejnl/img/20250924/00107.404.1020000140.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00107', '404', '20250924', '1020000140', '3', '/atms/share/ejnl/img/20250924/00107.404.1020000140.03.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00107', '404', '20250924', '1626050240', '1', '/atms/share/ejnl/img/20250924/00107.404.1626050240.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00107', '404', '20250924', '1626050240', '2', '/atms/share/ejnl/img/20250924/00107.404.1626050240.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00107', '404', '20250924', '1626050240', '3', '/atms/share/ejnl/img/20250924/00107.404.1626050240.03.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250922', '0901065340', '3', '/atms/share/ejnl/img/20250922/00706.415.0901065340.03.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250922', '0901065340', '2', '/atms/share/ejnl/img/20250922/00706.415.0901065340.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250922', '0901065340', '1', '/atms/share/ejnl/img/20250922/00706.415.0901065340.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1801584750', '2', '/atms/share/ejnl/img/20250920/00706.415.1801584750.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1632284660', '2', '/atms/share/ejnl/img/20250920/00706.415.1632284660.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1632554690', '1', '/atms/share/ejnl/img/20250920/00706.415.1632554690.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1632554690', '2', '/atms/share/ejnl/img/20250920/00706.415.1632554690.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1801584750', '1', '/atms/share/ejnl/img/20250920/00706.415.1801584750.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1837204910', '2', '/atms/share/ejnl/img/20250920/00706.415.1837204910.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1837204910', '1', '/atms/share/ejnl/img/20250920/00706.415.1837204910.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1836574890', '3', '/atms/share/ejnl/img/20250920/00706.415.1836574890.03.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1836574890', '2', '/atms/share/ejnl/img/20250920/00706.415.1836574890.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1836574890', '1', '/atms/share/ejnl/img/20250920/00706.415.1836574890.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1802504830', '2', '/atms/share/ejnl/img/20250920/00706.415.1802504830.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1632284650', '1', '/atms/share/ejnl/img/20250920/00706.415.1632284650.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1801584750', '3', '/atms/share/ejnl/img/20250920/00706.415.1801584750.03.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1802214770', '1', '/atms/share/ejnl/img/20250920/00706.415.1802214770.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1802214770', '2', '/atms/share/ejnl/img/20250920/00706.415.1802214770.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1802224790', '1', '/atms/share/ejnl/img/20250920/00706.415.1802224790.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1802224790', '2', '/atms/share/ejnl/img/20250920/00706.415.1802224790.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1802224800', '1', '/atms/share/ejnl/img/20250920/00706.415.1802224800.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1802224800', '2', '/atms/share/ejnl/img/20250920/00706.415.1802224800.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1619454530', '1', '/atms/share/ejnl/img/20250920/00706.415.1619454530.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1837484970', '1', '/atms/share/ejnl/img/20250920/00706.415.1837484970.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1837214940', '2', '/atms/share/ejnl/img/20250920/00706.415.1837214940.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1837214940', '1', '/atms/share/ejnl/img/20250920/00706.415.1837214940.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1837214930', '2', '/atms/share/ejnl/img/20250920/00706.415.1837214930.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1837214930', '1', '/atms/share/ejnl/img/20250920/00706.415.1837214930.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1619134460', '1', '/atms/share/ejnl/img/20250920/00706.415.1619134460.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1619134460', '3', '/atms/share/ejnl/img/20250920/00706.415.1619134460.03.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1619364480', '1', '/atms/share/ejnl/img/20250920/00706.415.1619364480.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1619454520', '1', '/atms/share/ejnl/img/20250920/00706.415.1619454520.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1632284660', '1', '/atms/share/ejnl/img/20250920/00706.415.1632284660.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1620054550', '1', '/atms/share/ejnl/img/20250920/00706.415.1620054550.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1632044610', '1', '/atms/share/ejnl/img/20250920/00706.415.1632044610.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1632044610', '2', '/atms/share/ejnl/img/20250920/00706.415.1632044610.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1632044610', '3', '/atms/share/ejnl/img/20250920/00706.415.1632044610.03.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1632264630', '1', '/atms/share/ejnl/img/20250920/00706.415.1632264630.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1632264630', '2', '/atms/share/ejnl/img/20250920/00706.415.1632264630.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1802504830', '1', '/atms/share/ejnl/img/20250920/00706.415.1802504830.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1632284650', '2', '/atms/share/ejnl/img/20250920/00706.415.1632284650.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1837484970', '2', '/atms/share/ejnl/img/20250920/00706.415.1837484970.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1917125220', '1', '/atms/share/ejnl/img/20250920/00706.415.1917125220.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1917125220', '2', '/atms/share/ejnl/img/20250920/00706.415.1917125220.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1917395250', '1', '/atms/share/ejnl/img/20250920/00706.415.1917395250.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1917395250', '2', '/atms/share/ejnl/img/20250920/00706.415.1917395250.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1917115210', '2', '/atms/share/ejnl/img/20250920/00706.415.1917115210.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1917115210', '1', '/atms/share/ejnl/img/20250920/00706.415.1917115210.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1917105190', '2', '/atms/share/ejnl/img/20250920/00706.415.1917105190.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1917105190', '1', '/atms/share/ejnl/img/20250920/00706.415.1917105190.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1916475170', '3', '/atms/share/ejnl/img/20250920/00706.415.1916475170.03.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1916475170', '2', '/atms/share/ejnl/img/20250920/00706.415.1916475170.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1916475170', '1', '/atms/share/ejnl/img/20250920/00706.415.1916475170.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1859195110', '1', '/atms/share/ejnl/img/20250920/00706.415.1859195110.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1858285030', '1', '/atms/share/ejnl/img/20250920/00706.415.1858285030.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1858285030', '2', '/atms/share/ejnl/img/20250920/00706.415.1858285030.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1858285030', '3', '/atms/share/ejnl/img/20250920/00706.415.1858285030.03.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1858515050', '1', '/atms/share/ejnl/img/20250920/00706.415.1858515050.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1858515050', '2', '/atms/share/ejnl/img/20250920/00706.415.1858515050.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1858525070', '1', '/atms/share/ejnl/img/20250920/00706.415.1858525070.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1858525070', '2', '/atms/share/ejnl/img/20250920/00706.415.1858525070.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1858525080', '1', '/atms/share/ejnl/img/20250920/00706.415.1858525080.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1858525080', '2', '/atms/share/ejnl/img/20250920/00706.415.1858525080.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250920', '1859195110', '2', '/atms/share/ejnl/img/20250920/00706.415.1859195110.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250919', '0851434240', '1', '/atms/share/ejnl/img/20250919/00706.415.0851434240.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250919', '0851434240', '2', '/atms/share/ejnl/img/20250919/00706.415.0851434240.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250919', '0851434240', '3', '/atms/share/ejnl/img/20250919/00706.415.0851434240.03.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1736503290', '2', '/atms/share/ejnl/img/20250918/00706.415.1736503290.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1751163650', '2', '/atms/share/ejnl/img/20250918/00706.415.1751163650.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1736503290', '1', '/atms/share/ejnl/img/20250918/00706.415.1736503290.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1736473280', '2', '/atms/share/ejnl/img/20250918/00706.415.1736473280.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1736473280', '1', '/atms/share/ejnl/img/20250918/00706.415.1736473280.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1736133270', '2', '/atms/share/ejnl/img/20250918/00706.415.1736133270.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1736133270', '1', '/atms/share/ejnl/img/20250918/00706.415.1736133270.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1625363250', '3', '/atms/share/ejnl/img/20250918/00706.415.1625363250.03.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1625363250', '2', '/atms/share/ejnl/img/20250918/00706.415.1625363250.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1625363250', '1', '/atms/share/ejnl/img/20250918/00706.415.1625363250.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1624083190', '2', '/atms/share/ejnl/img/20250918/00706.415.1624083190.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1624083190', '1', '/atms/share/ejnl/img/20250918/00706.415.1624083190.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1623323180', '2', '/atms/share/ejnl/img/20250918/00706.415.1623323180.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1623323180', '1', '/atms/share/ejnl/img/20250918/00706.415.1623323180.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1623283170', '2', '/atms/share/ejnl/img/20250918/00706.415.1623283170.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1527132990', '2', '/atms/share/ejnl/img/20250918/00706.415.1527132990.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1526212910', '3', '/atms/share/ejnl/img/20250918/00706.415.1526212910.03.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1526452930', '1', '/atms/share/ejnl/img/20250918/00706.415.1526452930.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1526452930', '2', '/atms/share/ejnl/img/20250918/00706.415.1526452930.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1526462950', '1', '/atms/share/ejnl/img/20250918/00706.415.1526462950.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1526462950', '2', '/atms/share/ejnl/img/20250918/00706.415.1526462950.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1526462960', '1', '/atms/share/ejnl/img/20250918/00706.415.1526462960.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1526462960', '2', '/atms/share/ejnl/img/20250918/00706.415.1526462960.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1527132990', '1', '/atms/share/ejnl/img/20250918/00706.415.1527132990.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1526212910', '2', '/atms/share/ejnl/img/20250918/00706.415.1526212910.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1542523050', '1', '/atms/share/ejnl/img/20250918/00706.415.1542523050.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1542523050', '3', '/atms/share/ejnl/img/20250918/00706.415.1542523050.03.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1613543070', '1', '/atms/share/ejnl/img/20250918/00706.415.1613543070.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1614293080', '1', '/atms/share/ejnl/img/20250918/00706.415.1614293080.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1616113140', '1', '/atms/share/ejnl/img/20250918/00706.415.1616113140.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1616113140', '2', '/atms/share/ejnl/img/20250918/00706.415.1616113140.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1616113140', '3', '/atms/share/ejnl/img/20250918/00706.415.1616113140.03.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1623283170', '1', '/atms/share/ejnl/img/20250918/00706.415.1623283170.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1526212910', '1', '/atms/share/ejnl/img/20250918/00706.415.1526212910.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1513252850', '2', '/atms/share/ejnl/img/20250918/00706.415.1513252850.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1513252850', '1', '/atms/share/ejnl/img/20250918/00706.415.1513252850.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1512582820', '2', '/atms/share/ejnl/img/20250918/00706.415.1512582820.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1512582820', '1', '/atms/share/ejnl/img/20250918/00706.415.1512582820.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1512582810', '2', '/atms/share/ejnl/img/20250918/00706.415.1512582810.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1512582810', '1', '/atms/share/ejnl/img/20250918/00706.415.1512582810.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1512562790', '2', '/atms/share/ejnl/img/20250918/00706.415.1512562790.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1512562790', '1', '/atms/share/ejnl/img/20250918/00706.415.1512562790.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1512342770', '3', '/atms/share/ejnl/img/20250918/00706.415.1512342770.03.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1512342770', '2', '/atms/share/ejnl/img/20250918/00706.415.1512342770.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1512342770', '1', '/atms/share/ejnl/img/20250918/00706.415.1512342770.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1506522710', '1', '/atms/share/ejnl/img/20250918/00706.415.1506522710.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1623263160', '1', '/atms/share/ejnl/img/20250918/00706.415.1623263160.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1623263160', '2', '/atms/share/ejnl/img/20250918/00706.415.1623263160.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1801483790', '2', '/atms/share/ejnl/img/20250918/00706.415.1801483790.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1751163650', '3', '/atms/share/ejnl/img/20250918/00706.415.1751163650.03.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1758203670', '1', '/atms/share/ejnl/img/20250918/00706.415.1758203670.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1758203670', '2', '/atms/share/ejnl/img/20250918/00706.415.1758203670.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1758213680', '1', '/atms/share/ejnl/img/20250918/00706.415.1758213680.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1758213680', '2', '/atms/share/ejnl/img/20250918/00706.415.1758213680.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1758263690', '1', '/atms/share/ejnl/img/20250918/00706.415.1758263690.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1758263690', '2', '/atms/share/ejnl/img/20250918/00706.415.1758263690.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1759023700', '1', '/atms/share/ejnl/img/20250918/00706.415.1759023700.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1759023700', '2', '/atms/share/ejnl/img/20250918/00706.415.1759023700.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1801483790', '1', '/atms/share/ejnl/img/20250918/00706.415.1801483790.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1737263300', '2', '/atms/share/ejnl/img/20250918/00706.415.1737263300.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1801483790', '3', '/atms/share/ejnl/img/20250918/00706.415.1801483790.03.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1843473810', '1', '/atms/share/ejnl/img/20250918/00706.415.1843473810.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1843473810', '2', '/atms/share/ejnl/img/20250918/00706.415.1843473810.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1844223820', '1', '/atms/share/ejnl/img/20250918/00706.415.1844223820.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1844223820', '2', '/atms/share/ejnl/img/20250918/00706.415.1844223820.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1506322690', '1', '/atms/share/ejnl/img/20250918/00706.415.1506322690.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1506322680', '1', '/atms/share/ejnl/img/20250918/00706.415.1506322680.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1506232640', '1', '/atms/share/ejnl/img/20250918/00706.415.1506232640.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1506012620', '3', '/atms/share/ejnl/img/20250918/00706.415.1506012620.03.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1506012620', '1', '/atms/share/ejnl/img/20250918/00706.415.1506012620.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1751163650', '1', '/atms/share/ejnl/img/20250918/00706.415.1751163650.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1737263300', '1', '/atms/share/ejnl/img/20250918/00706.415.1737263300.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1739553390', '1', '/atms/share/ejnl/img/20250918/00706.415.1739553390.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1739553390', '2', '/atms/share/ejnl/img/20250918/00706.415.1739553390.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1739553390', '3', '/atms/share/ejnl/img/20250918/00706.415.1739553390.03.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1741443410', '1', '/atms/share/ejnl/img/20250918/00706.415.1741443410.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1741443410', '2', '/atms/share/ejnl/img/20250918/00706.415.1741443410.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1742203420', '1', '/atms/share/ejnl/img/20250918/00706.415.1742203420.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1742203420', '2', '/atms/share/ejnl/img/20250918/00706.415.1742203420.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1745253510', '1', '/atms/share/ejnl/img/20250918/00706.415.1745253510.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1745253510', '2', '/atms/share/ejnl/img/20250918/00706.415.1745253510.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1747593530', '1', '/atms/share/ejnl/img/20250918/00706.415.1747593530.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1747593530', '2', '/atms/share/ejnl/img/20250918/00706.415.1747593530.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1748073540', '1', '/atms/share/ejnl/img/20250918/00706.415.1748073540.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1748073540', '2', '/atms/share/ejnl/img/20250918/00706.415.1748073540.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1745253510', '3', '/atms/share/ejnl/img/20250918/00706.415.1745253510.03.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1748113550', '1', '/atms/share/ejnl/img/20250918/00706.415.1748113550.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1748113550', '2', '/atms/share/ejnl/img/20250918/00706.415.1748113550.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1748483560', '1', '/atms/share/ejnl/img/20250918/00706.415.1748483560.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250918', '1748483560', '2', '/atms/share/ejnl/img/20250918/00706.415.1748483560.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250917', '1634242500', '1', '/atms/share/ejnl/img/20250917/00706.415.1634242500.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250917', '1650002520', '2', '/atms/share/ejnl/img/20250917/00706.415.1650002520.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250917', '1608072400', '3', '/atms/share/ejnl/img/20250917/00706.415.1608072400.03.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250917', '1608072400', '2', '/atms/share/ejnl/img/20250917/00706.415.1608072400.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250917', '1608072400', '1', '/atms/share/ejnl/img/20250917/00706.415.1608072400.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250917', '1603382360', '3', '/atms/share/ejnl/img/20250917/00706.415.1603382360.03.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250917', '1603382360', '2', '/atms/share/ejnl/img/20250917/00706.415.1603382360.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250917', '1603382360', '1', '/atms/share/ejnl/img/20250917/00706.415.1603382360.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250917', '1455162080', '3', '/atms/share/ejnl/img/20250917/00706.415.1455162080.03.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250917', '1455162080', '2', '/atms/share/ejnl/img/20250917/00706.415.1455162080.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250917', '1455162080', '1', '/atms/share/ejnl/img/20250917/00706.415.1455162080.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250917', '1452282070', '3', '/atms/share/ejnl/img/20250917/00706.415.1452282070.03.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250917', '1452282070', '2', '/atms/share/ejnl/img/20250917/00706.415.1452282070.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250917', '1452282070', '1', '/atms/share/ejnl/img/20250917/00706.415.1452282070.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250917', '1634242500', '2', '/atms/share/ejnl/img/20250917/00706.415.1634242500.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250917', '1634242500', '3', '/atms/share/ejnl/img/20250917/00706.415.1634242500.03.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250917', '1650002520', '1', '/atms/share/ejnl/img/20250917/00706.415.1650002520.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250917', '1650362530', '1', '/atms/share/ejnl/img/20250917/00706.415.1650362530.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('00706', '415', '20250917', '1650362530', '2', '/atms/share/ejnl/img/20250917/00706.415.1650362530.02.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('05113', '402', '20250821', '1839586960', '1', '/atms/share/ejnl/img/20250821/05113.402.1839586960.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('05113', '402', '20250821', '1709386550', '1', '/atms/share/ejnl/img/20250821/05113.402.1709386550.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('05113', '402', '20250821', '1709386550', '3', '/atms/share/ejnl/img/20250821/05113.402.1709386550.03.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('05113', '402', '20250821', '1709396560', '1', '/atms/share/ejnl/img/20250821/05113.402.1709396560.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('05113', '402', '20250821', '1710066580', '1', '/atms/share/ejnl/img/20250821/05113.402.1710066580.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('05113', '402', '20250821', '1710226600', '1', '/atms/share/ejnl/img/20250821/05113.402.1710226600.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('05113', '402', '20250821', '1759506610', '1', '/atms/share/ejnl/img/20250821/05113.402.1759506610.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('05113', '402', '20250821', '1800036620', '1', '/atms/share/ejnl/img/20250821/05113.402.1800036620.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('05113', '402', '20250821', '1801066630', '1', '/atms/share/ejnl/img/20250821/05113.402.1801066630.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('05113', '402', '20250821', '1832046740', '1', '/atms/share/ejnl/img/20250821/05113.402.1832046740.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('05113', '402', '20250821', '1832046740', '3', '/atms/share/ejnl/img/20250821/05113.402.1832046740.03.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('05113', '402', '20250821', '1832056750', '1', '/atms/share/ejnl/img/20250821/05113.402.1832056750.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('05113', '402', '20250821', '1832326770', '1', '/atms/share/ejnl/img/20250821/05113.402.1832326770.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('05113', '402', '20250821', '1834416800', '1', '/atms/share/ejnl/img/20250821/05113.402.1834416800.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('05113', '402', '20250821', '1832486790', '1', '/atms/share/ejnl/img/20250821/05113.402.1832486790.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('05113', '402', '20250821', '1838556950', '1', '/atms/share/ejnl/img/20250821/05113.402.1838556950.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('05113', '402', '20250821', '1838446940', '1', '/atms/share/ejnl/img/20250821/05113.402.1838446940.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('05113', '402', '20250821', '1837476930', '1', '/atms/share/ejnl/img/20250821/05113.402.1837476930.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('05113', '402', '20250821', '1837316910', '1', '/atms/share/ejnl/img/20250821/05113.402.1837316910.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('05113', '402', '20250821', '1837056890', '1', '/atms/share/ejnl/img/20250821/05113.402.1837056890.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('05113', '402', '20250821', '1837046880', '3', '/atms/share/ejnl/img/20250821/05113.402.1837046880.03.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('05113', '402', '20250821', '1837046880', '1', '/atms/share/ejnl/img/20250821/05113.402.1837046880.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('05113', '402', '20250821', '1835516820', '1', '/atms/share/ejnl/img/20250821/05113.402.1835516820.01.00.1.jpg');
INSERT INTO T_JOURNAL_IMAGE
(TERM_GROUP_ID, TERM_ID, JNL_DATE, JNL_TIME, IMAGE_TYPE, IMAGE_PATH)
VALUES('05113', '402', '20250821', '1834486810', '1', '/atms/share/ejnl/img/20250821/05113.402.1834486810.01.00.1.jpg');
COMMIT;
