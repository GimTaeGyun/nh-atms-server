package kr.co.nh.atms.journal.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 에이전트 종료(exit) / 폴링(ping) 공통 요청.
 */
@Data
public class AgentSimpleRequest {

    @NotBlank
    private String atmId;       // 8자리
}
