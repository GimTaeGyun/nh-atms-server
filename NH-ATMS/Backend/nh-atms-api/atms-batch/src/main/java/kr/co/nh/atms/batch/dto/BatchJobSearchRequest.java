package kr.co.nh.atms.batch.dto;

import kr.co.nh.atms.common.page.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 배치잡 목록 검색 조건.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BatchJobSearchRequest extends PageRequest {
    private String procName;   // 프로세스명 (LIKE)
    private String jobDesc;    // 작업설명 (LIKE)
    private String useFlag;    // 사용여부 (Y/N)
    private String hostName;   // 호스트
}
