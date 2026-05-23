package com.qalytix;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Full context test requires a running PostgreSQL instance.
// Re-enable once Testcontainers integration tests are added (Phase 1 integration test chunk).
@Disabled
@SpringBootTest
class QalytixBackendApplicationTests {

    @Test
    void contextLoads() {
    }

}
