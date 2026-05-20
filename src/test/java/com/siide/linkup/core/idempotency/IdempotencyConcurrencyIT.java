package com.siide.linkup.core.idempotency;

import com.siide.linkup.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.DockerClientFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrency contract: N parallel calls with the same {@code Idempotency-Key} and the
 * same body produce exactly one handler invocation. Some losers may receive
 * {@code IdempotencyInProgressException} (409) if they arrive while the winner is still
 * running — that is acceptable and the client retry contract.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@EnabledIf(value = "dockerAvailable", disabledReason = "Docker not available on this host")
class IdempotencyConcurrencyIT {

    @Autowired IdempotencyService service;

    static boolean dockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable t) {
            return false;
        }
    }

    @Test
    void handler_executes_exactly_once_under_50_concurrent_calls() throws Exception {
        String key = UUID.randomUUID().toString();
        UUID userId = UUID.randomUUID();
        String endpoint = "POST /api/v1/test";
        Map<String, Integer> body = Map.of("seats", 1);

        int threads = 50;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger handlerInvocations = new AtomicInteger();
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger inProgress = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    ResponseEntity<String> resp = service.execute(key, userId, endpoint, body, String.class, () -> {
                        handlerInvocations.incrementAndGet();
                        try { Thread.sleep(150); } catch (InterruptedException ignored) {}
                        return ResponseEntity.status(HttpStatus.CREATED).body("ok");
                    });
                    if (resp.getStatusCode().value() == 201) {
                        successes.incrementAndGet();
                    }
                } catch (Exception e) {
                    if (e.getClass().getSimpleName().equals("IdempotencyInProgressException")) {
                        inProgress.incrementAndGet();
                    }
                }
            });
        }

        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();

        assertThat(handlerInvocations.get())
                .as("handler must run exactly once across all concurrent calls")
                .isEqualTo(1);
        assertThat(successes.get() + inProgress.get()).isEqualTo(threads);
        assertThat(successes.get()).isGreaterThanOrEqualTo(1);
    }
}
