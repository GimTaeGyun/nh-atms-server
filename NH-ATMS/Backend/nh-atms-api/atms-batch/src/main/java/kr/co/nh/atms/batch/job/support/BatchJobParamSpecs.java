package kr.co.nh.atms.batch.job.support;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 배치잡별 수동실행 파라미터 스키마 — AS-IS atmb BatchScheduler.Task.paramSpec() 이식.
 *
 * 프론트 배치관리 화면이 수동실행 시 입력 폼을 구성할 수 있도록
 * 잡별 파라미터 이름/기본값(스키마)을 제공한다.
 * (AS-IS 와 동일하게 값이 비어 있으면 배치가 기본값(전일 등)을 적용한다는 의미)
 *
 * ※ 현행 수동실행 API(POST /jobs/{procName}/run)는 파라미터 없이 전일(statDate) 기준으로
 *   실행된다 — 본 스키마는 향후 파라미터 실행 지원을 위한 메타데이터 노출이다.
 */
public final class BatchJobParamSpecs {

    private static final Map<String, Map<String, Object>> SPECS = new LinkedHashMap<>();

    static {
        // AS-IS batday01/02/08/09 : job_date(yyyyMMdd, 공백=전일)
        SPECS.put("batday01", Map.of("job_date", ""));
        SPECS.put("batday02", Map.of("job_date", ""));
        SPECS.put("batday08", Map.of("job_date", ""));
        SPECS.put("batday09", Map.of("job_date", ""));
        // batday30/98 : 파라미터 없음 (AS-IS paramSpec null/빈 객체)
        SPECS.put("batday30", Map.of());
        SPECS.put("batday98", Map.of());
        // AS-IS batday99 : 경로/보관일수 목록 (TO-BE 는 application.yml 설정으로 통일)
        SPECS.put("batday99", Map.of("list", List.of(Map.of("path", "", "days", "-7"))));
    }

    private BatchJobParamSpecs() {
    }

    /** 잡별 파라미터 스키마 (미정의 잡은 빈 맵) */
    public static Map<String, Object> of(String procName) {
        return SPECS.getOrDefault(procName, Map.of());
    }
}
