package kr.co.nh.atms.journal.parser;

import kr.co.nh.atms.journal.dto.JifHeadKey;
import kr.co.nh.atms.journal.dto.JifMessage;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JifFieldUtilsTest {

    @Test
    void parseHead_buildsExpectedKey() {
        JifMessage msg = new JifMessage();
        Map<String, Object> head = new LinkedHashMap<>();
        head.put("yyyy", "2026");
        head.put("mm", "05");
        head.put("dd", "21");
        head.put("hh", "15");
        head.put("mi", "30");
        head.put("ss", "00");
        head.put("fff", "123");
        head.put("flag", "0");
        head.put("bk_cd", "011");
        head.put("atm_id", "03300005          "); // 5 + 3 + filler
        head.put("jnl_seq", "00012");
        head.put("jnl_kind", "22");
        msg.setHead(head);

        JifHeadKey k = JifFieldUtils.parseHead(msg);

        assertEquals("03300", k.getTermGroupId());
        assertEquals("005",   k.getTermId());
        assertEquals("20260521",   k.getJnlDate());
        assertEquals("1530001230", k.getJnlTime());
        assertEquals("22",  k.getJnlKind());
        assertEquals("011", k.getBankCd());
        assertEquals("00012", k.getJnlSeq());
    }

    @Test
    void parseHead_missingDate_throws() {
        JifMessage msg = new JifMessage();
        Map<String, Object> head = new LinkedHashMap<>();
        head.put("yyyy", "2026");
        // mm 누락
        head.put("dd", "21");
        msg.setHead(head);

        assertThrows(IllegalArgumentException.class, () -> JifFieldUtils.parseHead(msg));
    }

    @Test
    void parseHead_invalidAtmId_throws() {
        JifMessage msg = new JifMessage();
        Map<String, Object> head = new LinkedHashMap<>();
        head.put("yyyy", "2026"); head.put("mm", "05"); head.put("dd", "21");
        head.put("hh", "10");     head.put("mi", "00"); head.put("ss", "00");
        head.put("atm_id", "ABC"); // 너무 짧음
        head.put("jnl_kind", "21");
        msg.setHead(head);

        assertThrows(IllegalArgumentException.class, () -> JifFieldUtils.parseHead(msg));
    }
}
