package kr.co.nh.atms.web;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Tag("db")   // 실제 DB 접속 → 빌드 기본 제외, -PdbTest 일 때만 (2026-11-05)
@SpringBootTest
class AtmsWebApplicationTests {

    @Test
    void contextLoads() {
    }
}
