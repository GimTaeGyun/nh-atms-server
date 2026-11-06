package kr.co.nh.atms.journal.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 에이전트 폴링 응답 (계약: CommandList 스키마).
 */
@Data
@Builder
public class CommandListResponse {
    private List<CommandDto> commands;
}
