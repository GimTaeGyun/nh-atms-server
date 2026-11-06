package kr.co.nh.atms.file.remote.protocol;

// =============================================================
// 구 새마을금고 atmf.lib (atmf.server.ProtcolDataTransfer) 원본 이식.
// 프로토콜 호환을 위해 로직/상수 변경 없이 패키지만 변경.
// =============================================================
final public class ProtcolDataTransfer {
    /*
    |========================================================|
    | 데이터 전송 규약                                       |
    | : LITTLE ENDIAN                                        |
    | : UNSIGNED                                             |
    | : 최대1MB                                              |
    |========================================================|
    [FIXED DATA : 원격명령 : 전문 형식]
    ※ CHUNKED DATA 는 미사용
    */

    public static final int FIXED_HEAD_SIZE = 4;

    public static final int CHUNK_HEAD_SIZE = 4;

    public static final int MAX_FIXED_SIZE = 1024 * 1024 * 1;// 최대 1MB

    public static final String CHATSET = "utf-8";
}
