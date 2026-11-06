package kr.co.nh.atms.journal.controller;

import jakarta.servlet.http.HttpServletRequest;
import kr.co.nh.atms.journal.dto.AgentOpenRequest;
import kr.co.nh.atms.journal.dto.AgentOpenResponse;
import kr.co.nh.atms.journal.dto.AgentSimpleRequest;
import kr.co.nh.atms.journal.mapper.AgentStatusMapper;
import kr.co.nh.atms.journal.service.AgentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 레거시 ATM 에이전트 호환 엔드포인트.
 *
 * 실 에이전트(펌웨어, 수정 불가)는 form-urlencoded 로 레거시 .do 경로를 호출하고
 * 레거시 JSON(succ/atmid/files/atms/atmf/jnl/ping) 응답을 기대한다.
 * 신규 /api/agent/* (JSON) 와 동일한 비즈니스 로직(AgentService)을 재사용하며,
 * 경로/요청포맷만 레거시 계약에 맞춰 흡수한다.
 *
 * 에이전트 INI(atms 섹션) 실 호출 경로:
 *   agent.open    = /atms/agent/open.do          (POST, urlencoded)
 *   agent.exit    = /atms/agent/exit.do          (POST, urlencoded)
 *   agent.ping    = /atmr/agent/ping.do?atmid=   (GET, query)
 *   put.jnl.mode  = /atms/agent/put.jnl.mode.do  (POST, atmid&mode)
 * (배포 환경별로 prefix 가 /atms 또는 /atmr 로 달라 둘 다 매핑한다.)
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class LegacyAgentController {

    private final AgentService agentService;
    private final AgentStatusMapper agentStatusMapper;

    /** 에이전트 기동(open). form-urlencoded → 레거시 중첩 JSON 응답. */
    @PostMapping(value = {"/atms/agent/open.do", "/atmr/agent/open.do"})
    public AgentOpenResponse open(
            @RequestParam("atmid") String atmid,
            @RequestParam(value = "jnl_mode", required = false) String jnlMode,
            @RequestParam(value = "ap_version", required = false) String apVersion,
            @RequestParam(value = "ag_version", required = false) String agVersion,
            @RequestParam(value = "os_mac", required = false) String osMac,
            @RequestParam(value = "os_name", required = false) String osName,
            HttpServletRequest http) {
        AgentOpenRequest req = new AgentOpenRequest();
        req.setAtmId(trim(atmid));
        req.setJnlMode(trim(jnlMode));
        req.setApVersion(trim(apVersion));
        req.setAgVersion(trim(agVersion));
        req.setOsMac(trim(osMac));
        req.setOsName(trim(osName));
        return agentService.open(req, clientIp(http), http.getHeader("host"));
    }

    /** 에이전트 종료(exit). */
    @PostMapping(value = {"/atms/agent/exit.do", "/atmr/agent/exit.do"})
    public Map<String, Object> exit(@RequestParam("atmid") String atmid, HttpServletRequest http) {
        AgentSimpleRequest req = new AgentSimpleRequest();
        req.setAtmId(trim(atmid));
        boolean ok = agentService.exit(req, clientIp(http));
        return succ(ok);
    }

    /**
     * 폴링(ping). 에이전트는 GET(?atmid=) 또는 POST 로 호출한다.
     * /api/agent/ping: 개국 응답(agent.ping.url 기본값)이 안내하는 경로 —
    public Map<String, Object> ping(@RequestParam("atmid") String atmid, HttpServletRequest http) {
        AgentSimpleRequest req = new AgentSimpleRequest();
        req.setAtmId(trim(atmid));
        boolean ok = agentService.ping(req, clientIp(http));
        return succ(ok);
    }

    /** 저널 운영모드 변경 통보(put.jnl.mode). POST atmid&mode. 레거시는 항상 succ=true(접수). */
    @PostMapping(value = {"/atms/agent/put.jnl.mode.do", "/atmr/agent/put.jnl.mode.do",
                          "/atms/remote-exec/put.jnl.mode.do"})
    public Map<String, Object> putJnlMode(@RequestParam("atmid") String atmid,
                                          @RequestParam("mode") String mode) {
        String id = trim(atmid);
        String termGroupId = id.length() >= 5 ? id.substring(0, 5) : id;
        String termId = id.length() >= 8 ? id.substring(5, 8) : null;
        int n = agentStatusMapper.updateJnlMode(termGroupId, termId, trim(mode));
        log.debug("[legacy put.jnl.mode] atmid={} mode={} updated={}", id, mode, n);
        return succ(true);
    }

    private static Map<String, Object> succ(boolean ok) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("succ", ok);
        return m;
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    /** 프록시(X-Forwarded-For) 우선, 없으면 remoteAddr. 15자 컬럼 대비 IPv4 추출. */
    private static String clientIp(HttpServletRequest http) {
        String xff = http.getHeader("X-Forwarded-For");
        String ip;
        if (xff != null && !xff.isBlank()) {
            ip = xff.split(",")[0].trim();
        } else {
            ip = http.getRemoteAddr();
        }
        if (ip != null && ip.startsWith("::ffff:")) ip = ip.substring(7); // IPv4-mapped IPv6
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) ip = "127.0.0.1";
        return ip;
    }
}
