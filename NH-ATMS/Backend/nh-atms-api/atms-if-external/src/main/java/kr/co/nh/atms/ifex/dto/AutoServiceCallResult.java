package kr.co.nh.atms.ifex.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 자동출동 처리 결과 — 선정/요청/스킵 건수 및 송신 결과 목록.
 */
@Data
@Builder
public class AutoServiceCallResult {

    /** 조회된 자동출동 대상(T_FAULT_ANALYSIS) 건수 */
    private int candidateCount;

    /** 선정 기준 통과 후 실제 출동요청 송신 건수 */
    private int requestedCount;

    /** 송신 결과 목록 (대외 송신 EM_01001120 결과) */
    private List<ExternalIfResult> results;
}
