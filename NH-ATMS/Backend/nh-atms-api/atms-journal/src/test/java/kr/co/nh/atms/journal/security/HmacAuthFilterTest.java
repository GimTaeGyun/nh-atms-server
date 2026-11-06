package kr.co.nh.atms.journal.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HMAC 필터 적용 경로 판정 — 루트 컨텍스트(로컬 bootRun)와 '/journal' 컨텍스트(JEUS) 모두 같은 결과여야 한다.
 */
class HmacAuthFilterTest {

    private static HmacAuthFilter enabledFilter() {
        JournalHmacProperties props = new JournalHmacProperties();
        props.setEnabled(true);
        props.setSecret("test-secret");
        return new HmacAuthFilter(props);
    }

    private static MockHttpServletResponse run(String contextPath, String path) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", contextPath + path);
        req.setContextPath(contextPath);
        req.setContent("{}".getBytes());
        MockHttpServletResponse res = new MockHttpServletResponse();
        enabledFilter().doFilter(req, res, new MockFilterChain());
        return res;
    }

    @Test
    void rootContext_journalPathWithoutSignature_isRejected() throws Exception {
        assertEquals(401, run("", "/api/journal/receive").getStatus());
    }

    @Test
    void jeusContext_journalPathWithoutSignature_isRejected() throws Exception {
        assertEquals(401, run("/journal", "/api/journal/receive").getStatus());
    }

    @Test
    void jeusContext_otherPath_isNotFiltered() throws Exception {
        assertEquals(200, run("/journal", "/api/agent/open").getStatus());
    }

    @Test
    void pathWithinApplication_stripsContextPath() {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/journal/api/journal/receive");
        req.setContextPath("/journal");
        assertEquals("/api/journal/receive", HmacAuthFilter.pathWithinApplication(req));
    }
}
