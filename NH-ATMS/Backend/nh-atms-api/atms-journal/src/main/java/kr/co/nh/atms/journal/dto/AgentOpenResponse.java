package kr.co.nh.atms.journal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 에이전트 기동(open) 응답.
 * 레거시 atmr open.do 의 중첩 JSON 구조를 그대로 복원:
 *   { succ, atmid, files[], atms{}, atmf{}, jnl{}, ping{} }
 *
 * 서버 주소(atms/atmf)·폴링 URL 은 T_AGENT_STATUS 에 컬럼이 없어
 * 설정(agent.*) 기반으로 채운다. jnl/이미지옵션/폴링주기는 upsert 직후
 * DB 재조회(selectByIp) 값을 사용한다.
 * AP 자동배포(files)는 T_AP_LAST_VERSION + T_DOWNLOAD_SERVER 조회로 채우며,
 * 배포 대상 없음/조회 실패 시 빈 항목 1건(레거시 else 분기와 동일).
 */
@Data
@Builder
public class AgentOpenResponse {

    private boolean succ;

    @JsonProperty("atmid")
    private String atmId;

    private List<FileEntry> files;
    private Atms atms;
    private Atmf atmf;
    private Jnl  jnl;
    private Ping ping;

    /** AP 배포 파일 정보. */
    @Data
    @Builder
    public static class FileEntry {
        private FileType type;
        private String   vers;
        private String   file;
        private String   dest;
        private long     size;
    }

    @Data
    @Builder
    public static class FileType {
        private int    val;
        private String name;
    }

    /** 저널 수신 서버(atms-journal) 접속 정보. */
    @Data
    @Builder
    public static class Atms {
        private String host;
        private String port;
        private String schema;
    }

    /** 파일전송 서버(atms-file) 접속 정보. */
    @Data
    @Builder
    public static class Atmf {
        private String host;
        private String port;
    }

    /** 저널 모드 + 이미지 업로드 옵션. */
    @Data
    @Builder
    public static class Jnl {
        private String mode;                       // 저널 모드
        @JsonProperty("img01_upyn")
        private String img01Upyn;                  // 고객 이미지 업로드 여부
        @JsonProperty("img02_upyn")
        private String img02Upyn;                  // 수취 이미지 업로드 여부
        @JsonProperty("img03_upyn")
        private String img03Upyn;                  // 카드 이미지 업로드 여부
    }

    /** 폴링 정보. */
    @Data
    @Builder
    public static class Ping {
        private String url;
        private String sec;
    }
}
