package kr.co.nh.atms.journal.service;

import kr.co.nh.atms.journal.dto.AgentOpenRequest;
import kr.co.nh.atms.journal.dto.AgentOpenResponse;
import kr.co.nh.atms.journal.dto.AgentSimpleRequest;
import kr.co.nh.atms.journal.dto.ApDeployInfo;
import kr.co.nh.atms.journal.entity.AgentStatus;
import kr.co.nh.atms.journal.mapper.AgentStatusMapper;
import kr.co.nh.atms.journal.mapper.ApVersionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * ATM 에이전트 상태 관리 (레거시 atmr AgentService 신규 포팅).
 *
 *   open  → T_AGENT_STATUS upsert (connect_flag=1, 기동시각/버전/이미지옵션)
 *   exit  → connect_flag=0 + 종료시각
 *   ping  → 최종 폴링 시각 갱신
 *
 * atmId(8) = termGroupId(5) + termId(3).
 * 클라이언트 IP / host 는 Controller 에서 추출해 전달.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentService {

    private static final DateTimeFormatter D = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter T = DateTimeFormatter.ofPattern("HHmmss");

    private static final String DEFAULT_POLL_CYCLE = "30";

    private final AgentStatusMapper mapper;
    private final ApVersionMapper apVersionMapper;

    /** 에이전트에 통보할 저널 수신 서버(atms-journal) 접속 정보. */
    @Value("${agent.atms.host:}")
    private String atmsHost;
    @Value("${agent.atms.port:8081}")
    private String atmsPort;
    @Value("${agent.atms.schema:https}")
    private String atmsSchema;

    /** 에이전트에 통보할 파일전송 서버(atms-file) 접속 정보. */
    @Value("${agent.atmf.host:}")
    private String atmfHost;
    @Value("${agent.atmf.port:8083}")
    private String atmfPort;

    /** 폴링 엔드포인트 URL. */
    @Value("${agent.ping.url:/api/agent/ping}")
    private String pingUrl;

    /**
     * AP 배포본 저장소 루트(파일서버 업로드 상위경로).
     * T_DOWNLOAD_SERVER.FILE_PATH 에서 이 접두어를 제거한 상대경로를 에이전트에 내려준다.
     * (레거시 atmr 의 config repository 대응)
     */
    @Value("${agent.ap.repository:}")
    private String apRepository;

    @Transactional
    public AgentOpenResponse open(AgentOpenRequest req, String clientIp, String host) {
        String atmId = safe(req.getAtmId());
        String termGroupId = sub(atmId, 0, 5);
        String termId = sub(atmId, 5, 8);

        LocalDateTime now = LocalDateTime.now();
        String connectIp = null;
        String connectPort = null;
        if (host != null && !host.isBlank()) {
            String[] hp = host.split(":");
            connectIp = hp.length > 0 ? hp[0] : null;
            connectPort = hp.length > 1 ? hp[1] : null;
        }

        AgentStatus a = AgentStatus.builder()
                .atmIp(clientIp)
                .agentType("1")
                .termGroupId(termGroupId)
                .termId(termId)
                .connectFlag("1")
                .agentVer(req.getAgVersion())
                .apDownVer(req.getApVersion())
                .osName(req.getOsName())
                .atmMacAddr(req.getOsMac())
                .connectIp(connectIp)
                .connectPort(connectPort)
                .jnlMode(req.getJnlMode())
                .cardImage("1")
                .dispenserImage("1")
                .customerImage("1")
                .pollCycle(DEFAULT_POLL_CYCLE)
                .startDate(now.format(D))
                .startTime(now.format(T))
                .build();

        mapper.upsert(a);

        // upsert 직후 DB 재조회(레거시 selectAgentStatus 재조회와 동일) → 저널/이미지/폴링 설정 권위값 확보
        AgentStatus st = mapper.selectByIp(clientIp);
        if (st == null) st = a;

        log.debug("[agent open] atmId={} ip={} ver={}/{}", atmId, clientIp, req.getApVersion(), req.getAgVersion());

        return AgentOpenResponse.builder()
                .succ(true)
                .atmId(atmId)
                .files(List.of(apFile(termGroupId, termId)))
                .atms(AgentOpenResponse.Atms.builder()
                        .host(atmsHost).port(atmsPort).schema(atmsSchema).build())
                .atmf(AgentOpenResponse.Atmf.builder()
                        .host(atmfHost).port(atmfPort).build())
                .jnl(AgentOpenResponse.Jnl.builder()
                        .mode(st.getJnlMode())
                        .img01Upyn(st.getCustomerImage())
                        .img02Upyn(st.getDispenserImage())
                        .img03Upyn(st.getCardImage())
                        .build())
                .ping(AgentOpenResponse.Ping.builder()
                        .url(pingUrl)
                        .sec(st.getPollCycle() != null ? st.getPollCycle() : DEFAULT_POLL_CYCLE)
                        .build())
                .build();
    }

    /**
     * AP 자동배포 파일 항목 구성 (레거시 atmr AgentServiceImpl.handleAgentOpen 이식).
     * T_AP_LAST_VERSION + T_DOWNLOAD_SERVER 조인(selectApLastVersion 대응)으로
     * vers / file(저장소 루트 기준 상대경로) / dest(기기측 저장경로) / size 를 채운다.
     * 배포 데이터 없음·조회 실패 시 레거시 else 분기와 동일하게 빈 항목 1건 반환
     * (에이전트는 vers 빈값이면 다운로드 스킵).
     */
    private AgentOpenResponse.FileEntry apFile(String termGroupId, String termId) {
        try {
            ApDeployInfo info = apVersionMapper.selectApLastVersion(termGroupId, termId);
            if (info != null && info.getFilePath() != null && !info.getFilePath().isBlank()) {
                return AgentOpenResponse.FileEntry.builder()
                        .type(AgentOpenResponse.FileType.builder().val(1).name("ap").build())
                        .vers(nvl(info.getLastDownVer()))
                        .file(relativizeApPath(info.getFilePath()))
                        .dest(nvl(info.getDownPath()))
                        .size(info.getFileSize() != null ? info.getFileSize() : 0L)
                        .build();
            }
        } catch (Exception e) {
            // AP 배포 정보 조회 실패가 에이전트 기동을 막지 않도록 fallback (레거시도 open 전체 try-catch)
            log.warn("[agent open] AP 배포버전 조회 실패 term={}/{} — 빈 항목 반환", termGroupId, termId, e);
        }
        return emptyApFile();
    }

    /**
     * 서버측 파일경로에서 저장소 루트(agent.ap.repository)를 제거한 상대경로 반환.
     * 항상 '/' 구분자 + 선행 '/' 형태 (레거시 relativize 정합).
     */
    private String relativizeApPath(String serverFilePath) {
        String path = serverFilePath.trim().replace('\\', '/');
        String repo = apRepository == null ? "" : apRepository.trim().replace('\\', '/');
        if (!repo.isEmpty()) {
            if (repo.endsWith("/")) repo = repo.substring(0, repo.length() - 1);
            if (path.startsWith(repo)) path = path.substring(repo.length());
        }
        return path.startsWith("/") ? path : "/" + path;
    }

    private static String nvl(String s) {
        return s == null ? "" : s.trim();
    }

    /** 배포 대상 없음/조회 실패 시 빈 항목 1건 (레거시 else 분기와 동일). */
    private static AgentOpenResponse.FileEntry emptyApFile() {
        return AgentOpenResponse.FileEntry.builder()
                .type(AgentOpenResponse.FileType.builder().val(1).name("ap").build())
                .vers("")
                .file("")
                .dest("")
                .size(0L)
                .build();
    }

    @Transactional
    public boolean exit(AgentSimpleRequest req, String clientIp) {
        LocalDateTime now = LocalDateTime.now();
        int n = mapper.updateExit(clientIp, now.format(D), now.format(T));
        log.debug("[agent exit] atmId={} ip={} updated={}", req.getAtmId(), clientIp, n);
        return n > 0;
    }

    @Transactional
    public boolean ping(AgentSimpleRequest req, String clientIp) {
        LocalDateTime now = LocalDateTime.now();
        int n = mapper.updatePoll(clientIp, now.format(D), now.format(T));
        log.debug("[agent ping] atmId={} ip={} updated={}", req.getAtmId(), clientIp, n);
        return n > 0;
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private static String sub(String s, int from, int to) {
        if (s == null || s.length() < to) return null;
        return s.substring(from, to);
    }
}
