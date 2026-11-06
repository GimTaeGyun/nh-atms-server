package kr.co.nh.atms.batch.executor;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 배치 작업 실행 결과(집계 건수).
 */
@Getter
@AllArgsConstructor
public class BatchTaskResult {
    private final long targetCnt;   // 대상건수
    private final long procCnt;     // 처리건수
    private final long errCnt;      // 에러건수

    public static BatchTaskResult empty() {
        return new BatchTaskResult(0, 0, 0);
    }

    public static BatchTaskResult of(long target, long proc, long err) {
        return new BatchTaskResult(target, proc, err);
    }
}
