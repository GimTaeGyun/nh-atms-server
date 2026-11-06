package kr.co.nh.atms.ifin.constants;

/**
 * 대내 인터페이스 상수. 구 atmi ATMIConstants 이식.
 */
public final class InternalIfConstants {

    private InternalIfConstants() {
    }

    /*-----------------------------------------------------------------------*/
    /* 응답코드 (AS-IS 복원)                                                  */
    /*-----------------------------------------------------------------------*/
    /** OK */
    public static final String RESPONSE_OK      = "0000";
    /** 조회자료없음 or 조회대상자료 아님 */
    public static final String RESPONSE_NO_DATA = "0097";
    /** 결번요청 응답(무응답) */
    public static final String RESPONSE_MISSING = "0098";
    /** 오류 */
    public static final String RESPONSE_ERROR   = "0099";

    public static final String BANK_CODE = "045";

    /** 시재조회 거래코드 (구 tr_cd) */
    public static final String DRAWER_CHECK_TR_CD = "S0ACH0107I001";
    /** 시재조회 EAI서비스코드(요청) */
    public static final String DRAWER_CHECK_SVC_CD = "ATMONST10100-S";
    /** 시재조회 EAI서비스코드(응답) */
    public static final String DRAWER_CHECK_RESP_SVC_CD = "ATMONST10100";

    /** SMS 거래코드 (구 tr_cd) */
    public static final String SMS_SEND_TR_CD = "SMS000001";
    /** SMS EAI서비스코드 */
    public static final String SMS_SEND_SVC_CD = "SMSONSS10200";
}
