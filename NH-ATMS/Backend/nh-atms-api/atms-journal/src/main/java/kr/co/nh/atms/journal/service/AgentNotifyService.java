package kr.co.nh.atms.journal.service;

import kr.co.nh.atms.common.exception.BusinessException;
import kr.co.nh.atms.journal.dto.DownloadResultNotifyRequest;
import kr.co.nh.atms.journal.dto.JournalModeNotifyRequest;
import kr.co.nh.atms.journal.mapper.AgentStatusMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 에이전트→서버 통보 처리.
 *   journal-mode    : 저널모드 변경 통보 → T_AGENT_STATUS.JNL_MODE 반영
 *   download-result : 다운로드 결과 통보 → 로그 기록(성공/실패)
 *
 * atmId(8) = termGroupId(5) + termId(3).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentNotifyService {

    private final AgentStatusMapper agentStatusMapper;

    @Transactional
    public void journalMode(JournalModeNotifyRequest req) {
        String id = requireAtmId(req.getAtmId());
        int n = agentStatusMapper.updateJnlMode(id.substring(0, 5), id.substring(5, 8), req.getJnlmode());
        log.info("[agent-notify] journal-mode atmId={} mode={} reason={} at={} updated={}",
                id, req.getJnlmode(), req.getReason(), req.getOccurredAt(), n);
    }

    @Transactional(readOnly = true)
    public void downloadResult(DownloadResultNotifyRequest req) {
        String id = requireAtmId(req.getAtmId());
        if (req.isSuccess()) {
            log.info("[agent-notify] download-result OK atmId={} file={} ver={} at={}",
                    id, req.getFileName(), req.getVersion(), req.getOccurredAt());
        } else {
            log.warn("[agent-notify] download-result FAIL atmId={} file={} ver={} err={} at={}",
                    id, req.getFileName(), req.getVersion(), req.getErrorMessage(), req.getOccurredAt());
        }
    }

    private static String requireAtmId(String atmId) {
        String id = atmId == null ? "" : atmId.trim();
        if (id.length() != 8) {
            throw new BusinessException("INVALID_ATM_ID", "atmId 는 8자리여야 합니다.");
        }
        return id;
    }
}
