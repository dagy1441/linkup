package com.siide.linkup.feature.booking;

import com.siide.linkup.feature.activity.domain.ActivityRepository;
import com.siide.linkup.feature.activity.domain.model.Activity;
import com.siide.linkup.feature.activity.domain.model.ActivityCategory;
import com.siide.linkup.feature.activity.domain.model.Location;
import com.siide.linkup.feature.booking.application.BookingCommandService;
import com.siide.linkup.feature.booking.application.dto.CreateBookingCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.DockerClientFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the atomic seat-reservation guarantee: N concurrent booking attempts on
 * an activity with capacity = 1 produce exactly one successful booking. The mechanism
 * under test is the {@code @Modifying} JPQL UPDATE in {@code JpaActivityRepository}
 * that enforces {@code booked_count + qty <= capacity} in a single SQL round-trip.
 */
@SpringBootTest
@Import(com.siide.linkup.TestcontainersConfiguration.class)
@ActiveProfiles("test")
@EnabledIf(value = "dockerAvailable", disabledReason = "Docker not available on this host")
class BookingConcurrencyIT {

    @Autowired BookingCommandService bookingService;
    @Autowired ActivityRepository activityRepository;

    static boolean dockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable t) {
            return false;
        }
    }

    @Test
    void only_one_booking_succeeds_when_capacity_is_one() throws Exception {
        Instant now = Instant.now();
        Activity activity = Activity.create(
                "Race", null,
                ActivityCategory.CULTURE,
                Location.ofCity("Abidjan"),
                now.plus(1, ChronoUnit.HOURS),
                1,
                UUID.randomUUID(),
                now);
        activityRepository.save(activity);

        int threads = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    UUID userId = UUID.randomUUID();
                    bookingService.create(new CreateBookingCommand(activity.getId(), 1), userId);
                    successes.incrementAndGet();
                } catch (Exception e) {
                    failures.incrementAndGet();
                }
            });
        }

        start.countDown();
        pool.shutdown();
        boolean terminated = pool.awaitTermination(60, TimeUnit.SECONDS);
        assertThat(terminated).isTrue();

        assertThat(successes.get()).isEqualTo(1);
        assertThat(failures.get()).isEqualTo(threads - 1);

        Activity reloaded = activityRepository.findById(activity.getId()).orElseThrow();
        assertThat(reloaded.getBookedCount()).isEqualTo(1);
    }
}
