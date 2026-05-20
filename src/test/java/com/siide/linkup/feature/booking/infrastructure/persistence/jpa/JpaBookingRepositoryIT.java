package com.siide.linkup.feature.booking.infrastructure.persistence.jpa;

import com.siide.linkup.TestcontainersConfiguration;
import com.siide.linkup.feature.booking.domain.model.Booking;
import com.siide.linkup.feature.booking.domain.model.BookingStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.DockerClientFactory;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Full-context IT exercising the partial unique index defined in V3.
 * <p>
 * Uses {@code ddl-auto: validate} so Flyway owns the schema (otherwise Hibernate's
 * default {@code create-drop} regenerates the table from JPA metadata and drops
 * the partial unique index that lives only in the migration).
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
@EnabledIf(value = "dockerAvailable", disabledReason = "Docker not available on this host")
class JpaBookingRepositoryIT {

    @Autowired JpaBookingRepository repository;

    static boolean dockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable t) {
            return false;
        }
    }

    @Test
    @Transactional
    void partial_unique_index_rejects_second_confirmed_booking_on_same_activity() {
        UUID userId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();

        repository.saveAndFlush(Booking.confirm(userId, activityId, 1));

        Booking duplicate = Booking.confirm(userId, activityId, 2);
        assertThatThrownBy(() -> repository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void partial_unique_index_allows_re_booking_after_cancellation() {
        UUID userId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();

        Booking first = Booking.confirm(userId, activityId, 1);
        first.cancel(Instant.now());
        repository.saveAndFlush(first);

        Booking second = Booking.confirm(userId, activityId, 1);
        repository.saveAndFlush(second);

        assertThat(repository.countByUserIdAndStatus(userId, BookingStatus.CONFIRMED)).isEqualTo(1);
    }
}
