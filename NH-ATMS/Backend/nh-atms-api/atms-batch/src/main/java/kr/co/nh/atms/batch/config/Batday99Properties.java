package kr.co.nh.atms.batch.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * batday99 (파일 정리) 설정 — AS-IS atmb config.json 의 batday99 항목 이식.
 *
 * application.yml 예:
 * <pre>
 * atms:
 *   batch:
 *     batday99:
 *       jnl-img-root: /nas/ejnl/img     # 저널 이미지 루트(yyyyMMdd 하위 디렉토리)
 *       jnl-img-months: -3              # 보관개월(음수). 기준일 이전 일자 디렉토리 전체 삭제
 *       targets:                        # 보관일수(음수) 초과 파일 삭제 대상 경로 목록
 *         - path: /nas/temp
 *           days: -7
 * </pre>
 */
@Data
@Component
@ConfigurationProperties(prefix = "atms.batch.batday99")
public class Batday99Properties {

    /** 저널 이미지 루트 디렉토리 (yyyyMMdd 하위 디렉토리 단위 삭제). 미설정 시 스킵 */
    private String jnlImgRoot;

    /** 저널 이미지 보관개월 — 음수 (예: -3 = 3개월 이전 일자 디렉토리 삭제) */
    private int jnlImgMonths = -3;

    /** 일반 파일 정리 대상 목록 */
    private List<Target> targets = new ArrayList<>();

    @Data
    public static class Target {
        /** 정리 대상 디렉토리(또는 파일) 경로 */
        private String path;
        /** 보관일수 — 음수 (예: -7 = 수정일이 7일 이전인 파일 삭제) */
        private int days = -7;
    }
}
